package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class StopMouseInputMixin {
    @Shadow private boolean ignoreFirstMove;
    @Shadow private double xpos;
    @Shadow private double ypos;

    // Read before vanilla updates xpos/ypos and clears ignoreFirstMove.
    @Inject(method="onMove", at=@At("HEAD"))
    private void ransom$physicalMouseMove(long window, double x, double y, CallbackInfo ci) {
        Minecraft minecraft=Minecraft.getInstance();
        if (window==minecraft.getWindow().handle()) {
            RansomEncounter.mouseMoved(minecraft.isWindowActive(), ignoreFirstMove, xpos, ypos, x, y);
        }
    }
}
