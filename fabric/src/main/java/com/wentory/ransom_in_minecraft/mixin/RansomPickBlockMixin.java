package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class RansomPickBlockMixin {
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void ransom$blockPick(CallbackInfo ci) {
        if (RansomEncounter.handsLocked()) ci.cancel();
    }
}