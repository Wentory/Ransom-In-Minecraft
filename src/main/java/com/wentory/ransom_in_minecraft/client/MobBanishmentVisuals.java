package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import com.wentory.ransom_in_minecraft.network.MobBanishmentPayload;
import com.wentory.ransom_in_minecraft.network.WormCreeperPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class MobBanishmentVisuals {
    private static final Map<UUID, Long> STARTS = new HashMap<>();
    private MobBanishmentVisuals() {}
    public static void accept(MobBanishmentPayload payload) {
        MoonlightEffects.mob(payload);
        STARTS.put(payload.id(), payload.start());
        ClientInfectedZombies.remove(payload.id());
        InfectedCreeperVisuals.accept(new WormCreeperPayload(payload.id(), -1));
    }
    public static void clear() { STARTS.clear(); }
    public static float progress(LivingEntity entity, float partial) {
        Long start = STARTS.get(entity.getUUID());
        if (start == null) return -1;
        return net.minecraft.util.Mth.clamp((entity.level().getGameTime() - start + partial)
                / com.wentory.ransom_in_minecraft.CrucifixActions.MOB_BANISHMENT_TICKS, 0, 1);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void before(RenderLivingEvent.Pre<?, ?> event) {
        float progress = progress(event.getEntity(), event.getPartialTick());
        if (progress < 0) return;
        if (progress >= 1) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
    public static final class Layers {
        public static void add(EntityRenderersEvent.AddLayers event) {
            attach(event.getRenderer(EntityType.ZOMBIE));
            attach(event.getRenderer(EntityType.DROWNED));
            attach(event.getRenderer(EntityType.CREEPER));
        }
        private static <T extends LivingEntity, M extends EntityModel<T>> void attach(LivingEntityRenderer<T, M> renderer) {
            if (renderer != null) renderer.addLayer(new FlashLayer<>(renderer));
        }
    }
    private static final class FlashLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        FlashLayer(LivingEntityRenderer<T, M> parent) { super(parent); }
        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                                     float swing, float amount, float partial, float age, float yaw, float pitch) {
            float progress = progress(entity, partial);
            if (progress < 0 || progress >= 0.2F) return;
            int alpha = Math.round(255 * net.minecraft.util.Mth.clamp((0.2F - progress) / 0.1F, 0, 1));
            pose.pushPose();
            pose.scale(1.008F, 1.008F, 1.008F);
            getParentModel().renderToBuffer(pose,
                    buffers.getBuffer(RansomRenderTypes.banishmentFlash()),
                    0x00F000F0, OverlayTexture.NO_OVERLAY, 85 / 255F, 221 / 255F, 1, alpha / 255F);
            pose.popPose();
        }
    }
}
