package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


public final class InfectedPlayerVisuals {
    private static final Identifier GLITCH = Identifier.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "textures/glitch.png");
    private InfectedPlayerVisuals() {}


    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            AvatarRenderer<?> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) renderer.addLayer(new InfectionLayer(renderer));
        }
    }

    private static final class InfectionLayer extends RenderLayer<net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> {
        private InfectionLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight,
 net.minecraft.client.renderer.entity.state.AvatarRenderState renderState, float netHeadYaw, float headPitch) {
 if (!(RenderStateEntities.get(renderState) instanceof AbstractClientPlayer player)) return;
 DeferredBuffers buffers = new DeferredBuffers(collector);
 getParentModel().setupAnim(renderState);
            if (player.isInvisible() || player.isDeadOrDying()) return;

            boolean infected = ClientInfectionTracker.isInfected(player.getUUID());
            float extraGlitch = InfectedPlayerGlitchRenderer.extraGlitchStrength(player);
            float success = InfectedPlayerGlitchRenderer.successStrength(player);
            if (!infected && extraGlitch <= 0.0F && success <= 0.0F) return;

            PlayerModel model = getParentModel();
            int frame = (player.tickCount / 2) % 6;
            if (infected) {
                poseStack.pushPose();
                poseStack.scale(1.035F, 1.035F, 1.035F);
                model.renderToBuffer(poseStack,
                        buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(renderState.skin.body().texturePath())),
                        packedLight, OverlayTexture.NO_OVERLAY, 0x70FF1010);
                poseStack.popPose();

                var animated = new AnimatedSheetVertexConsumer(
                        buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(GLITCH)), frame, 6);
                model.renderToBuffer(poseStack, animated,
                        0x00F000F0, OverlayTexture.NO_OVERLAY, 0x80FFFFFF);
            }

            if (extraGlitch > 0.0F) {
                int alpha = Math.min(255, 90 + Math.round(extraGlitch * 150.0F));
                int copies = extraGlitch > 0.7F ? 3 : 1;
                for (int copy = 0; copy < copies; copy++) {
                    float direction = copy % 2 == 0 ? -1.0F : 1.0F;
                    float offset = direction * (0.012F + copy * 0.009F) * extraGlitch;
                    poseStack.pushPose();
                    poseStack.translate(offset, (copy - 1) * 0.006F * extraGlitch, -0.004F * copy);
                    var burst = new AnimatedSheetVertexConsumer(
                            buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(GLITCH)),
                            frame + copy * 2, 6, alpha);
                    model.renderToBuffer(poseStack, burst,
                            0x00F000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    poseStack.popPose();
                }

                if (extraGlitch > 0.7F && (player.tickCount & 1) == 0) {
                    poseStack.pushPose();
                    poseStack.scale(1.055F, 1.025F, 1.055F);
                    model.renderToBuffer(poseStack,
                            buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(renderState.skin.body().texturePath())),
                            0x00F000F0, OverlayTexture.NO_OVERLAY,
                            (Math.round(115.0F * extraGlitch) << 24) | 0x00FF0808);
                    poseStack.popPose();
                }
            }

            if (success > 0.0F) {
                int alpha = Math.round((90.0F + ((player.tickCount & 1) == 0 ? 100.0F : 35.0F)) * success);
                poseStack.pushPose();
                poseStack.scale(1.04F, 1.04F, 1.04F);
                model.renderToBuffer(poseStack,
                        buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(renderState.skin.body().texturePath())),
                        0x00F000F0, OverlayTexture.NO_OVERLAY,
                        (Mth.clamp(alpha, 0, 220) << 24) | 0x0030FF55);
                poseStack.popPose();
            }
            buffers.endBatch();
        }
    }
}
