package com.ltcg.modmenuwebaddon.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ltcg.modmenuwebaddon.ModMenuWebAddonClient;
import com.ltcg.modmenuwebaddon.config.AddonConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory table of fetched mod info, mirrored to disk so results survive between launches
 * (Modrinth/CurseForge are only re-queried once {@link AddonConfig#cacheTtlHours} has elapsed).
 */
public class ModDataCache {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, FetchedModInfo>>() {}.getType();

	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("modmenuwebaddon");
	private static final Path CACHE_FILE = DIR.resolve("cache.json");
	private static final Path ICON_DIR = DIR.resolve("icons");

	private static final Map<String, FetchedModInfo> ENTRIES = load();

	private ModDataCache() {
	}

	private static Map<String, FetchedModInfo> load() {
		if (Files.exists(CACHE_FILE)) {
			try {
				String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
				Map<String, FetchedModInfo> loaded = GSON.fromJson(json, MAP_TYPE);
				if (loaded != null) {
					return new ConcurrentHashMap<>(loaded);
				}
			} catch (IOException | RuntimeException e) {
				ModMenuWebAddonClient.LOGGER.warn("Could not read modmenuwebaddon cache, starting fresh", e);
			}
		}
		return new ConcurrentHashMap<>();
	}

	public static synchronized void save() {
		try {
			Files.createDirectories(DIR);
			Files.writeString(CACHE_FILE, GSON.toJson(ENTRIES, MAP_TYPE), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ModMenuWebAddonClient.LOGGER.warn("Could not write modmenuwebaddon cache", e);
		}
	}

	public static FetchedModInfo get(String modId) {
		return ENTRIES.get(modId);
	}

	public static boolean isFresh(String modId) {
		FetchedModInfo info = ENTRIES.get(modId);
		if (info == null) {
			return false;
		}
		long ttlMillis = AddonConfig.get().cacheTtlHours * 3600_000L;
		return (System.currentTimeMillis() - info.fetchedAt) < ttlMillis;
	}

	public static void put(String modId, FetchedModInfo info) {
		info.fetchedAt = System.currentTimeMillis();
		ENTRIES.put(modId, info);
		save();
	}

	public static Path iconPath(String modId) {
		return ICON_DIR.resolve(modId + ".png");
	}

	public static void saveIcon(String modId, byte[] pngBytes) throws IOException {
		Files.createDirectories(ICON_DIR);
		Files.write(iconPath(modId), pngBytes);
	}

	public static boolean hasIconFile(String modId) {
		return Files.isRegularFile(iconPath(modId));
	}
}
