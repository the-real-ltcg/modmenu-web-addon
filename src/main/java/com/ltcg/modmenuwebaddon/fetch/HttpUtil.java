package com.ltcg.modmenuwebaddon.fetch;

import com.ltcg.modmenuwebaddon.ModMenuWebAddonClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class HttpUtil {
	static final String USER_AGENT = "modmenu-web-addon/1.0.0 (fabric client mod)";
	static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private HttpUtil() {
	}

	static byte[] downloadBytes(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.header("User-Agent", USER_AGENT)
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() == 200) {
				return response.body();
			}
		} catch (Exception e) {
			ModMenuWebAddonClient.LOGGER.debug("Failed to download {}", url, e);
		}
		return null;
	}
}
