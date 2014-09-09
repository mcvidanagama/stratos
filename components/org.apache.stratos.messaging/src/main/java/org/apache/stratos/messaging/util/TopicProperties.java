package org.apache.stratos.messaging.util;

import java.io.InputStream;
import java.util.Properties;

public class TopicProperties {

	private static class TopicPropertiessHolder {
		public static TopicProperties webServiceURLs = new TopicProperties();
	}

	private Properties webServiceURLs;
	private InputStream input = null;

	public TopicProperties() {
		try {
			Properties newURLProperties = new Properties();
			input =
			        TopicProperties.class.getClassLoader()
			                             .getResourceAsStream("mqtttopic.properties");
			newURLProperties.load(input);
			webServiceURLs = newURLProperties;
		} catch (Exception e) {
			webServiceURLs = null;
		}
	}

	public String getValueFromKey(String urlKey) {
		if (webServiceURLs == null)
			return null;
		else
			return webServiceURLs.getProperty(urlKey);
	}

	public static TopicProperties getInstance() {
		return TopicPropertiessHolder.webServiceURLs;
	}

}
