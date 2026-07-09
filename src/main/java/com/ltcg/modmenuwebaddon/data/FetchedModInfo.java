package com.ltcg.modmenuwebaddon.data;

public class FetchedModInfo {
	public String description;
	public String website;
	public String issueTracker;
	public String source;
	public boolean hasIcon;
	public long fetchedAt;

	/** Raw icon bytes, only populated between a fetch and {@link com.ltcg.modmenuwebaddon.data.ModDataCache#saveIcon}; never persisted in the JSON cache. */
	public transient byte[] iconBytes;

	public boolean isEmpty() {
		return description == null && website == null && issueTracker == null && source == null && !hasIcon;
	}
}
