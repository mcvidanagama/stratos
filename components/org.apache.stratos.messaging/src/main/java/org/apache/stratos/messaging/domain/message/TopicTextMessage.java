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

package org.apache.stratos.messaging.domain.message;

import java.util.HashMap;

/**
 * Custom text message for internal queue
 */
public class TopicTextMessage  {

	private String textMessage;

	private HashMap<String,String> header;

	public TopicTextMessage(){
		header=new HashMap<String, String>();
	}

	public String getText() {
		return textMessage;
	}

	public void setText(String textMessage) {
		this.textMessage = textMessage;
	}

	public String getStringProperty(String key) {
		return header.get(key);
	}

	public void setStringProperty(String key,String value) {
		header.put(key,value);
	}
}
