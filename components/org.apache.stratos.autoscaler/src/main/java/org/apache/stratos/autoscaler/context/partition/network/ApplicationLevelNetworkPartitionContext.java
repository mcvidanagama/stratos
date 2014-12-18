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
package org.apache.stratos.autoscaler.context.partition.network;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.application.ApplicationInstanceContext;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds runtime data of a network partition.
 *
 */
public class ApplicationLevelNetworkPartitionContext extends NetworkPartitionContext implements Serializable {
    private static final Log log = LogFactory.getLog(ApplicationLevelNetworkPartitionContext.class);
    private final String id;
    private boolean createdOnBurst;

    //group instances kept inside a partition
    private Map<String, ApplicationInstanceContext> instanceIdToInstanceContextMap;

    public ApplicationLevelNetworkPartitionContext(String id) {
        this.id = id;
        this.instanceIdToInstanceContextMap = new HashMap<String, ApplicationInstanceContext>();
    }

    public Map<String, ApplicationInstanceContext> getInstanceIdToInstanceContextMap() {
        return instanceIdToInstanceContextMap;
    }

    public void setInstanceIdToInstanceContextMap(Map<String, ApplicationInstanceContext> instanceIdToInstanceContextMap) {
        this.instanceIdToInstanceContextMap = instanceIdToInstanceContextMap;
    }

    public void addInstanceContext(ApplicationInstanceContext context) {
        this.instanceIdToInstanceContextMap.put(context.getId(), context);

    }


    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;

    }

    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ApplicationLevelNetworkPartitionContext)) {
            return false;
        }
        final ApplicationLevelNetworkPartitionContext other = (ApplicationLevelNetworkPartitionContext) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ApplicationNetworkPartitionContext [id=" + id  + "]";
    }

    public String getId() {
        return id;
    }

    public boolean isCreatedOnBurst() {
        return createdOnBurst;
    }

    public void setCreatedOnBurst(boolean createdOnBurst) {
        this.createdOnBurst = createdOnBurst;
    }

    public void removeClusterApplicationContext(String instanceId) {
        this.instanceIdToInstanceContextMap.remove(instanceId);
    }
}
