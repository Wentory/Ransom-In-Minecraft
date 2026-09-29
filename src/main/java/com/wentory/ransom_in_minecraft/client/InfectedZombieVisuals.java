package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class InfectedZombieVisuals {
    private static boolean renderingSlicePass;

    private InfectedZombieVisuals() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity zombie = event.getEntity();
        if (renderingSlicePass || !isGlitchMob(zombie) || zombie.isInvisible()
                || zombie.isDeadOrDying()) return;
        int seed = zombie.getUUID().hashCode();
        int cycle = 25 + Math.floorMod(hash(seed), 24);
        if (Math.floorMod(zombie.tickCount + seed, cycle) >= 2) return;
        if (!renderSlicedZombie(event, zombie, seed)) return;
        renderParticles(event.getPoseStack(), event.getMultiBufferSource(), zombie, seed);
        renderStop(event, zombie);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterRender(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity zombie = event.getEntity();
        if (renderingSlicePass || !(isGlitchMob(zombie)
                || ClientInfectedZombies.isWormVictim(zombie.getUUID())) || zombie.isInvisible()
                || zombie.isDeadOrDying()) return;
        renderParticles(event.getPoseStack(), event.getMultiBufferSource(), zombie, zombie.getUUID().hashCode());
        renderStop(event, zombie);
    }

    private static void renderStop(RenderLivingEvent<?, ?> event, LivingEntity entity) {
        if (!(entity instanceof Zombie)) return;
        float remaining = ClientInfectedZombies.stopRemaining(entity.getUUID(), event.getPartialTick());
        if (remaining <= 0) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 front = camera.subtract(entity.getPosition(event.getPartialTick()));
        double length = Math.hypot(front.x, front.z);
        pose.pushPose();
        pose.translate(length > 0.001D ? front.x / length * 0.5D : 0,
                entity.getBbHeight() * 0.6D, length > 0.001D ? front.z / length * 0.5D : 0.5D);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        VertexConsumer c = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucentEmissive(
                new ResourceLocation(RansomInMinecraft.MODID, "textures/block.png")));
        int alpha = Math.round(Mth.clamp(remaining / 2.0F, 0, 1) * 255);
        float size = 0.55F;
        c.vertex(pose.last().pose(), -size, -size, 0).color(255, 255, 255, alpha).uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(pose.last().normal(), 0, 0, 1).endVertex();
        c.vertex(pose.last().pose(), size, -size, 0).color(255, 255, 255, alpha).uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(pose.last().normal(), 0, 0, 1).endVertex();
        c.vertex(pose.last().pose(), size, size, 0).color(255, 255, 255, alpha).uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(pose.last().normal(), 0, 0, 1).endVertex();
        c.vertex(pose.last().pose(), -size, size, 0).color(255, 255, 255, alpha).uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0x00F000F0).normal(pose.last().normal(), 0, 0, 1).endVertex();
        pose.popPose();
    }

    private static boolean isGlitchMob(LivingEntity entity) {
        return entity instanceof Zombie && ClientInfectedZombies.contains(entity.getUUID())
                || entity instanceof Creeper && InfectedCreeperVisuals.phase(entity.getUUID()) >= 0
                && InfectedCreeperVisuals.phase(entity.getUUID()) < 2;
    }

    private static boolean renderSlicedZombie(RenderLivingEvent.Pre<?, ?> event, LivingEntity zombie, int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (width <= 0 || height <= 0) return false;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 position = zombie.getPosition(event.getPartialTick());
        Quaternionf rotation = new Quaternionf(minecraft.gameRenderer.getMainCamera().rotation());
        float feet = screenY(position, camera, rotation, 0.0F, height);
        float head = screenY(position, camera, rotation, zombie.getBbHeight(), height);
        if (!Float.isFinite(feet) || !Float.isFinite(head)) return false;
        int bottom = Mth.clamp((int) Math.floor(Math.min(feet, head)), 0, height);
        int top = Mth.clamp((int) Math.ceil(Math.max(feet, head)), 0, height);
        int span = top - bottom;
        if (span < 10) return false;

        int beat = hash(seed ^ zombie.tickCount / 2);
        int bandHeight = Math.max(3, Math.round(span * 0.16F));
        int first = bottom + Math.floorMod(hash(beat + 1), Math.max(1, span - bandHeight));
        int second = bottom + Math.floorMod(hash(beat + 2), Math.max(1, span - bandHeight));
        if (second < first) {
            int swap = first;
            first = second;
            second = swap;
        }
        second = Math.max(first + bandHeight, second);
        second = Math.min(top, second);
        float shiftPixels = Math.min(40.0F, Math.max(7.0F, span * 0.14F));
        float depth = Math.max(0.5F, (float) position.distanceTo(camera));
        float focalPixels = Math.max(1.0F,
                height * 0.5F * Math.abs(RenderSystem.getProjectionMatrix().m11()));
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(rotation);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        buffers.endBatch();
        if (event.getMultiBufferSource() instanceof MultiBufferSource.BufferSource eventBuffers
                && eventBuffers != buffers) eventBuffers.endBatch();
        renderingSlicePass = true;
        try {
            renderBand(event, zombie, buffers, width, 0, first,
                    0.0F, 1.0F, depth, focalPixels, right, rotation);
            renderBand(event, zombie, buffers, width, first, first + bandHeight,
                    shiftPixels, 1.14F, depth, focalPixels, right, rotation);
            renderBand(event, zombie, buffers, width, first + bandHeight, second,
                    0.0F, 1.0F, depth, focalPixels, right, rotation);
            if ((beat & 3) != 0) renderBand(event, zombie, buffers, width, second,
                    Math.min(top, second + bandHeight), -shiftPixels * 0.75F,
                    0.86F, depth, focalPixels, right, rotation);
            renderBand(event, zombie, buffers, width, second + bandHeight, height,
                    0.0F, 1.0F, depth, focalPixels, right, rotation);
        } finally {
            RenderSystem.disableScissor();
            renderingSlicePass = false;
        }
        return true;
    }

    private static float screenY(Vec3 position, Vec3 camera, Quaternionf rotation,
                                 float localY, int framebufferHeight) {
        Vec3 relative = position.subtract(camera);
        Vector3f view = new Vector3f((float) relative.x, (float) relative.y + localY,
                (float) relative.z).rotate(new Quaternionf(rotation).conjugate());
        Vector4f clip = new Vector4f(view, 1.0F).mul(RenderSystem.getProjectionMatrix());
        if (clip.w <= 0.0F) return Float.NaN;
        return (clip.y / clip.w + 1.0F) * framebufferHeight * 0.5F;
    }

    private static void renderBand(RenderLivingEvent.Pre<?, ?> event, LivingEntity zombie,
                                   MultiBufferSource.BufferSource buffers, int width, int bottom, int top,
                                   float offsetPixels, float stretch, float depth, float focalPixels,
                                   Vector3f right, Quaternionf rotation) {
        if (top <= bottom) return;
        RenderSystem.enableScissor(0, bottom, width, top - bottom);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            if (offsetPixels != 0.0F) {
                float shift = offsetPixels * depth / focalPixels;
                pose.translate(right.x * shift, right.y * shift, right.z * shift);
            }
            if (stretch != 1.0F) {
                pose.mulPose(rotation);
                pose.scale(stretch, 1.0F, 1.0F);
                pose.mulPose(new Quaternionf(rotation).conjugate());
            }
            @SuppressWarnings("rawtypes")
            LivingEntityRenderer renderer = event.getRenderer();
            renderer.render(zombie, Mth.rotLerp(event.getPartialTick(), zombie.yRotO, zombie.getYRot()),
                    event.getPartialTick(), pose, buffers, event.getPackedLight());
            buffers.endBatch();
        } finally {
            pose.popPose();
            RenderSystem.disableScissor();
        }
    }

    private static void renderParticles(PoseStack pose, MultiBufferSource buffers, LivingEntity zombie, int seed) {
        pose.pushPose();
        pose.translate(0, zombie.getBbHeight() * 0.5D, 0);
        GlitchParticles.render(pose, buffers, seed, zombie.tickCount, 7,
                0.55F, zombie.getBbHeight() * 0.48F, 0.4F, 0.8F, 200);
        pose.popPose();
    }
    private static int hash(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ value >>> 16;
    }
}
