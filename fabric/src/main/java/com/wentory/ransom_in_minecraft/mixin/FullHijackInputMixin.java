package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.FullHijackClient;
import com.wentory.ransom_in_minecraft.platform.FabricEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class FullHijackInputMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void ransom$bodyInput(CallbackInfo ci) {
        var player=Minecraft.getInstance().player;
        if(player!=null) FullHijackClient.movement(new FabricEvents.MovementInputUpdateEvent(player,(KeyboardInput)(Object)this));
    }
}
