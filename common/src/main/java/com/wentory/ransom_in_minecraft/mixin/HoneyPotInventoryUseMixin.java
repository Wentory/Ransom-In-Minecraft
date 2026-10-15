package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.RansomItems;
import com.wentory.ransom_in_minecraft.network.HoneyPotInventoryPayload;
import com.wentory.ransom_in_minecraft.client.HoneyPotInventorySender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HoneyPotInventoryUseMixin {
    @Invoker("getHoveredSlot") protected abstract Slot ransom$hoveredSlot(double x, double y);
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ransom$useHoneyPot(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var screen = (AbstractContainerScreen<?>) (Object) this;
        var slot = ransom$hoveredSlot(event.x(), event.y());
        if (slot != null && slot.container == player.getInventory()
                && com.wentory.ransom_in_minecraft.client.FullHijackClient.active()
                && slot.getItem().is(RansomItems.CRUCIFIX.get())) {
            cir.setReturnValue(true);
            return;
        }
        if (event.button() != 1 || (com.wentory.ransom_in_minecraft.client.FullHijackClient.active()
                && !com.wentory.ransom_in_minecraft.client.FullHijackClient.inventoryUnlocked())) return;
        if (slot == null || slot.container != player.getInventory() || !screen.getMenu().getCarried().isEmpty()
                || !slot.getItem().is(RansomItems.HONEY_POT.get())) return;
        HoneyPotInventorySender.send(new HoneyPotInventoryPayload(slot.getContainerSlot()));
        cir.setReturnValue(true);
    }
}
