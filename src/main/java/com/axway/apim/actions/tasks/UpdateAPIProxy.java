package com.axway.apim.actions.tasks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;

import com.axway.apim.actions.rest.PUTRequest;
import com.axway.apim.actions.rest.RestAPICall;
import com.axway.apim.actions.rest.Transaction;
import com.axway.apim.actions.tasks.props.PropertyHandler;
import com.axway.apim.lib.APIPropertyAnnotation;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.swagger.api.IAPIDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

public class UpdateAPIProxy extends AbstractAPIMTask implements IResponseParser {
	
	public UpdateAPIProxy(IAPIDefinition desiredState, IAPIDefinition actualState) {
		super(desiredState, actualState);
	}

	public void execute(List<String> changedProps) throws AppException {
		LOG.debug("Updating API-Proxy");
		URI uri;
		HttpEntity entity;
		ObjectMapper objectMapper = new ObjectMapper();
		
		Transaction context = Transaction.getInstance();
		
		try {
			JsonNode lastJsonReponse = (JsonNode)context.get("lastResponse");
			if(lastJsonReponse==null) { // This class is called as the first, so, first load the API
				lastJsonReponse = initActualAPIContext(this.actualState);
			}
			handledChangedProps(lastJsonReponse, this.desiredState, changedProps);
		
			uri = new URIBuilder(cmd.getAPIManagerURL()).setPath(RestAPICall.API_VERSION+"/proxies/"+context.get("virtualAPIId")).build();
			entity = new StringEntity(objectMapper.writeValueAsString(lastJsonReponse));
			
			RestAPICall updateAPIProxy = new PUTRequest(entity, uri, this);
			updateAPIProxy.execute();
		} catch (Exception e) {
			throw new AppException("Cannot update API-Proxy.", ErrorCode.CANT_UPDATE_API_PROXY, e);
		}
	}
	
	@Override
	public JsonNode parseResponse(HttpResponse response) throws AppException {
		String backendAPIId = JsonPath.parse(getJSONPayload(response)).read("$.id", String.class);
		Transaction.getInstance().put("backendAPIId", backendAPIId);
		return null;
	}	
	
	private static JsonNode handledChangedProps(JsonNode lastJsonReponse, IAPIDefinition desired, List<String> changedProps) throws AppException {
		Field field = null;
		if(changedProps!=null) {
			String logMessage = "Updating proxy for the following props: ";
			for(String fieldName : changedProps) {
				try {
					field = desired.getClass().getSuperclass().getDeclaredField(fieldName);
					if (field.isAnnotationPresent(APIPropertyAnnotation.class)) {
						APIPropertyAnnotation property = field.getAnnotation(APIPropertyAnnotation.class);
						if(void.class != property.propHandler()) { // Properties going this way, must be migrated
							Class clazz = property.propHandler();
							PropertyHandler propHandler = (PropertyHandler) clazz.newInstance();
							lastJsonReponse = propHandler.handleProperty(desired, lastJsonReponse);
							logMessage = logMessage + field.getName() + " ";
						} else {
							try {
								String getterMethodName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
								Method method = desired.getClass().getSuperclass().getMethod(getterMethodName, null);
								Object handler = method.invoke(desired, null);
								if(handler instanceof PropertyHandler) { // This is NEW/Preferred way
									((PropertyHandler)handler).handleProperty(desired, lastJsonReponse);
									logMessage = logMessage + field.getName() + " ";
								}
							} catch (Exception e) {
								LOG.warn("Property: " + field.getName() + " has no handler configured and is not a propertyHandler");
							}
						}
					}
				} catch (Exception e) {
					throw new AppException("Can't handle property: "+field+" to update API-Proxy.", ErrorCode.CANT_UPDATE_API_PROXY, e);
				}
			}
			LOG.info(logMessage);
		}
		return lastJsonReponse;
	}
}
