package com.ltcg.modmenuwebaddon.fetch;

import com.ltcg.modmenuwebaddon.ModMenuWebAddonClient;
import com.ltcg.modmenuwebaddon.config.AddonConfig;
import com.ltcg.modmenuwebaddon.data.FetchedModInfo;
import com.ltcg.modmenuwebaddon.data.ModDataCache;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scans every loaded mod for gaps in the metadata Mod Menu displays (description, website,
 * issue tracker, source, icon) and, for whichever fields are missing, kicks off a background
 * lookup against Modrinth (and CurseForge, if configured) to fill them in. Results land in
 * {@link ModDataCache}, which the mixins read from when Mod Menu asks for a mod's info.
 */
public final class ModInfoFetcher {
	// "mixinextras" and "fabricloader" are loader-internal support libraries, not mods with real
	// Modrinth/CurseForge listings of their own — searching for them by name tends to false-match
	// unrelated projects (e.g. "MixinExtras" once matched an unrelated 1.13.2 Rift mod called
	// "Modern Mixins" purely because it was the only search hit).
	private static final Set<String> SKIP_IDS = Set.of("minecraft", "java", "modmenuwebaddon", "mixinextras", "fabricloader");

	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, threadFactory());

	private ModInfoFetcher() {
	}

	public static void fetchMissingInBackground() {
		AddonConfig config = AddonConfig.get();
		if (!config.enabled) {
			return;
		}
		Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
		for (ModContainer container : mods) {
			ModMetadata meta = container.getMetadata();
			String modId = meta.getId();
			if (shouldSkip(container, modId, meta) || ModDataCache.isFresh(modId)) {
				continue;
			}
			EXECUTOR.submit(() -> fetchOne(config, container, meta, modId));
		}
	}

	private static boolean shouldSkip(ModContainer container, String modId, ModMetadata meta) {
		if (SKIP_IDS.contains(modId)) {
			return true;
		}
		if ("builtin".equals(meta.getType())) {
			return true;
		}
		// Jar-in-jar nested mods are implementation details bundled inside another mod (like
		// mixinextras inside fabricloader) rather than independent projects worth looking up.
		if (container.getContainingMod().isPresent()) {
			return true;
		}
		// Fabric API sub-modules: dozens of these load per instance, and Mod Menu already
		// collapses them under the "Fabric API" parent entry, so there's nothing to fill in.
		return meta.containsCustomValue("fabric-api:module-lifecycle");
	}

	private static void fetchOne(AddonConfig config, ModContainer container, ModMetadata meta, String modId) {
		try {
			boolean missingDescription = config.fillMissingDescription && isBlank(meta.getDescription());
			boolean missingWebsite = config.fillMissingLinks && meta.getContact().get("homepage").isEmpty();
			boolean missingIssues = config.fillMissingLinks && meta.getContact().get("issues").isEmpty();
			boolean missingSource = config.fillMissingLinks && meta.getContact().get("sources").isEmpty();
			boolean missingIcon = config.fillMissingIcon && !hasLocalIcon(container, meta, modId);

			if (!missingDescription && !missingWebsite && !missingIssues && !missingSource && !missingIcon) {
				return;
			}

			FetchedModInfo info = ModrinthClient.fetch(modId, meta.getName());
			if (needsMore(info, missingDescription, missingWebsite, missingIssues, missingSource, missingIcon)
					&& config.curseForgeConfigured()) {
				FetchedModInfo cfInfo = CurseForgeClient.fetch(config.curseForgeApiKey, modId, meta.getName());
				info = merge(info, cfInfo);
			}

			if (info == null || info.isEmpty()) {
				return;
			}
			if (!missingDescription) info.description = null;
			if (!missingWebsite) info.website = null;
			if (!missingIssues) info.issueTracker = null;
			if (!missingSource) info.source = null;
			if (!missingIcon || !info.hasIcon) {
				info.hasIcon = false;
			} else {
				ModDataCache.saveIcon(modId, info.iconBytes);
			}
			ModDataCache.put(modId, info);
		} catch (Exception e) {
			ModMenuWebAddonClient.LOGGER.warn("Failed to fetch web info for mod {}", modId, e);
		}
	}

	private static boolean needsMore(FetchedModInfo info, boolean missingDescription, boolean missingWebsite,
			boolean missingIssues, boolean missingSource, boolean missingIcon) {
		if (info == null) {
			return true;
		}
		return (missingDescription && info.description == null)
				|| (missingWebsite && info.website == null)
				|| (missingIssues && info.issueTracker == null)
				|| (missingSource && info.source == null)
				|| (missingIcon && !info.hasIcon);
	}

	private static FetchedModInfo merge(FetchedModInfo primary, FetchedModInfo fallback) {
		if (primary == null) {
			return fallback;
		}
		if (fallback == null) {
			return primary;
		}
		if (primary.description == null) primary.description = fallback.description;
		if (primary.website == null) primary.website = fallback.website;
		if (primary.issueTracker == null) primary.issueTracker = fallback.issueTracker;
		if (primary.source == null) primary.source = fallback.source;
		if (!primary.hasIcon && fallback.hasIcon) {
			primary.hasIcon = true;
			primary.iconBytes = fallback.iconBytes;
		}
		return primary;
	}

	private static boolean hasLocalIcon(ModContainer container, ModMetadata meta, String modId) {
		try {
			String iconPath = meta.getIconPath(64).orElse("assets/" + modId + "/icon.png");
			Path resolved = container.getPath(iconPath);
			return resolved != null && Files.isRegularFile(resolved);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static ThreadFactory threadFactory() {
		AtomicInteger counter = new AtomicInteger();
		return runnable -> {
			Thread thread = new Thread(runnable, "modmenu-web-addon-fetch-" + counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		};
	}
}
