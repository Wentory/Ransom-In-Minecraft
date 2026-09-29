package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.RenderLivingEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class InfectedZombieVisuals {
    private static boolean renderingSlicePass;

    private InfectedZombieVisuals() {}

    public static void beforeRender(RenderLivingEvent.Pre<?, ?, ?> event) {
        if (!(RenderStateEntities.get(event.getRenderState()) instanceof LivingEntity zombie)) return;
        if (renderingSlicePass || !isGlitchMob(zombie) || zombie.isInvisible()
                || zombie.isDeadOrDying()) return;
        int seed = zombie.getUUID().hashCode();
        int cycle = 25 + Math.floorMod(hash(seed), 24);
        if (Math.floorMod(zombie.tickCount + seed, cycle) >= 2) return;
        if (!renderSlicedZombie(event, zombie, seed)) return;
        DeferredBuffers buffers = new DeferredBuffers(event.getSubmitNodeCollector());
        renderParticles(event.getPoseStack(), buffers, zombie, seed);
        buffers.endBatch();
        renderStop(event, zombie);
        event.setCanceled(true);
    }

    public static void afterRender(RenderLivingEvent.Post<?, ?, ?> event) {
        if (!(RenderStateEntities.get(event.getRenderState()) instanceof LivingEntity zombie)) return;
        if (renderingSlicePass || !(isGlitchMob(zombie)
                || ClientInfectedZombies.isWormVictim(zombie.getUUID())) || zombie.isInvisible()
                || zombie.isDeadOrDying()) return;
        DeferredBuffers buffers = new DeferredBuffers(event.getSubmitNodeCollector());
        renderParticles(event.getPoseStack(), buffers, zombie, zombie.getUUID().hashCode());
        buffers.endBatch();
        renderStop(event, zombie);
    }

    private static void renderStop(RenderLivingEvent<?, ?, ?> event, LivingEntity entity) {
        if (!(entity instanceof Zombie)) return;
        float remaining = ClientInfectedZombies.stopRemaining(entity.getUUID(), event.getPartialTick());
        if (remaining <= 0) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Vec3 front = camera.subtract(entity.getPosition(event.getPartialTick()));
        double length = Math.hypot(front.x, front.z);
        pose.pushPose();
        pose.translate(length > 0.001D ? front.x / length * 0.5D : 0,
                entity.getBbHeight() * 0.6D, length > 0.001D ? front.z / length * 0.5D : 0.5D);
        pose.mulPose(Minecraft.getInstance().gameRenderer.mainCamera().rotation());
        DeferredBuffers buffers = new DeferredBuffers(event.getSubmitNodeCollector());
        VertexConsumer c = buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(
                Identifier.fromNamespaceAndPath(RansomFabric.MODID, "textures/block.png")));
        int alpha = Math.round(Mth.clamp(remaining / 2.0F, 0, 1) * 255);
        float size = 0.55F;
        c.addVertex(pose.last().pose(), -size, -size, 0).setColor(255, 255, 255, alpha).setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose.last(), 0, 0, 1);
        c.addVertex(pose.last().pose(), size, -size, 0).setColor(255, 255, 255, alpha).setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose.last(), 0, 0, 1);
        c.addVertex(pose.last().pose(), size, size, 0).setColor(255, 255, 255, alpha).setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose.last(), 0, 0, 1);
        c.addVertex(pose.last().pose(), -size, size, 0).setColor(255, 255, 255, alpha).setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose.last(), 0, 0, 1);
        pose.popPose();
        buffers.endBatch();
    }

    private static boolean isGlitchMob(LivingEntity entity) {
        return entity instanceof Zombie && ClientInfectedZombies.contains(entity.getUUID())
                || entity instanceof Creeper && InfectedCreeperVisuals.phase(entity.getUUID()) >= 0
                && InfectedCreeperVisuals.phase(entity.getUUID()) < 2;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean renderSlicedZombie(RenderLivingEvent.Pre<?, ?, ?> event, LivingEntity zombie, int seed) {
        float height=zombie.getBbHeight();
        java.util.List<GeometryDistortion.Band> bands=new java.util.ArrayList<>();
        int beat=hash(seed^zombie.tickCount/2);
        float first=height*(0.2F+Math.floorMod(beat,20)/100F);
        float second=height*0.7F;
        bands.add(new GeometryDistortion.Band(-0.5F,first,0,1,false));
        bands.add(new GeometryDistortion.Band(first,first+height*0.16F,0.25F,1.14F,false));
        bands.add(new GeometryDistortion.Band(first+height*0.16F,second,0,1,false));
        bands.add(new GeometryDistortion.Band(second,second+height*0.16F,-0.18F,0.86F,(beat&3)==0));
        bands.add(new GeometryDistortion.Band(second+height*0.16F,height+0.7F,0,1,false));
        var matrix=event.getPoseStack().last().pose();
        Vector3f right=new Vector3f(1,0,0).rotate(Minecraft.getInstance().gameRenderer.mainCamera().rotation());
        var collector=GeometryDistortion.wrap(event.getSubmitNodeCollector(),matrix.m31(),matrix.m30(),matrix.m32(),right,bands);
        renderingSlicePass=true;
        try {
            LivingEntityRenderer renderer=event.getRenderer();
            renderer.submit(event.getRenderState(),event.getPoseStack(),collector,
                    Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState);
        } finally { renderingSlicePass=false; }
        return true;
    }
    private static void renderParticles(PoseStack pose, DeferredBuffers buffers, LivingEntity zombie, int seed) {
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
