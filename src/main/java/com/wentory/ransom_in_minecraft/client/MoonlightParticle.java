package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A real engine-managed particle: one fixed star sprite, smooth motion and alpha envelope. */
final class MoonlightParticle extends Particle {
    private static final ParticleRenderType[] TYPES = {
            new StarRenderType("tiny"), new StarRenderType("small"),
            new StarRenderType("normal"), new StarRenderType("big")
    };
    private static boolean compatiblePass;
    private final ParticleRenderType type;
    private final float size;
    private final MoonlightEffects.Profile profile;
    private final double outwardX, outwardZ;
    private final boolean pulsing;
    private final float pulsePhase, pulseSpeed, pulseAmplitude;
    private final double originY, heightFadeStart, heightFadeEnd;
    private final float rotationSpeed;

    MoonlightParticle(ClientLevel level, Vec3 position, Vec3 velocity, Vec3 outward,
                      MoonlightEffects.Profile profile) {
        super(level, position.x, position.y, position.z);
        this.profile = profile;
        originY = position.y;
        if (profile == MoonlightEffects.GEYSER) {
            heightFadeEnd = Mth.lerp(random.nextDouble(), MoonlightEffects.GEYSER_FADE_END_MIN,
                    MoonlightEffects.GEYSER_FADE_END_MAX);
            heightFadeStart = Math.min(heightFadeEnd - 0.5,
                    Mth.lerp(random.nextDouble(), MoonlightEffects.GEYSER_FADE_START_MIN,
                            MoonlightEffects.GEYSER_FADE_START_MAX));
        } else if (profile == MoonlightEffects.AMBIENT) {
            heightFadeEnd = Mth.lerp(random.nextDouble(), MoonlightEffects.AMBIENT_FADE_END_MIN,
                    MoonlightEffects.AMBIENT_FADE_END_MAX);
            heightFadeStart = Math.max(1, heightFadeEnd - 1.5);
        } else {
            heightFadeStart = heightFadeEnd = 0;
        }
        type = TYPES[random.nextInt(TYPES.length)];
        size = profile.size() * (0.8F + random.nextFloat() * 0.4F);
        lifetime = profile.lifeMin() + random.nextInt(profile.lifeVariation() + 1);
        xd = velocity.x;
        yd = velocity.y;
        zd = velocity.z;
        outwardX = outward.x;
        outwardZ = outward.z;
        hasPhysics = false;
        gravity = 0;
        pulsing = random.nextFloat() < MoonlightEffects.PULSE_CHANCE;
        pulsePhase = random.nextFloat() * Mth.TWO_PI;
        pulseSpeed = 0.045F + random.nextFloat() * 0.035F;
        pulseAmplitude = 0.05F + random.nextFloat() * 0.07F;
        rotationSpeed = random.nextFloat() < MoonlightEffects.ROTATION_CHANCE
                ? (float) Math.toRadians(MoonlightEffects.ROTATION_DEGREES_PER_SECOND)
                    / 20 * (0.6F + random.nextFloat() * 0.4F) * (random.nextBoolean() ? 1 : -1)
                : 0;
        setSize(size * 2, size * 2);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        // The geyser widens gradually after rising; no per-tick random forces or gravity.
        double spread = profile.spreadAcceleration() * Math.sin(Math.PI * Math.min(age / 30.0, 1));
        xd = xd * profile.drag() + outwardX * spread;
        zd = zd * profile.drag() + outwardZ * spread;
        yd = yd * profile.drag() + profile.drift() * (1 - profile.drag());
        move(xd, yd, zd);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partial) {
        float time = age + partial;
        float fadeIn = smooth(time / MoonlightEffects.FADE_IN_TICKS);
        float fadeOut = smooth((lifetime - time) / profile.fadeOutTicks());
        if (heightFadeEnd > 0) {
            double height = Mth.lerp(partial, yo, y) - originY;
            fadeOut *= 1 - smooth((float) ((height - heightFadeStart) / (heightFadeEnd - heightFadeStart)));
        }
        int opacity = Math.round(255 * fadeIn * fadeOut);
        if (opacity == 0) return;
        float radius = size * (pulsing ? 1 + pulseAmplitude * Mth.sin(time * pulseSpeed + pulsePhase) : 1);
        Vec3 eye = camera.getPosition();
        float px = (float) (Mth.lerp(partial, xo, x) - eye.x);
        float py = (float) (Mth.lerp(partial, yo, y) - eye.y);
        float pz = (float) (Mth.lerp(partial, zo, z) - eye.z);
        int tint = compatiblePass ? 0x93EFFF : 0xFFFFFF;
        float angle = time * rotationSpeed;
        corner(buffer, camera, -radius, -radius, px, py, pz, 0, 1, opacity, tint, angle);
        // Match SingleQuadParticle winding: the opposite order is back-facing and culled in 1.20.1.
        corner(buffer, camera, -radius, radius, px, py, pz, 0, 0, opacity, tint, angle);
        corner(buffer, camera, radius, radius, px, py, pz, 1, 0, opacity, tint, angle);
        corner(buffer, camera, radius, -radius, px, py, pz, 1, 1, opacity, tint, angle);
    }

    private static void corner(VertexConsumer buffer, Camera camera, float x, float y,
                               float px, float py, float pz, float u, float v, int alpha, int tint, float angle) {
        Vector3f point = new Vector3f(x, y, 0).rotateZ(angle).rotate(camera.rotation());
        buffer.vertex(point.x + px, point.y + py, point.z + pz)
                .uv(u, v).color(alpha << 24 | tint).endVertex();
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    public ParticleRenderType getRenderType() { return type; }

    private record StarRenderType(ResourceLocation texture) implements ParticleRenderType {
        StarRenderType(String variant) {
            this(new ResourceLocation(RansomInMinecraft.MODID,
                    "textures/item/crucifix/moonlight_star_" + variant + ".png"));
        }

        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // The camera-facing quad has the same transparent, unlit mask as Circle/Shine.
            // Do not bind the item-entity framebuffer from a particle-engine pass.
            var shader = CrucifixIrisCompatibility.useCompatibleRenderer() ? null : CrucifixCircleRenderer.shader();
            compatiblePass = shader == null;
            if (shader == null) {
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            } else {
                RenderSystem.setShader(() -> shader);
                var minecraft = Minecraft.getInstance();
                float seconds = (minecraft.level.getGameTime()
                        + minecraft.getFrameTime()) / 20;
                shader.safeGetUniform("EffectTime").set(seconds);
                shader.safeGetUniform("LayerDepth").set(0.0F);
                shader.safeGetUniform("ViewOffset").set(0.0F, 0.0F);
            }
            RenderSystem.setShaderTexture(0, texture);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        @Override public void end(Tesselator tesselator) { tesselator.end(); }
    }
}
