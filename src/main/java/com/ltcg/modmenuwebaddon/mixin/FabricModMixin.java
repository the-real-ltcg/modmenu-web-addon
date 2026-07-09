package com.ltcg.modmenuwebaddon.mixin;

import com.ltcg.modmenuwebaddon.data.FetchedModInfo;
import com.ltcg.modmenuwebaddon.data.ModDataCache;
import com.terraformersmc.modmenu.util.mod.fabric.FabricMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fills in whichever of description/website/issue-tracker/source Mod Menu would otherwise show
 * blank, using whatever the background fetcher already found for this mod on Modrinth/CurseForge.
 */
@Mixin(FabricMod.class)
public abstract class FabricModMixin {
	@Shadow
	public abstract String getId();

	@Inject(method = "getDescription", at = @At("RETURN"), cancellable = true)
	private void modmenuwebaddon$fillDescription(CallbackInfoReturnable<String> cir) {
		String current = cir.getReturnValue();
		if (current != null && !current.isBlank()) {
			return;
		}
		FetchedModInfo info = ModDataCache.get(this.getId());
		if (info != null && info.description != null) {
			cir.setReturnValue(info.description);
		}
	}

	@Inject(method = "getWebsite", at = @At("RETURN"), cancellable = true)
	private void modmenuwebaddon$fillWebsite(CallbackInfoReturnable<String> cir) {
		if (cir.getReturnValue() != null) {
			return;
		}
		FetchedModInfo info = ModDataCache.get(this.getId());
		if (info != null && info.website != null) {
			cir.setReturnValue(info.website);
		}
	}

	@Inject(method = "getIssueTracker", at = @At("RETURN"), cancellable = true)
	private void modmenuwebaddon$fillIssueTracker(CallbackInfoReturnable<String> cir) {
		if (cir.getReturnValue() != null) {
			return;
		}
		FetchedModInfo info = ModDataCache.get(this.getId());
		if (info != null && info.issueTracker != null) {
			cir.setReturnValue(info.issueTracker);
		}
	}

	@Inject(method = "getSource", at = @At("RETURN"), cancellable = true)
	private void modmenuwebaddon$fillSource(CallbackInfoReturnable<String> cir) {
		if (cir.getReturnValue() != null) {
			return;
		}
		FetchedModInfo info = ModDataCache.get(this.getId());
		if (info != null && info.source != null) {
			cir.setReturnValue(info.source);
		}
	}
}
