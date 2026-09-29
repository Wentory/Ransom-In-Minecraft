package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.RenderStateEntities;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class RenderStateEntityMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("HEAD"))
    private void ransom$remember(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        RenderStateEntities.remember(state, entity);
    }
}
