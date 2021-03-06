package com.axway.apim.adapter.apis;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.axway.apim.adapter.APIManagerAdapter;
import com.axway.apim.adapter.apis.APIFilter.Builder.APIType;
import com.axway.apim.adapter.apis.APIManagerPoliciesAdapter.PolicyType;
import com.axway.apim.api.API;
import com.axway.apim.api.model.DeviceType;
import com.axway.apim.api.model.InboundProfile;
import com.axway.apim.api.model.OutboundProfile;
import com.axway.apim.api.model.Policy;
import com.axway.apim.api.model.SecurityDevice;
import com.axway.apim.api.model.SecurityProfile;
import com.axway.apim.api.model.ServiceProfile;
import com.axway.apim.lib.errorHandling.AppException;
import com.axway.apim.lib.utils.TestIndicator;

public class APIFilterTest {
	
	@BeforeClass
	public void setupTestIndicator() {
		TestIndicator.getInstance().setTestRunning(true);
		APIManagerAdapter.apiManagerVersion = "7.7";
	}
	
	@AfterClass
	public void removeTestIndicator() {
		TestIndicator.getInstance().setTestRunning(false);
		APIManagerAdapter.apiManagerVersion = null;
	}
	
	@Test
	public void testStandardActualAPI() {
		APIFilter filter = new APIFilter.Builder(APIType.ACTUAL_API).build();
		Assert.assertEquals(filter.isIncludeClientApplications(), true);
		Assert.assertEquals(filter.isIncludeClientOrganizations(), true);
		Assert.assertEquals(filter.isIncludeQuotas(), true);
		Assert.assertEquals(filter.isIncludeOriginalAPIDefinition(), true);
	}
	
	@Test
	public void testCustomActualAPI() {
		APIFilter filter = new APIFilter.Builder(APIType.ACTUAL_API)
				.includeClientApplications(false)
				.includeClientOrganizations(false)
				.includeQuotas(false)
				.build();
		Assert.assertEquals(filter.isIncludeClientApplications(), false);
		Assert.assertEquals(filter.isIncludeClientOrganizations(), false);
		Assert.assertEquals(filter.isIncludeQuotas(), false);
		Assert.assertEquals(filter.isIncludeOriginalAPIDefinition(), true);
	}
	
