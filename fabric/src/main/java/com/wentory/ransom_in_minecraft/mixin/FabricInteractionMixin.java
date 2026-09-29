package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.WormEffects;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.InputEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public abstract class FabricInteractionMixin {
    @Inject(method="startAttack",at=@At("HEAD"))
    private void ransom$attack(CallbackInfoReturnable<Boolean> ci){WormEffects.doubleClick(new InputEvent.InteractionKeyMappingTriggered(0));}
    @Inject(method="startUseItem",at=@At("HEAD"))
    private void ransom$use(CallbackInfo ci){WormEffects.doubleClick(new InputEvent.InteractionKeyMappingTriggered(1));}
}
