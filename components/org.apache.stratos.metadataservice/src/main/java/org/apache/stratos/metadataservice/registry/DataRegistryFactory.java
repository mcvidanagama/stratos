package org.apache.stratos.metadataservice.registry;

public class DataRegistryFactory {

	public static DataStore getDataRegistryFactory(String registryName) {
		if (registryName.equals("GREG"))
			return new GRegRegistry();
		else
			return null;
	}

}
