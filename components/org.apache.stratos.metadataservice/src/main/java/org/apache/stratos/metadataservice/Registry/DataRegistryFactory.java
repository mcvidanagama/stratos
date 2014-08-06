package org.apache.stratos.metadataservice.Registry;

public class DataRegistryFactory {

	public static DataStore getDataRegistryFactory(String registryName) {
		if (registryName.equals("GREG"))
			return new GRegRegistry();
		else
			return null;
	}

}
