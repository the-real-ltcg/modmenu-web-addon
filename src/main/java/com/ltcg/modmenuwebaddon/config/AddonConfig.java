package com.ltcg.modmenuwebaddon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddonConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("modmenuwebaddon.json");

	public boolean enabled = true;
	public boolean fillMissingDescription = true;
	public boolean fillMissingIcon = true;
	public boolean fillMissingLinks = true;
	public boolean useCurseForge = false;
	public String curseForgeApiKey = "";
	public long cacheTtlHours = 24 * 7;

	private static AddonConfig instance;

	public static AddonConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static AddonConfig load() {
		if (Files.exists(PATH)) {
			try {
				String json = Files.readString(PATH, StandardCharsets.UTF_8);
				AddonConfig loaded = GSON.fromJson(json, AddonConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | RuntimeException ignored) {
				// Fall through to defaults; a corrupt/unreadable config shouldn't crash the game.
			}
		}
		AddonConfig fresh = new AddonConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException ignored) {
			// Non-fatal: config just won't persist this run.
		}
	}

	public boolean curseForgeConfigured() {
		return useCurseForge && curseForgeApiKey != null && !curseForgeApiKey.isBlank();
	}
}
