/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.grouping.definitions;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "groups")
public class ServiceGroupDefinition implements Serializable {

    private String name;

    private List<ServiceGroupDefinition> groups;

    private List<String> cartridges;

    private boolean isGroupScalingEnabled;

    private DependencyDefinitions dependencies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCartridges() {
        return cartridges;
    }

    public void setCartridges(List<String> cartridges) {
        this.cartridges = cartridges;
    }

    public DependencyDefinitions getDependencies() {
        return dependencies;
    }

    public void setDependencies(DependencyDefinitions dependencies) {
        this.dependencies = dependencies;
    }

    public boolean isGroupScalingEnabled() {
        return isGroupScalingEnabled;
    }

    public void setGroupScalingEnabled(boolean isGroupScalingEnabled) {
        this.isGroupScalingEnabled = isGroupScalingEnabled;
    }

    public List<ServiceGroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(List<ServiceGroupDefinition> groups) {
        this.groups = groups;
    }
}
