package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WormIllusionLayers {
    private static final ResourceLocation GLITCH = new ResourceLocation(RansomInMinecraft.MODID, "textures/glitch.png");
    private WormIllusionLayers() {}
    @SubscribeEvent @SuppressWarnings({"rawtypes", "unchecked"})
    public static void add(EntityRenderersEvent.AddLayers event) {
        for (var type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValues()) {
            LivingEntityRenderer renderer = event.getRenderer((net.minecraft.world.entity.EntityType) type);
            if (renderer != null) renderer.addLayer(new IllusionLayer(renderer));
        }
    }
    private static final class IllusionLayer extends RenderLayer<LivingEntity, EntityModel<LivingEntity>> {
        IllusionLayer(RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> parent) { super(parent); }
        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                                     float swing, float amount, float partial, float age, float yaw, float pitch) {
            if (!WormEffects.illusion(entity) || entity.isInvisible() || entity.isDeadOrDying()) return;
            pose.pushPose();
            pose.scale(1.04F, 1.04F, 1.04F);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))),
                    light, OverlayTexture.NO_OVERLAY, 1.000000F, 0.031373F, 0.031373F, 0.564706F);
            pose.scale(1.015F, 1.015F, 1.015F);
            var noise = new AnimatedSheetVertexConsumer(buffers.getBuffer(RenderType.entityTranslucentEmissive(GLITCH)), entity.tickCount / 2, 6);
            getParentModel().renderToBuffer(pose, noise, 0x00F000F0, OverlayTexture.NO_OVERLAY, 1.000000F, 1.000000F, 1.000000F, 0.800000F);
            pose.popPose();
        }
    }
}
