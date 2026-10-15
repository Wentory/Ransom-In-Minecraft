package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.FullHijackClient;
import com.wentory.ransom_in_minecraft.platform.FabricEvents;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class FullHijackScreenMixin {
    @Inject(method="setScreen",at=@At("HEAD"),cancellable=true)
    private void ransom$opening(Screen screen,CallbackInfo ci) {
        var event=new FabricEvents.ScreenEvent.Opening(screen);
        FullHijackClient.opening(event);
        if(event.isCanceled()) ci.cancel();
    }
}