	@Test
	public void filterWithId() {
		APIFilter filter = new APIFilter.Builder()
				.hasId("9878973123")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 0);
		Assert.assertEquals(filter.getId(), "9878973123");
	}
	
	@Test
	public void filterWithPath() throws IOException, AppException {
		// For this test, we must simulate API-Manager version >7.7
		APIManagerAdapter.apiManagerVersion = null;
		APIManagerAdapter.getInstance().configAdapter.setAPIManagerTestResponse("{ \"productVersion\": \"7.7.20200130\" }", false);
		APIFilter filter = new APIFilter.Builder()
				.hasApiPath("/v1/api")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 3);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "path");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "/v1/api");
	}
	
	@Test
	public void filterWithPathOn762() throws IOException, AppException {
		// For this test, we must simulate API-Manager version >7.7
		APIManagerAdapter.apiManagerVersion = null;
		APIManagerAdapter.getInstance().configAdapter.setAPIManagerTestResponse("{ \"productVersion\": \"7.6.2 SP4\" }", false);
		APIFilter filter = new APIFilter.Builder()
				.hasApiPath("/v1/api")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 0);
		Assert.assertEquals(filter.getApiPath(), "/v1/api");
	}
	
	@Test
	public void filterWithName() {
		APIFilter filter = new APIFilter.Builder()
				.hasName("The name I want")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 3);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "name");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "The name I want");
	}
	
	@Test
	public void hasFullWildCardName() {
		APIFilter filter = new APIFilter.Builder()
				.hasName("*")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 0);
	}
	
	@Test
	public void filterWithBackendApiID() {
		APIFilter filter = new APIFilter.Builder()
				.hasApiId("7868768768")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 3);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "apiid");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "7868768768");
	}
	
	@Test
	public void filterWithDeprecated() {
		APIFilter filter = new APIFilter.Builder()
				.isDeprecated(true)
				.build();
		Assert.assertEquals(filter.getFilters().size(), 3);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "deprecated");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "true");
	}
	
	@Test
	public void filterWithRetired() {
		APIFilter filter = new APIFilter.Builder()
				.isRetired(true)
				.build();
		Assert.assertEquals(filter.getFilters().size(), 3);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "retired");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "true");
	}
	
	@Test
	public void filterWithState() {
		APIFilter filter = new APIFilter.Builder()
				.hasState("unpublished")
				.build();
		Assert.assertEquals(filter.getFilters().size(), 3);
		Assert.assertEquals(filter.getFilters().get(0).getValue(), "state");
		Assert.assertEquals(filter.getFilters().get(1).getValue(), "eq");
		Assert.assertEquals(filter.getFilters().get(2).getValue(), "unpublished");
	}
	
	@Test
	public void apiFilterEqualTest() {
		APIFilter filter1 = new APIFilter.Builder()
				.hasId("12345")
				.build();
		
		APIFilter filter2 = new APIFilter.Builder()
				.hasId("12345")
				.build();
		
		Assert.assertEquals(filter1, filter2, "Both filters should be equal");
	}
	
	@Test
	public void testBackendBasepathFilter() throws AppException {
		APIFilter filter = new APIFilter.Builder()
				.hasBackendBasepath("*emr-system*")
				.build();
		API testAPI = getAPIWithBackendBasepath("http://emr-system:8081");
		assertTrue(filter.filter(testAPI), "API with base-path: http://emr-system:8081 should match to filter: *emr-system*");
		testAPI = getAPIWithBackendBasepath("http://sec.hipaa:8086");
		assertFalse(filter.filter(testAPI), "API with base-path: http://sec.hipaa:8086 should NOT match to filter: *emr-system*");
		
		filter = new APIFilter.Builder()
				.hasBackendBasepath("http://emr-system:8081")
				.build();
		
		testAPI = getAPIWithBackendBasepath("http://emr-system:8081");
		assertTrue(filter.filter(testAPI), "API with base-path: http://emr-system:8081 should match to filter: http://emr-system:8081");
		
		testAPI = getAPIWithBackendBasepath("http://fhir3.healthintersections.com.au");
		assertFalse(filter.filter(testAPI), "API with base-path: http://fhir3.healthintersections.com.au should NOT match to filter: http://emr-system:8081");
	}
	
	@Test
	public void testPolicyFilter() throws AppException {
		APIFilter filter = new APIFilter.Builder()
				.hasPolicyName("*Policy*")
				.build();
		API testAPI = new API();
		addPolicy(testAPI, "Request Policy 1", PolicyType.REQUEST);
		addPolicy(testAPI, "Routing Policy 1", PolicyType.ROUTING);
		assertTrue(filter.filter(testAPI), "API must match to pattern '*Policy*'");
		
		filter = new APIFilter.Builder()
				.hasPolicyName("*Response*")
				.build();
		testAPI = new API();
		addPolicy(testAPI, "Request Policy 1", PolicyType.REQUEST);
		addPolicy(testAPI, "Response Policy 1", PolicyType.RESPONSE);
		assertTrue(filter.filter(testAPI), "API must match to pattern '*Response*'");
		
		filter = new APIFilter.Builder()
				.hasPolicyName("*Routing*")
				.build();
		testAPI = new API();
		addPolicy(testAPI, "Request Policy 1", PolicyType.REQUEST);
		addPolicy(testAPI, "Response Policy 1", PolicyType.RESPONSE);
		assertFalse(filter.filter(testAPI), "API must NOT match to pattern '*Response*'");
		
		filter = new APIFilter.Builder()
				.hasPolicyName("Response Policy 1")
				.build();
		testAPI = new API();
		addPolicy(testAPI, "Response Policy 1", PolicyType.RESPONSE);
		assertTrue(filter.filter(testAPI), "API must match to pattern 'Response Policy 1'");
		
		filter = new APIFilter.Builder()
				.hasPolicyName("Response Policy 2")
				.build();
		testAPI = new API();
		addPolicy(testAPI, "Response Policy 1", PolicyType.RESPONSE);
		assertFalse(filter.filter(testAPI), "API must NOT match to pattern 'Response Policy 2' as it is using 'Response Policy 1'");
		
		filter = new APIFilter.Builder()
				.hasPolicyName("Not used policy")
				.build();
		testAPI = new API();
		addPolicy(testAPI, "Response Policy 1", PolicyType.RESPONSE);
		assertFalse(filter.filter(testAPI), "API must NOT match to pattern 'Not used policy' as it is using 'Response Policy 1'");
	}
	
	@Test
	public void testInboundSecurityPolicyFilter() throws AppException {
		API testAPI = new API();
		addInboundSecurityPolicy(testAPI, "Inbound Security Policy 1");
		
		APIFilter filter = new APIFilter.Builder()
				.hasPolicyName("Inbound Security*")
				.build();
		assertTrue(filter.filter(testAPI), "API must match to pattern 'Inbound Security*'");
	}
	
	private API getAPIWithBackendBasepath(String basePath) {
		API api = new API();
		ServiceProfile serviceProfile = new ServiceProfile();
		serviceProfile.setBasePath(basePath);
		Map<String, ServiceProfile> serviceProfiles = new HashMap<String, ServiceProfile>();
		serviceProfiles.put("_default", serviceProfile);
		api.setServiceProfiles(serviceProfiles);
		return api;
	}
	
	private API addPolicy(API api, String policyName, PolicyType type) throws AppException {
		OutboundProfile outboundProfile = new OutboundProfile();
		Policy policy = new Policy(policyName);
		switch(type) {
		case REQUEST:
			outboundProfile.setRequestPolicy(policy);
			break;
		case ROUTING:
			outboundProfile.setRoutePolicy(policy);
			break;
		case RESPONSE:
			outboundProfile.setResponsePolicy(policy);
			break;
		case FAULT_HANDLER:
			outboundProfile.setFaultHandlerPolicy(policy);
			break;
		default:
			break;			
		}
		Map<String, OutboundProfile> outboundProfiles = new HashMap<String, OutboundProfile>();
		outboundProfiles.put("_default", outboundProfile);
		api.setOutboundProfiles(outboundProfiles);
		return api;
	}
	
	private API addInboundSecurityPolicy(API api, String policyName) throws AppException {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("authenticationPolicy", "<key type='CircuitContainer'><id field='name' value='API Keys'/><key type='FilterCircuit'><id field='name' value='"+policyName+"'/></key></key>");
		SecurityDevice securityDevice = new SecurityDevice();
		securityDevice.setType(DeviceType.authPolicy);
		securityDevice.setConvertPolicies(false);
		securityDevice.setProperties(properties);
		List<SecurityDevice> devices = new ArrayList<SecurityDevice>();
		devices.add(securityDevice);
		SecurityProfile securityProfile = new SecurityProfile();
		securityProfile.setName("_default");
		securityProfile.setDevices(devices);
		List<SecurityProfile> securityProfiles = new ArrayList<SecurityProfile>();
		securityProfiles.add(securityProfile);
		api.setSecurityProfiles(securityProfiles);
		
		InboundProfile inboundProfile = new InboundProfile();
		inboundProfile.setSecurityProfile("_default");
		Map<String, InboundProfile> inboundProfiles = new HashMap<String, InboundProfile>();
		inboundProfiles.put("_default", inboundProfile);
		api.setInboundProfiles(inboundProfiles);
		return api;
	}
}

/**
 *    "inboundProfiles":{
      "findPetsByStatus":{
         "securityProfile":"Yet another API-Key profile"
      },
      "_default":{
         "securityProfile":"_default"
      }
   },
   
   
 */

