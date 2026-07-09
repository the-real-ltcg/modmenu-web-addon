package com.ltcg.modmenuwebaddon;

import com.ltcg.modmenuwebaddon.fetch.ModInfoFetcher;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModMenuWebAddonClient implements ClientModInitializer {
	public static final String MOD_ID = "modmenuwebaddon";
	public static final Logger LOGGER = LoggerFactory.getLogger("Mod Menu Web Addon");

	@Override
	public void onInitializeClient() {
		ModInfoFetcher.fetchMissingInBackground();
	}
}
