package org.apache.stratos.metadataservice.services;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.Registry.DataRegistryFactory;
import org.apache.stratos.metadataservice.annotation.AuthorizationAction;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.exception.RestAPIException;
import org.apache.stratos.metadataservice.util.ConfUtil;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

@Path("/metadataservice/")
public class MetaDataAdmin {

	private static Log log = LogFactory.getLog(MetaDataAdmin.class);
	@Context
	HttpServletRequest httpServletRequest;

	private static ConfigurationContext configContext = null;

	private static final String CARBON_HOME = "/../../../../";
	private static String axis2Repo = "repository/deployment/client";
	private static String axis2Conf = "repository/conf/axis2/axis2_client.xml";

	private static final String defaultUsername = "admin@org.com";
	private static final String defaultPassword = "admin123";
	private static final String serverURL = "https://localhost:9445/services/";
	private static final String mainResource = "/startos/";
	private static final int defaultRank = 3;

	@POST
	@Path("/init")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public void initialize() throws RestAPIException {

	}

	private static WSRegistryServiceClient setRegistry() throws Exception {

		XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();

		String gregUsername = conf.getString("metadataservice.username", defaultUsername);
		String gregPassword = conf.getString("metadataservice.password", defaultPassword);
		String gregServerURL = conf.getString("metadataservice.serverurl", serverURL);
		System.setProperty("javax.net.ssl.trustStore", "repository" + File.separator + "resources" +
		                                               File.separator + "security" +
		                                               File.separator + "wso2carbon.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");
		System.setProperty("carbon.repo.write.mode", "true");
		configContext =
		                ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo,
		                                                                                     axis2Conf);
		return new WSRegistryServiceClient(gregServerURL, gregUsername, gregPassword, configContext);
	}

	@POST
	@Path("/cartridge/metadata/{applicationname}/{cartridgetype}")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String addCartridgeMetaDataDetails(@PathParam("applicationname") String applicationName,
	                                          @PathParam("cartridgetype") String cartridgeType,
	                                          CartridgeMetaData cartridgeMetaData) throws Exception {

		String registryType = "GREG";
		return DataRegistryFactory.getDataRegistryFactory(registryType)
		                          .addCartridgeMetaDataDetails(applicationName, cartridgeType,
		                                                       cartridgeMetaData);

	}

	@GET
	@Path("/cartridge/metadata/{applicationname}/{cartridgetype}")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String getCartridgeMetaDataDetails(@PathParam("applicationname") String applicationName,
	                                          @PathParam("cartridgetype") String cartridgeType)

	throws Exception {

		String registryType = "GREG";
		return DataRegistryFactory.getDataRegistryFactory(registryType)
		                          .getCartridgeMetaDataDetails(applicationName, cartridgeType);

	}

	public boolean removeCartridgeMetaDataDetails(String applicationName, String cartridgeType)
	                                                                                           throws Exception {

		String registryType = "GREG";
		return DataRegistryFactory.getDataRegistryFactory(registryType)
		                          .removeCartridgeMetaDataDetails(applicationName, cartridgeType);

	}
}
