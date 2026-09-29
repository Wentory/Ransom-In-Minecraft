package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.model.EntityModel;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


public final class WormIllusionLayers {
    private static final Identifier GLITCH = Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "textures/glitch.png");
    private WormIllusionLayers() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void add(EntityRenderersEvent.AddLayers event) {
        for (var type : event.getEntityTypes()) {
            if (event.getRenderer(type) instanceof LivingEntityRenderer renderer) renderer.addLayer(new IllusionLayer(renderer));
        }
    }
    private static final class IllusionLayer extends RenderLayer<net.minecraft.client.renderer.entity.state.LivingEntityRenderState, EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState>> {
        IllusionLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.LivingEntityRenderState, EntityModel<net.minecraft.client.renderer.entity.state.LivingEntityRenderState>> parent) { super(parent); }
        @Override public void submit(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light,
 net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, float yaw, float pitch) {
 if (!(RenderStateEntities.get(state) instanceof LivingEntity entity)) return;
 DeferredBuffers buffers = new DeferredBuffers(collector);
 getParentModel().setupAnim(state);
            if (!WormEffects.illusion(entity) || entity.isInvisible() || entity.isDeadOrDying()) return;
            pose.pushPose();
            pose.scale(1.04F, 1.04F, 1.04F);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(((LivingEntityRenderer) net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity)).getTextureLocation(state))),
                    light, OverlayTexture.NO_OVERLAY, 0x90FF0808);
            pose.scale(1.015F, 1.015F, 1.015F);
            var noise = new AnimatedSheetVertexConsumer(buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(GLITCH)), entity.tickCount / 2, 6);
            getParentModel().renderToBuffer(pose, noise, 0x00F000F0, OverlayTexture.NO_OVERLAY, 0xCCFFFFFF);
            pose.popPose();
            buffers.endBatch();
        }
    }
}
