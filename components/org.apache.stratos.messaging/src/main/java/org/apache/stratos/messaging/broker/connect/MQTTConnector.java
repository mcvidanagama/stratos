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

package org.apache.stratos.messaging.broker.connect;

import javax.jms.JMSException;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

/**
 * This class is responsible for loading the mqtt config file from the
 * classpath
 * and initialize the topic connection. Later if some other object needs a topic
 * session, this object is capable of providing one.
 * 
 */
public class MQTTConnector {

	private static MqttClient topicClient;

	private static MqttClient topicClientSub;

	public static synchronized MqttClient getMQTTConClient() {
		if (topicClient == null) {
			String broker = "tcp://localhost:1883";
			String clientId = "Stratos";
			MemoryPersistence persistence = new MemoryPersistence();

			try {
				topicClient = new MqttClient(broker, clientId, persistence);
				MqttConnectOptions connOpts = new MqttConnectOptions();
				connOpts.setCleanSession(true);
				System.out.println("Connecting to broker: " + broker);
				// topicClient.connect();

				System.out.println("Connected");

			} catch (MqttException me) {
				System.out.println("reason " + me.getReasonCode());
				System.out.println("msg " + me.getMessage());
				System.out.println("loc " + me.getLocalizedMessage());
				System.out.println("cause " + me.getCause());
				System.out.println("excep " + me);
				me.printStackTrace();
			}

		}
		return topicClient;

	}

	public static synchronized MqttClient getMQTTSubClient(String identifier) {
		// if (topicClientSub == null) {

		String broker = "tcp://localhost:1883";

		// Creating new default persistence for mqtt client
		MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence("/tmp");

		try {
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			// mqtt client with specific url and a random client id
			topicClientSub = new MqttClient(broker, identifier, persistence);

			System.out.println("Connecting to subscribe broker: " + broker);
			// topicClient.connect();

			System.out.println("Connected");

		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();
		}

		// }
		return topicClientSub;

	}

	public void close() throws JMSException, MqttException {
		if (topicClient == null) {
			return;
		}
		// topicClientSub.disconnect();
		topicClient.disconnect();
	}

}
