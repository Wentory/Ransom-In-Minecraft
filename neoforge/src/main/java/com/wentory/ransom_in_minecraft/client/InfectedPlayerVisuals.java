package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
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

    private static final class InfectionLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
        private InfectionLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                           AvatarRenderState state, float yRot, float xRot) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || state.isInvisible
                    || !(minecraft.level.getEntity(state.id) instanceof Player player)
                    || !ClientInfectionTracker.isInfected(player.getUUID())) return;

            PlayerModel model = getParentModel();
            int frame = ((int) state.ageInTicks / 2) % 6;
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(GLITCH),
                    (pose, vertices) -> {
                        PoseStack replay = new PoseStack();
                        replay.last().set(pose);
                        model.setupAnim(state);
                        model.renderToBuffer(replay,
                                new AnimatedSheetVertexConsumer(vertices, frame, 6, 128),
                                0x00F000F0, OverlayTexture.NO_OVERLAY, 0x80FFFFFF);
                    });
        }
    }
}
