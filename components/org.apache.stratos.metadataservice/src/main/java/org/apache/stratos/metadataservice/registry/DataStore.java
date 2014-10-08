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

import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.definition.NewProperty;
import org.wso2.carbon.registry.api.RegistryException;

import java.util.List;

/*
 * Interface of the Data Store
 */
public interface DataStore {
	public void addCartridgeMetaDataDetails(String applicationName, String cartridgeType,
	                                          CartridgeMetaData cartridgeMetaData) throws Exception;

	public String getCartridgeMetaDataDetails(String applicationName, String cartridgeType)
	                                                                                       throws Exception;

	public boolean removeCartridgeMetaDataDetails(String applicationName, String cartridgeType)
	                                                                                           throws Exception;

    public void addPropertiesToCluster(String applicationName, String clusterId, NewProperty[] properties)
            throws RegistryException;
    public List<NewProperty> getPropertiesOfCluster(String applicationName, String clusterId)
            throws RegistryException;

    public void addPropertyToCluster(String applicationId, String clusterId, NewProperty property) throws RegistryException;

    void addPropertiesToApplication(String applicationId, NewProperty[] properties) throws RegistryException;

    void addPropertyToApplication(String applicationId, NewProperty property) throws RegistryException;

    List<NewProperty> getPropertiesOfApplication(String applicationId) throws RegistryException;
}
