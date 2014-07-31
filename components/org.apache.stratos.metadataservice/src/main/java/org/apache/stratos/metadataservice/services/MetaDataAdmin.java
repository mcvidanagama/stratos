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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.annotation.AuthorizationAction;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.exception.RestAPIException;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Comment;
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

	private static final String username = "admin@org.com";
	private static final String password = "admin123";
	private static final String serverURL = "https://localhost:9445/services/";

	@POST
	@Path("/init")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public void initialize() throws RestAPIException {

	}

	private static WSRegistryServiceClient setRegistry() throws Exception {

		System.setProperty("javax.net.ssl.trustStore", "repository" + File.separator + "resources" +
		                                               File.separator + "security" +
		                                               File.separator + "wso2carbon.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
		System.setProperty("javax.net.ssl.trustStoreType", "JKS");
		System.setProperty("carbon.repo.write.mode", "true");
		configContext =
		                ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo,
		                                                                                     axis2Conf);
		return new WSRegistryServiceClient(serverURL, username, password, configContext);
	}

	@POST
	@Path("/cartridge/metadata")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String addCartridgeMetaDataDetails(CartridgeMetaData cartridgeMetaData) throws Exception {
		System.out.println("Adding meta data details");
		Registry registry = setRegistry();
		try {

			Resource resource = registry.newResource();

			String type = cartridgeMetaData.type;

			resource.setContent("Hello Out there!" + type);

			String resourcePath = "/startos/app-3/" + type;

			resource.addProperty("id", "ewwtreiwet");
			resource.addProperty("name", "admin");

			registry.put(resourcePath, resource);

			System.out.println("A resource added to: " + resourcePath);

			System.out.println(cartridgeMetaData.type);
			registry.rateResource(resourcePath, 3);

			System.out.println("Resource rated with 3 stars!");
			Comment comment = new Comment();
			comment.setText("Wow! A comment out there");
			registry.addComment(resourcePath, comment);
			System.out.println("Comment added to resource");

			Resource getResource = registry.get("/startos/app-2");
			System.out.println("Resource retrived");
			System.out.println("Printing retrieved resource content: " +
			                   new String((byte[]) getResource.getContent()));

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
		try {

			Resource getResource = registry.get("/startos/" + cartridgeType);
			System.out.println("Resource retrived");
			System.out.println("Printing retrieved resource content: " +
			                   new String((byte[]) getResource.getContent()));

		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			// Close the session
			((WSRegistryServiceClient) registry).logut();
		}
		return "result";
	}
}
