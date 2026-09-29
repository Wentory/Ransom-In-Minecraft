package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfectedZombieLayers {
    private static final ResourceLocation GLITCH = ResourceLocation.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "textures/glitch.png");

    private InfectedZombieLayers() {}

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        ZombieRenderer renderer = event.getRenderer(EntityType.ZOMBIE);
        if (renderer != null) renderer.addLayer(new InfectionLayer<>(renderer));
        DrownedRenderer drowned = event.getRenderer(EntityType.DROWNED);
        if (drowned != null) drowned.addLayer(new InfectionLayer<>(drowned));
    }

    private static final class InfectionLayer<T extends Zombie, M extends AbstractZombieModel<T>> extends RenderLayer<T, M> {
        private InfectionLayer(RenderLayerParent<T, M> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, T zombie,
                           float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
            if (!ClientInfectedZombies.contains(zombie.getUUID()) || zombie.isInvisible() || zombie.isDeadOrDying()) return;
            M model = getParentModel();
            pose.pushPose();
            pose.scale(1.035F, 1.035F, 1.035F);
            model.renderToBuffer(pose,
                    buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(zombie))),
                    packedLight, OverlayTexture.NO_OVERLAY, 0x76FF1010);
            pose.popPose();
            pose.pushPose();
            pose.scale(1.055F, 1.055F, 1.055F);
            var animated = new AnimatedSheetVertexConsumer(
                    buffers.getBuffer(RenderType.entityTranslucentEmissive(GLITCH)), zombie.tickCount / 2, 6);
            model.renderToBuffer(pose, animated, 0x00F000F0, OverlayTexture.NO_OVERLAY, 0xCCFFFFFF);
            pose.popPose();
        }
    }
}
