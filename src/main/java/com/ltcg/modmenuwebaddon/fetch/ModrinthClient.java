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
 * Talks to the public, unauthenticated Modrinth API (docs.modrinth.com/api) to fill in
 * whatever a mod's own fabric.mod.json is missing.
 */
public final class ModrinthClient {
	private ModrinthClient() {
	}

	public static FetchedModInfo fetch(String modId, String modName) {
		FetchedModInfo info = fetchProject(modId);
		if (info == null) {
			String bestProjectId = searchForProjectId(modId, modName);
			if (bestProjectId != null) {
				info = fetchProject(bestProjectId);
			}
		}
		return info;
	}

	private static FetchedModInfo fetchProject(String idOrSlug) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + urlEncode(idOrSlug)))
					.header("User-Agent", HttpUtil.USER_AGENT)
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> response = HttpUtil.CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				return null;
			}
			JsonObject project = JsonParser.parseString(response.body()).getAsJsonObject();
			return toFetchedInfo(project);
		} catch (Exception e) {
			ModMenuWebAddonClient.LOGGER.debug("Modrinth project lookup failed for {}", idOrSlug, e);
			return null;
		}
	}

	private static String searchForProjectId(String modId, String modName) {
		try {
			String query = urlEncode(modName != null && !modName.isBlank() ? modName : modId);
			String facets = urlEncode("[[\"project_type:mod\"]]");
			HttpRequest request = HttpRequest.newBuilder(URI.create(
					"https://api.modrinth.com/v2/search?query=" + query + "&facets=" + facets + "&limit=5"))
					.header("User-Agent", HttpUtil.USER_AGENT)
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> response = HttpUtil.CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				return null;
			}
			JsonArray hits = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("hits");
			if (hits == null || hits.isEmpty()) {
				return null;
			}
			for (JsonElement hitElement : hits) {
				JsonObject hit = hitElement.getAsJsonObject();
				String slug = getString(hit, "slug");
				if (slug != null && slug.equalsIgnoreCase(modId)) {
					return getString(hit, "project_id");
				}
			}
			// No exact slug match: only trust the top hit if its title is plainly the same mod
			// (ignoring case/punctuation) rather than just "the only/best result search returned".
			// Otherwise a query like "MixinExtras" can confidently latch onto an unrelated project
			// (e.g. a different, long-abandoned mod that merely shares no real similarity at all).
			JsonObject topHit = hits.get(0).getAsJsonObject();
			String title = getString(topHit, "title");
			if (title != null && looksLikeSameMod(title, modName != null ? modName : modId)) {
				return getString(topHit, "project_id");
			}
			return null;
		} catch (Exception e) {
			ModMenuWebAddonClient.LOGGER.debug("Modrinth search failed for {}", modId, e);
			return null;
		}
	}

	private static boolean looksLikeSameMod(String title, String name) {
		String normalizedTitle = normalize(title);
		String normalizedName = normalize(name);
		if (normalizedTitle.isEmpty() || normalizedName.isEmpty()) {
			return false;
		}
		return normalizedTitle.equals(normalizedName)
				|| normalizedTitle.contains(normalizedName)
				|| normalizedName.contains(normalizedTitle);
	}

	private static String normalize(String s) {
		return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static FetchedModInfo toFetchedInfo(JsonObject project) {
		FetchedModInfo info = new FetchedModInfo();
		info.description = getString(project, "description");
		info.issueTracker = getString(project, "issues_url");
		info.source = getString(project, "source_url");
		String wiki = getString(project, "wiki_url");
		String slug = getString(project, "slug");
		info.website = wiki != null ? wiki : (slug != null ? "https://modrinth.com/mod/" + slug : null);
		String iconUrl = getString(project, "icon_url");
		if (iconUrl != null) {
			byte[] bytes = HttpUtil.downloadBytes(iconUrl);
			info.hasIcon = bytes != null;
			info.iconBytes = bytes;
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
