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
package org.apache.stratos.cloud.controller.receiver.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.applications.ApplicationTerminatedEvent;
import org.apache.stratos.messaging.listener.applications.ApplicationTerminatedEventListener;
import org.apache.stratos.messaging.message.receiver.applications.ApplicationsEventReceiver;

/**
 * This is to receive the application topic messages.
 */
public class ApplicationTopicReceiver {
	private static final Log log = LogFactory.getLog(ApplicationTopicReceiver.class);
	private ApplicationsEventReceiver applicationsEventReceiver;
	private boolean terminated;

	public ApplicationTopicReceiver() {
		this.applicationsEventReceiver = new ApplicationsEventReceiver();
		addEventListeners();

	}

	public void execute() {

		if (log.isInfoEnabled()) {
			log.info("Cloud controller application status thread started");
		}
		applicationsEventReceiver.execute();

		if (log.isInfoEnabled()) {
			log.info("Cloud controller application status thread terminated");
		}

	}

	private void addEventListeners() {
		applicationsEventReceiver.addEventListener(new ApplicationTerminatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				//Remove the application related data
				ApplicationTerminatedEvent terminatedEvent = (ApplicationTerminatedEvent) event;
				log.info("ApplicationTerminatedEvent received for [application] " + terminatedEvent.getAppId());
				String appId = terminatedEvent.getAppId();
				TopologyBuilder.handleApplicationClustersRemoved(appId, terminatedEvent.getClusterData());
			}
		});
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}
}