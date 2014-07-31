package org.apache.stratos.metadataservice.definition;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cartridgeMetaData")
public class CartridgeMetaData {
	public String type;

	public String host;

	public String provider;

	public String displayName;

	public String description;

	public String version;

	public List<PropertyBean> property;

	@Override
	public String toString() {

		return "Type: " + type + ", Provider: " + provider + ", Host: " + host +
		       ", Display Name: " + displayName + ", Description: " + description + ", Version: " +
		       version + ", Multitenant " + getProperties();
	}

	private String getProperties() {

		StringBuilder propertyBuilder = new StringBuilder();
		if (property != null) {
			for (PropertyBean propertyBean : property) {
				propertyBuilder.append(propertyBean.name + " : " + propertyBean.value + " | ");
			}
		}
		return propertyBuilder.toString();
	}
}
