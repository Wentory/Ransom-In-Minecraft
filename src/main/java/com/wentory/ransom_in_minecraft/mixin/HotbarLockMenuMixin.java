package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class HotbarLockMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void ransom$lockHotbar(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        boolean hotbarLocked = RansomServerState.isHotbarLocked(player);
        boolean clickedHotbar = hotbarLocked && slotId >= 0 && slotId < menu.slots.size()
                && isPlayerHotbar(menu.getSlot(slotId), player);
        boolean clickedEncrypted = slotId >= 0 && slotId < menu.slots.size()
                && isPlayerEncrypted(menu.getSlot(slotId), player);
        boolean targetsHotbarByNumber = hotbarLocked && clickType == ClickType.SWAP
                && button >= 0 && button < Inventory.getSelectionSize();
        boolean canMoveIntoLockedSlotsAutomatically = hotbarLocked
                && (clickType == ClickType.QUICK_MOVE || clickType == ClickType.PICKUP_ALL);

        if (clickedHotbar || clickedEncrypted || targetsHotbarByNumber || canMoveIntoLockedSlotsAutomatically) ci.cancel();
    }

    private static boolean isPlayerHotbar(Slot slot, Player player) {
        return slot.container == player.getInventory()
                && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() < Inventory.getSelectionSize();
    }

    private static boolean isPlayerEncrypted(Slot slot, Player player) {
        return slot.container == player.getInventory()
                && RansomServerState.isInventorySlotEncrypted(player, slot.getContainerSlot());
    }
}
