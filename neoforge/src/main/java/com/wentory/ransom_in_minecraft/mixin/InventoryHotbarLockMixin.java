package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryHotbarLockMixin {
    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void ransom$getUnlockedFreeSlot(CallbackInfoReturnable<Integer> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (!RansomServerState.isHotbarLocked(inventory.player)) return;
        for (int slot = Inventory.getSelectionSize(); slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                cir.setReturnValue(slot);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void ransom$getUnlockedStackSlot(ItemStack incoming, CallbackInfoReturnable<Integer> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (!RansomServerState.isHotbarLocked(inventory.player)) return;
        for (int slot = Inventory.getSelectionSize(); slot < 36; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(existing, incoming)
                    && existing.getCount() < existing.getMaxStackSize()) {
                cir.setReturnValue(slot);
                return;
            }
        }
        cir.setReturnValue(-1);
    }
}
