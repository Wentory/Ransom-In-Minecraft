package com.wentory.ransom_in_minecraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import com.wentory.ransom_in_minecraft.network.RansomServerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class SceneCarriedItemMixin {
    @Inject(method = "ensureHasSentCarriedItem", at = @At("HEAD"), cancellable = true)
    private void ransom$keepLockedSelectionOutOfHotbarProtocol(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && RansomServerState.isHotbarLocked(player)) ci.cancel();
    }
}
