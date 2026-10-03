package com.wentory.ransom_in_minecraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class SceneCarriedItemMixin {
    @Inject(method = "ensureHasSentCarriedItem", at = @At("HEAD"), cancellable = true)
    private void ransom$keepVirtualSlotOutOfHotbarProtocol(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getInventory().selected == Inventory.getSelectionSize()) ci.cancel();
    }
}
