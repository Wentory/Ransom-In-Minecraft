package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class HotbarLockMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void ransom$lockHotbar(int slotId, int button, ContainerInput clickType, Player player, CallbackInfo ci) {
        if (!RansomServerState.isHotbarLocked(player)) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        boolean clickedHotbar = slotId >= 0 && slotId < menu.slots.size()
                && isPlayerHotbar(menu.getSlot(slotId), player);
        boolean targetsHotbarByNumber = clickType == ContainerInput.SWAP
                && button >= 0 && button < Inventory.getSelectionSize();
        boolean canMoveIntoHotbarAutomatically = clickType == ContainerInput.QUICK_MOVE
                || clickType == ContainerInput.PICKUP_ALL;

        if (clickedHotbar || targetsHotbarByNumber || canMoveIntoHotbarAutomatically) ci.cancel();
    }

    private static boolean isPlayerHotbar(Slot slot, Player player) {
        return slot.container == player.getInventory()
                && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() < Inventory.getSelectionSize();
    }
}
