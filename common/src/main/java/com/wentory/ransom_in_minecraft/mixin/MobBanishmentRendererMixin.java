package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.MobBanishmentVisuals;
import com.wentory.ransom_in_minecraft.client.RenderStateEntities;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntityRenderer.class)
public abstract class MobBanishmentRendererMixin {
    @Shadow public abstract net.minecraft.resources.Identifier getTextureLocation(LivingEntityRenderState state);
    private static float progress(LivingEntityRenderState state){
        var entity=RenderStateEntities.get(state);
        return entity instanceof LivingEntity living?MobBanishmentVisuals.progress(living,Math.clamp(state.ageInTicks-living.tickCount,0,1)):-1;
    }
    @Inject(method="getRenderType",at=@At("RETURN"),cancellable=true)
    private void ransom$transparent(LivingEntityRenderState state,boolean visible,boolean transparent,boolean glowing,CallbackInfoReturnable<RenderType> ci){
        if(progress(state)>=0 && visible)ci.setReturnValue(RenderTypes.entityTranslucent(getTextureLocation(state)));
    }
    @ModifyVariable(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at=@At("HEAD"), argsOnly=true, ordinal=0)
    private net.minecraft.client.renderer.SubmitNodeCollector ransom$banishmentCollector(
            net.minecraft.client.renderer.SubmitNodeCollector original, LivingEntityRenderState state,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        float progress=progress(state);
        return progress<0 ? original : new com.wentory.ransom_in_minecraft.client.BanishmentSubmitCollector(original, progress);
    }
}
