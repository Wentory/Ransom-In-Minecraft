package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class RansomKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void ransom$keyPress(long window, int action, KeyEvent key, CallbackInfo ci) {
        RansomEncounter.keyInput(new com.wentory.ransom_in_minecraft.platform.FabricEvents.InputEvent.Key(key.key(), action));
    }
}