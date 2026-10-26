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
    // Do not invent a selected slot: other mods enumerate the real hotbar by index.
    // Mask only the equipped view; indexed inventory access and item ownership stay intact.
    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void ransom$emptyLockedMainHand(CallbackInfoReturnable<ItemStack> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (RansomServerState.isHotbarLocked(inventory.player)) cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void ransom$getUnlockedFreeSlot(CallbackInfoReturnable<Integer> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (!RansomServerState.isHotbarLocked(inventory.player)
                && !RansomServerState.hasEncryptedInventorySlots(inventory.player)) return;
        for (int slot = Inventory.getSelectionSize(); slot < 36; slot++) {
            if (!RansomServerState.isInventorySlotEncrypted(inventory.player, slot)
                    && inventory.getItem(slot).isEmpty()) {
                cir.setReturnValue(slot);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void ransom$getUnlockedStackSlot(ItemStack incoming, CallbackInfoReturnable<Integer> cir) {
        Inventory inventory = (Inventory) (Object) this;
        if (!RansomServerState.isHotbarLocked(inventory.player)
                && !RansomServerState.hasEncryptedInventorySlots(inventory.player)) return;

        for (int slot = Inventory.getSelectionSize(); slot < 36; slot++) {
            if (RansomServerState.isInventorySlotEncrypted(inventory.player, slot)) continue;
            ItemStack existing = inventory.getItem(slot);
            if (ItemStack.isSameItemSameTags(existing, incoming)
                    && existing.getCount() < existing.getMaxStackSize()) {
                cir.setReturnValue(slot);
                return;
            }
        }
        cir.setReturnValue(-1);
    }
}
