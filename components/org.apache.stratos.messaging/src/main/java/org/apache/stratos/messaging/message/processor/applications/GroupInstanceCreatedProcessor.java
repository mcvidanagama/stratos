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
package org.apache.stratos.messaging.message.processor.applications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Applications;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.event.applications.GroupInstanceCreatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.applications.updater.ApplicationsUpdater;
import org.apache.stratos.messaging.util.Util;

/**
 * This processor will act upon the Group activation events
 */
public class GroupInstanceCreatedProcessor extends MessageProcessor {
    private static final Log log = LogFactory.getLog(GroupInstanceCreatedProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Applications applications = (Applications) object;

        if (GroupInstanceCreatedEvent.class.getName().equals(type)) {
            // Return if applications has not been initialized
            if (!applications.isInitialized())
                return false;

            // Parse complete message and build event
            GroupInstanceCreatedEvent event = (GroupInstanceCreatedEvent) Util.
                    jsonToObject(message, GroupInstanceCreatedEvent.class);

            ApplicationsUpdater.acquireWriteLockForApplication(event.getAppId());

            try {
                return doProcess(event, applications);

            } finally {
                ApplicationsUpdater.releaseWriteLockForApplication(event.getAppId());
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, applications);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess(GroupInstanceCreatedEvent event, Applications applications) {

        // Validate event against the existing applications
        Application application = applications.getApplication(event.getAppId());
        if (application == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Application does not exist: [service] %s",
                        event.getAppId()));
            }
            return false;
        }
        Group group = application.getGroupRecursively(event.getGroupId());

        if (group == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Group not exists in service: [AppId] %s [groupId] %s", event.getAppId(),
                        event.getGroupId()));
                return false;
            }
        } else {
            // Apply changes to the applications
            String instanceId = event.getGroupInstance().getInstanceId();
            GroupInstance context = group.getInstanceContexts(instanceId);
            if(context != null) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Group Instance already exists in Group: [AppId] %s [groupId] %s " +
                                    "[instanceId] %s", event.getAppId(), event.getGroupId(),
                            instanceId));
                    return false;
                }
            }
            // Apply changes to the topology
            group.addInstance(instanceId, event.getGroupInstance());
        }

        // Notify event listeners
        notifyEventListeners(event);
        return true;
    }
}
