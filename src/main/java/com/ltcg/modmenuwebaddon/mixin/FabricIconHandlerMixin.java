package com.ltcg.modmenuwebaddon.mixin;

import com.ltcg.modmenuwebaddon.ModMenuWebAddonClient;
import com.ltcg.modmenuwebaddon.data.ModDataCache;
import com.mojang.blaze3d.platform.NativeImage;
import com.terraformersmc.modmenu.util.mod.fabric.FabricIconHandler;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * When Mod Menu can't find a mod's own icon file, falls back to whichever icon the background
 * fetcher already downloaded for that mod from Modrinth/CurseForge, if any.
 */
@Mixin(FabricIconHandler.class)
public abstract class FabricIconHandlerMixin {
	@Shadow
	abstract DynamicTexture getCachedModIcon(Path path);

	@Shadow
	abstract void cacheModIcon(Path path, DynamicTexture tex);

	@Inject(method = "createIcon", at = @At("RETURN"), cancellable = true)
	private void modmenuwebaddon$fallbackIcon(ModContainer iconSource, String iconPath, CallbackInfoReturnable<DynamicTexture> cir) {
		if (cir.getReturnValue() != null) {
			return;
		}
		String modId = iconSource.getMetadata().getId();
		Path cachedFile = ModDataCache.iconPath(modId);
		if (!Files.isRegularFile(cachedFile)) {
			return;
		}
		DynamicTexture cached = this.getCachedModIcon(cachedFile);
		if (cached != null) {
			cir.setReturnValue(cached);
			return;
		}
		try (InputStream in = Files.newInputStream(cachedFile)) {
			NativeImage image = NativeImage.read(in);
			if (image.getWidth() != image.getHeight()) {
				image.close();
				return;
			}
			DynamicTexture tex = new DynamicTexture(
					() -> Identifier.fromNamespaceAndPath(ModMenuWebAddonClient.MOD_ID, "cached_icon/" + modId).toString(),
					image);
			this.cacheModIcon(cachedFile, tex);
			cir.setReturnValue(tex);
		} catch (IOException e) {
			ModMenuWebAddonClient.LOGGER.debug("Failed to load cached icon for {}", modId, e);
		}
	}
}
