package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.FullHijackDistortion;
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
    private void ransom$closeDistortion(CallbackInfo callback) { FullHijackDistortion.clear(); }
    @Inject(method = "render", at = @At("HEAD"))
    private void ransom$beginFrame(float partial, long nanos, boolean renderLevel, CallbackInfo callback) {
        FullHijackDistortion.beginFrame();
    }
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void ransom$distortBeforeHud(Gui gui, GuiGraphics graphics, float partial) {
        FullHijackDistortion.beforeHud(graphics, partial);
        gui.render(graphics, partial);
    }
}
