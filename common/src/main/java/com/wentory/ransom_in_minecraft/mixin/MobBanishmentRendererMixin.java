package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.MobBanishmentVisuals;
import com.wentory.ransom_in_minecraft.client.RenderStateEntities;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
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
    @ModifyArgs(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void ransom$evaporate(Args args){
        float progress=progress((LivingEntityRenderState)args.get(1));
        if(progress<0)return;
        int color=args.get(6);
        int alpha=Math.round((color>>>24)*(1-progress));
        args.set(6,alpha<<24|(progress<.2F?0x93EFFF:color&0xFFFFFF));
    }
}
