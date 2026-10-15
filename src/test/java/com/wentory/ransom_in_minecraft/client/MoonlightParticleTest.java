package com.wentory.ransom_in_minecraft.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;

/** Compare actual particle geometry with vanilla without opening a window. */
public final class MoonlightParticleTest {
    public static void main(String[] args) {
        var camera = new Camera();
        var vanilla = new SingleQuadParticle(null, 0, 0, 0) {
            protected float getU0() { return 0; }
            protected float getU1() { return 1; }
            protected float getV0() { return 0; }
            protected float getV1() { return 1; }
            public int getLightColor(float partial) { return 0xF000F0; }
            public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
        };
        float reference = area(vanilla, camera, DefaultVertexFormat.PARTICLE);
        for (var profile : new MoonlightEffects.Profile[]{MoonlightEffects.AMBIENT, MoonlightEffects.GEYSER,
                MoonlightEffects.CHEST, MoonlightEffects.MOB, MoonlightEffects.CRUCIFIX}) {
            var particle = new MoonlightParticle(null, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, profile);
            for (int tick = 0; tick < 5; tick++) particle.tick();
            float actual = area(particle, camera, DefaultVertexFormat.POSITION_TEX_COLOR);
            if (actual * reference <= 0) throw new AssertionError("Moonlight faces away from vanilla particles");
        }
        System.out.println("Moonlight particles: all five emission profiles emit four vertices facing the same way as vanilla");
    }
    private static float area(Particle particle, Camera camera, VertexFormat format) {
        var builder = new BufferBuilder(4096);
        builder.begin(VertexFormat.Mode.QUADS, format);
        particle.render(builder, camera, .5F);
        var rendered = builder.end();
        if (rendered.drawState().vertexCount() != 4) throw new AssertionError("Missing star quad");
        var bytes = rendered.vertexBuffer(); int stride = format.getVertexSize();
        float x0 = bytes.getFloat(0), y0 = bytes.getFloat(4);
        float x1 = bytes.getFloat(stride), y1 = bytes.getFloat(stride + 4);
        float x2 = bytes.getFloat(stride * 2), y2 = bytes.getFloat(stride * 2 + 4);
        float normal = (x1-x0)*(y2-y0)-(y1-y0)*(x2-x0);
        rendered.release();
        return normal;
    }
}
