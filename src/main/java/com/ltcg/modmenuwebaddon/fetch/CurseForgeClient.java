package com.ltcg.modmenuwebaddon.fetch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ltcg.modmenuwebaddon.ModMenuWebAddonClient;
import com.ltcg.modmenuwebaddon.data.FetchedModInfo;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Talks to the CurseForge Core API (docs.curseforge.com). This requires a personal API key
 * (free, from console.curseforge.com) supplied by the user in the addon config; CurseForge's
 * terms don't allow redistributing a shared key, so unlike {@link ModrinthClient} this source
 * is opt-in and only used when the user has configured their own key.
 */
public final class CurseForgeClient {
	private static final int MINECRAFT_GAME_ID = 432;
	private static final int MODS_CLASS_ID = 6;

	private CurseForgeClient() {
	}

	public static FetchedModInfo fetch(String apiKey, String modId, String modName) {
		try {
			String query = modName != null && !modName.isBlank() ? modName : modId;
			HttpRequest request = HttpRequest.newBuilder(URI.create(
					"https://api.curseforge.com/v1/mods/search?gameId=" + MINECRAFT_GAME_ID
							+ "&classId=" + MODS_CLASS_ID
							+ "&slug=" + urlEncode(modId)))
					.header("x-api-key", apiKey)
					.header("Accept", "application/json")
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> response = HttpUtil.CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			JsonArray data = response.statusCode() == 200
					? JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data")
					: null;

			if (data == null || data.isEmpty()) {
				// Exact slug search found nothing; fall back to a free-text name search.
				data = searchByName(apiKey, query);
			}
			if (data == null || data.isEmpty()) {
				return null;
			}
			return toFetchedInfo(data.get(0).getAsJsonObject());
		} catch (Exception e) {
			ModMenuWebAddonClient.LOGGER.debug("CurseForge lookup failed for {}", modId, e);
			return null;
		}
	}

	private static JsonArray searchByName(String apiKey, String name) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(
				"https://api.curseforge.com/v1/mods/search?gameId=" + MINECRAFT_GAME_ID
						+ "&classId=" + MODS_CLASS_ID
						+ "&searchFilter=" + urlEncode(name)
						+ "&pageSize=5"))
				.header("x-api-key", apiKey)
				.header("Accept", "application/json")
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();
		HttpResponse<String> response = HttpUtil.CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200) {
			return null;
		}
		return JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("data");
	}

	private static FetchedModInfo toFetchedInfo(JsonObject mod) {
		FetchedModInfo info = new FetchedModInfo();
		info.description = getString(mod, "summary");
		JsonObject links = mod.has("links") && mod.get("links").isJsonObject() ? mod.getAsJsonObject("links") : null;
		if (links != null) {
			info.website = getString(links, "websiteUrl");
			info.issueTracker = getString(links, "issuesUrl");
			info.source = getString(links, "sourceUrl");
		}
		if (info.website == null) {
			String slug = getString(mod, "slug");
			if (slug != null) {
				info.website = "https://www.curseforge.com/minecraft/mc-mods/" + slug;
			}
		}
		JsonObject logo = mod.has("logo") && mod.get("logo").isJsonObject() ? mod.getAsJsonObject("logo") : null;
		if (logo != null) {
			String iconUrl = getString(logo, "url");
			if (iconUrl != null) {
				byte[] bytes = HttpUtil.downloadBytes(iconUrl);
				info.hasIcon = bytes != null;
				info.iconBytes = bytes;
			}
		}
		return info;
	}

	private static String getString(JsonObject obj, String key) {
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		String value = el.getAsString();
		return value.isBlank() ? null : value;
	}

	private static String urlEncode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}
