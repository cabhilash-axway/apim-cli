package com.axway.apim.users.lib;

import com.axway.apim.lib.APIMCoreCLIOptions;
import com.axway.apim.lib.CommandParameters;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.errorHandling.AppException;

public class UserImportParams extends CommandParameters {

	public UserImportParams(APIMCoreCLIOptions parser)
			throws AppException {
		super(parser.getCmd(), parser.getInternalCmd(), new EnvironmentProperties(parser.getCmd().getOptionValue("stage"), parser.getCmd().getOptionValue("swaggerPromoteHome")));
	}
	
	public static synchronized UserImportParams getInstance() {
		return (UserImportParams)CommandParameters.getInstance();
	}
	
	@Override
	public boolean ignoreCache() {
		// For import action we ignore the cache in all cases!
		return true;
	}
}
