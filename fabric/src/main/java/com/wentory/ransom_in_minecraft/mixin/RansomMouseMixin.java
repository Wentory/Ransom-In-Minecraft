package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class RansomMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void ransom$mouseButton(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        RansomEncounter.mouseInput(new com.wentory.ransom_in_minecraft.platform.FabricEvents.InputEvent.MouseButton.Pre(action));
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ransom$blockHotbarScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (RansomEncounter.handsLocked()) ci.cancel();
    }
}