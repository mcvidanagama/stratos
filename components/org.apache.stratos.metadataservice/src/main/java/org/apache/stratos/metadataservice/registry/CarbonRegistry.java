/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadataservice.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Comment;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

/**
 * Factory class for carbon registry
 */
public class CarbonRegistry extends AbstractAdmin implements DataStore {


    private static final String mainResource = "/startos/";

    private static Log log = LogFactory.getLog(CarbonRegistry.class);

    @Context
    HttpServletRequest httpServletRequest;


    public CarbonRegistry() {

    }

    /*
     * Add the meta data to governance registry
     *
     * @see org.apache.stratos.metadataservice.registry.DataStore#
     * addCartridgeMetaDataDetails(java.lang.String, java.lang.String,
     * org.apache.stratos.metadataservice.definition.CartridgeMetaData)
     */
    @Override
    public String addCartridgeMetaDataDetails(String applicationName, String cartridgeType,
                                              CartridgeMetaData cartridgeMetaData) throws Exception {

        Registry tempRegistry = getGovernanceUserRegistry();
        try {

            Resource resource = tempRegistry.newResource();

            String type = cartridgeMetaData.type;

            resource.setContent("Application description :: " + type);

            String resourcePath = mainResource + applicationName + "/" + cartridgeType;

            resource.addProperty("Application Name", cartridgeMetaData.applicationName);
            resource.addProperty("Display Name", cartridgeMetaData.displayName);
            resource.addProperty("Description", cartridgeMetaData.description);
            resource.addProperty("Cartidge Type", cartridgeMetaData.type);
            resource.addProperty("Provider", cartridgeMetaData.provider);
            resource.addProperty("Version", cartridgeMetaData.version);
            resource.addProperty("Host", cartridgeMetaData.host);

            resource.addProperty("Property", cartridgeMetaData.property);

            tempRegistry.put(resourcePath, resource);

            if (log.isDebugEnabled()) {
                log.debug("A resource added to: " + resourcePath);
            }

            Comment comment = new Comment();
            comment.setText("Added the " + applicationName + " " + type + " cartridge");
            // registry.addComment(resourcePath, comment);

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
	            String msg="Add CartridgeMeta Data Details Failed";
                log.error(msg, e);
            }
        }

        return "success";
    }

    /*
     * Get the meta data from the registry
     *
     * @see org.apache.stratos.metadataservice.registry.DataStore#
     * getCartridgeMetaDataDetails(java.lang.String, java.lang.String)
     */
    @Override
    public String getCartridgeMetaDataDetails(String applicationName, String cartridgeType)
            throws Exception {
        Registry registry = getGovernanceUserRegistry();
        CartridgeMetaData cartridgeMetaData = new CartridgeMetaData();
        try {

            String resourcePath = mainResource + applicationName + "/" + cartridgeType;
            if (registry.resourceExists(resourcePath)) {

                Resource getResource = registry.get(resourcePath);

                cartridgeMetaData.type = getResource.getProperty("Cartidge Type");
                cartridgeMetaData.applicationName = getResource.getProperty("Application Name");
                cartridgeMetaData.description = getResource.getProperty("Description");
                cartridgeMetaData.displayName = getResource.getProperty("Display Name");
                cartridgeMetaData.host = getResource.getProperty("Host");
                cartridgeMetaData.provider = getResource.getProperty("Provider");
                cartridgeMetaData.version = getResource.getProperty("Version");
                cartridgeMetaData.property = getResource.getProperty("Property");

            }

        } catch (Exception e) {

            if (log.isErrorEnabled()) {
	            String msg="Get CartridgeMeta Data Details Failed";
                log.error(msg, e);
            }
        }
        return cartridgeMetaData.toString();
    }

    /*
     *
     * Remove the meta data from the registry
     *
     * @see org.apache.stratos.metadataservice.registry.DataStore#
     * removeCartridgeMetaDataDetails(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeCartridgeMetaDataDetails(String applicationName, String cartridgeType)
            throws Exception {
        Registry registry = getGovernanceUserRegistry();
        String resourcePath = mainResource + applicationName + "/" + cartridgeType;
        registry.delete(resourcePath);
        return false;
    }

}
