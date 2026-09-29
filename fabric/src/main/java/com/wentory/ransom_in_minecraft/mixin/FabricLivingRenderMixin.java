package com.wentory.ransom_in_minecraft.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.client.*;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
@SuppressWarnings({"rawtypes","unchecked"})
public abstract class FabricLivingRenderMixin {
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"),cancellable=true)
    private void ransom$before(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){
        float partial=Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if((Object)this instanceof AvatarRenderer<?> avatar && state instanceof AvatarRenderState player){
            var event=new RenderPlayerEvent.Pre(avatar,player,pose,collector,partial);
            InfectedPlayerGlitchRenderer.beforePlayerRender(event);
            if(event.isCanceled()){ci.cancel();return;}
        }
        var event=new RenderLivingEvent.Pre((LivingEntityRenderer)(Object)this,state,pose,collector,partial);
        InfectedCreeperVisuals.face(event);
        if(!event.isCanceled()) InfectedZombieVisuals.beforeRender(event);
        if(event.isCanceled()) ci.cancel();
    }
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("TAIL"))
    private void ransom$after(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){
        float partial=Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if((Object)this instanceof AvatarRenderer<?> avatar && state instanceof AvatarRenderState player)
            InfectedPlayerGlitchRenderer.afterPlayerRender(new RenderPlayerEvent.Post(avatar,player,pose,collector,partial));
        InfectedZombieVisuals.afterRender(new RenderLivingEvent.Post((LivingEntityRenderer)(Object)this,state,pose,collector,partial));
    }
}
