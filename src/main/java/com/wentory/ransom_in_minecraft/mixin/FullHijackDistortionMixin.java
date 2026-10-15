package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.FullHijackDistortion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class FullHijackDistortionMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void ransom$closeDistortion(CallbackInfo callback) {
        FullHijackDistortion.clear();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void ransom$beginFrame(DeltaTracker delta, boolean renderLevel, CallbackInfo callback) {
        FullHijackDistortion.beginFrame();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void ransom$distortBeforeHud(Gui gui, GuiGraphics graphics, DeltaTracker delta) {
        FullHijackDistortion.beforeHud(graphics, delta);
        gui.render(graphics, delta);
    }
}
