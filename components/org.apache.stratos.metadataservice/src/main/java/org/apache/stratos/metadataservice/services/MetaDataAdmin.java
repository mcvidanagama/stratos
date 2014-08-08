package org.apache.stratos.metadataservice.services;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.annotation.AuthorizationAction;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.definition.PropertyBean;
import org.apache.stratos.metadataservice.exception.RestAPIException;
import org.apache.stratos.metadataservice.util.ConfUtil;
<<<<<<< HEAD
<<<<<<< HEAD
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Comment;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
=======
>>>>>>> b65a712... updates
=======
>>>>>>> b65a712... updates

@Path("/metadataservice/")
public class MetaDataAdmin {

	private static Log log = LogFactory.getLog(MetaDataAdmin.class);
	@Context
	HttpServletRequest httpServletRequest;

<<<<<<< HEAD
<<<<<<< HEAD
	private static ConfigurationContext configContext = null;

	private static final String CARBON_HOME = "/../../../../";
	private static String axis2Repo = "repository/deployment/client";
	private static String axis2Conf = "repository/conf/axis2/axis2_client.xml";

	private static final String username = "admin@org.com";
	private static final String password = "admin123";
	private static final String serverURL = "https://localhost:9445/services/";
	private static final String mainResource = "/startos/";
	private static final int defaultRank = 3;
=======
=======
>>>>>>> b65a712... updates
	private final String defaultRegType = "GREG";

	private XMLConfiguration conf;
>>>>>>> b65a712... updates

	@POST
	@Path("/init")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public void initialize() throws RestAPIException {

	}

<<<<<<< HEAD
<<<<<<< HEAD
	private static WSRegistryServiceClient setRegistry() throws Exception {

		XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
		String gregUsername = conf.getString("metadataservice.username", "admin@org.com");
		String gregPassword = conf.getString("metadataservice.password", "admin@org.com");

		System.setProperty("javax.net.ssl.trustStore", "repository" + File.separator + "resources" +
		                                               File.separator + "security" +
		                                               File.separator + "wso2carbon.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");
		System.setProperty("carbon.repo.write.mode", "true");
		configContext =
		                ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo,
		                                                                                     axis2Conf);
		return new WSRegistryServiceClient(serverURL, gregUsername, gregPassword, configContext);
	}

=======
>>>>>>> b65a712... updates
=======
>>>>>>> b65a712... updates
	@POST
	@Path("/cartridge/metadata/{applicationname}/{cartridgetype}")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String addCartridgeMetaDataDetails(@PathParam("applicationname") String applicationName,
	                                          @PathParam("cartridgetype") String cartridgeType,
	                                          CartridgeMetaData cartridgeMetaData) throws Exception {
		System.out.println("Adding meta data details");
		Registry registry = setRegistry();
		try {

			Resource resource = registry.newResource();

			String type = cartridgeMetaData.type;

			resource.setContent("Application description :: " + type);

			String resourcePath = mainResource + applicationName + "/" + cartridgeType;

			resource.addProperty("Application Name", cartridgeMetaData.applicationName);
			resource.addProperty("Cartidge Type", cartridgeMetaData.type);

			for (PropertyBean prop : cartridgeMetaData.property) {
				resource.addProperty(prop.name, prop.value);
			}

			registry.put(resourcePath, resource);

			System.out.println("A resource added to: " + resourcePath);

			System.out.println(cartridgeMetaData.type);
			registry.rateResource(resourcePath, defaultRank);

			Comment comment = new Comment();
			comment.setText("Added the " + applicationName + " " + type + " cartridge");
			registry.addComment(resourcePath, comment);

		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			// Close the session
			((WSRegistryServiceClient) registry).logut();
		}
		System.out.println("Add meta data details");
		return "success";

	}

	@GET
	@Path("/cartridge/metadata/{applicationname}/{cartridgetype}")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String getPartition(@PathParam("applicationname") String applicationName,
	                           @PathParam("cartridgetype") String cartridgeType)

	throws Exception {
		Registry registry = setRegistry();
		CartridgeMetaData cartridgeMetaData = new CartridgeMetaData();
		try {

			String resourcePath = mainResource + applicationName + "/" + cartridgeType;
			if (registry.resourceExists(resourcePath)) {

				Resource getResource = registry.get(resourcePath);
				System.out.println("Resource retrived");
				System.out.println("Printing retrieved resource content: " +
				                   new String((byte[]) getResource.getContent()));
			}

		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			// Close the session
			((WSRegistryServiceClient) registry).logut();
		}
		return cartridgeMetaData.toString();
	}
}
