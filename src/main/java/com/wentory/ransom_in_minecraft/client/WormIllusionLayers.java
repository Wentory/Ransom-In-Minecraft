package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WormIllusionLayers {
    private WormIllusionLayers() {}
    @SubscribeEvent @SuppressWarnings({"rawtypes", "unchecked"})
    public static void add(EntityRenderersEvent.AddLayers event) {
        for (var type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (event.getEntityRenderer(type) instanceof LivingEntityRenderer renderer)
                renderer.addLayer(new IllusionLayer(renderer));
        }
    }
    private static final class IllusionLayer extends RenderLayer<LivingEntity, EntityModel<LivingEntity>> {
        IllusionLayer(RenderLayerParent<LivingEntity, EntityModel<LivingEntity>> parent) { super(parent); }
        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                                     float swing, float amount, float partial, float age, float yaw, float pitch) {
            if (!WormEffects.illusion(entity) || entity.isInvisible() || entity.isDeadOrDying()) return;
            HijackedMobSurface.render(getParentModel(), getTextureLocation(entity), pose, buffers,
                    light, entity.tickCount, 0x90FF0808);
        }
    }
}
