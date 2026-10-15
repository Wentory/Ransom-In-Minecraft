package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.WormEffects;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.InputEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public abstract class FabricInteractionMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void ransom$attack(CallbackInfoReturnable<Boolean> ci){
        if(com.wentory.ransom_in_minecraft.client.FullHijackClient.active()){ci.setReturnValue(false);return;}
        if(com.wentory.ransom_in_minecraft.client.CaveRansomClient.attack()){
            var player=Minecraft.getInstance().player;
            if(player!=null) player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            ci.setReturnValue(true);return;
        }
        WormEffects.doubleClick(new InputEvent.InteractionKeyMappingTriggered(0));
    }
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void ransom$use(CallbackInfo ci){
        if(com.wentory.ransom_in_minecraft.client.FullHijackClient.active()){ci.cancel();return;}
        WormEffects.doubleClick(new InputEvent.InteractionKeyMappingTriggered(1));
    }
}
