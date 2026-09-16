package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfectedPlayerVisuals {
    private static final Identifier GLITCH = Identifier.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "textures/glitch.png");
    private InfectedPlayerVisuals() {}

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new InfectionLayer(renderer));
        }
    }

    private static final class InfectionLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private InfectionLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                           AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!ClientInfectionTracker.isInfected(player.getUUID()) || player.isInvisible()) return;

            PlayerModel<AbstractClientPlayer> model = getParentModel();
            poseStack.pushPose();
            poseStack.scale(1.035F, 1.035F, 1.035F);
            model.renderToBuffer(poseStack,
                    buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())),
                    packedLight, OverlayTexture.NO_OVERLAY, 0x70FF1010);
            poseStack.popPose();

            int frame = (player.tickCount / 2) % 6;
            var animated = new AnimatedSheetVertexConsumer(
                    buffers.getBuffer(RenderType.entityTranslucentEmissive(GLITCH)), frame, 6);
            model.renderToBuffer(poseStack, animated,
                    0x00F000F0, OverlayTexture.NO_OVERLAY, 0x80FFFFFF);
        }
    }

}
