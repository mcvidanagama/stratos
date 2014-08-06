package org.apache.stratos.metadataservice.registry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.definition.PropertyBean;
import org.apache.stratos.metadataservice.util.ConfUtil;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Comment;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

public class GRegRegistry implements DataStore {

	private static Log log = LogFactory.getLog(GRegRegistry.class);
	@Context
	HttpServletRequest httpServletRequest;

	private static ConfigurationContext configContext = null;

	private static String axis2Repo = "repository/deployment/client";
	private static String axis2Conf = "repository/conf/axis2/axis2_client.xml";

	private static final String defaultUsername = "admin@org.com";
	private static final String defaultPassword = "admin123";
	private static final String serverURL = "https://localhost:9445/services/";
	private static final String mainResource = "/startos/";
	private static final int defaultRank = 3;

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

	@Override
	public String addCartridgeMetaDataDetails(String applicationName, String cartridgeType,
	                                          CartridgeMetaData cartridgeMetaData) throws Exception {
		System.out.println("Adding meta data details");
		Registry registry = setRegistry();
		try {

			Resource resource = registry.newResource();

			String type = cartridgeMetaData.type;

			resource.setContent("Application description :: " + type);

			String resourcePath = mainResource + applicationName + "/" + cartridgeType;

			resource.addProperty("Application Name", cartridgeMetaData.applicationName);
			resource.addProperty("Display Name", cartridgeMetaData.displayName);
			resource.addProperty("Description", cartridgeMetaData.description);
			resource.addProperty("Cartidge Type", cartridgeMetaData.type);
			resource.addProperty("provider", cartridgeMetaData.provider);
			resource.addProperty("Version", cartridgeMetaData.version);
			resource.addProperty("host", cartridgeMetaData.host);

			for (PropertyBean prop : cartridgeMetaData.property) {
				resource.addProperty("hostname", prop.hostname);
				resource.addProperty("username", prop.username);
				resource.addProperty("password", prop.password);
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

	@Override
	public String getCartridgeMetaDataDetails(String applicationName, String cartridgeType)
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

				cartridgeMetaData.type = getResource.getProperty("Cartidge Type");
				cartridgeMetaData.applicationName = getResource.getProperty("Application Name");
				cartridgeMetaData.description = getResource.getProperty("Description");
				cartridgeMetaData.displayName = getResource.getProperty("Display Name");
				cartridgeMetaData.host = getResource.getProperty("host");
				cartridgeMetaData.provider = getResource.getProperty("provider");
				cartridgeMetaData.version = getResource.getProperty("Version");

				List<PropertyBean> lst = new ArrayList<PropertyBean>();
				PropertyBean prop = new PropertyBean();
				prop.hostname = getResource.getProperty("hostname");
				prop.username = getResource.getProperty("username");
				prop.password = getResource.getProperty("password");
				lst.add(prop);

				cartridgeMetaData.property = lst;

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

	@Override
	public boolean removeCartridgeMetaDataDetails(String applicationName, String cartridgeType)
	                                                                                           throws Exception {
		Registry registry = setRegistry();
		String resourcePath = mainResource + applicationName + "/" + cartridgeType;
		registry.delete(resourcePath);
		return false;
	}

}
