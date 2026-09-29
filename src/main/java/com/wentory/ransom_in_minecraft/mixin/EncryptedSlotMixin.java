package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class EncryptedSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void ransom$blockEncryptedPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        if (slot.container instanceof Inventory inventory
                && RansomServerState.isInventorySlotEncrypted(player, slot.getContainerSlot())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void ransom$blockEncryptedPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        if (slot.container instanceof Inventory inventory
                && RansomServerState.isInventorySlotEncrypted(inventory.player, slot.getContainerSlot())) {
            cir.setReturnValue(false);
        }
    }
}
