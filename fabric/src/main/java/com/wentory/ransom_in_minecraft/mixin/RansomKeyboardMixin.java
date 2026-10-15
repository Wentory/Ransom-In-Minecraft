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
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void ransom$keyPress(long window, int action, KeyEvent key, CallbackInfo ci) {
        RansomEncounter.keyInput(new com.wentory.ransom_in_minecraft.platform.FabricEvents.InputEvent.Key(key.key(), action));
        if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            var event = new com.wentory.ransom_in_minecraft.platform.FabricEvents.ScreenEvent.KeyPressed.Pre(
                    net.minecraft.client.Minecraft.getInstance().gui.screen(), key.key(), key.scancode());
            com.wentory.ransom_in_minecraft.client.FullHijackClient.inventoryKey(event);
            if (event.isCanceled()) ci.cancel();
        }
    }
}
