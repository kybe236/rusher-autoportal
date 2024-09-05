package org.kybe;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * Portal Maker
 *
 * @author kybe236
 */
public class Main extends Plugin {
	
	@Override
	public void onLoad() {
		//creating and registering a new module
		final PortalMakerModule portalMakerModule = new PortalMakerModule();
		RusherHackAPI.getModuleManager().registerFeature(portalMakerModule);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("Example plugin unloaded!");
	}
	
}