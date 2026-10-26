package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import java.util.List;
import java.util.Map;

/** Exercises Minecraft's real model-emission path without a window or OpenGL context. */
public final class BanishmentVertexConsumerTest {
    public static void main(String[] args) {
        var model = new ModelPart(List.of(new ModelPart.Cube(0, 0, 0, 0, 0,
                1, 1, 1, 0, 0, 0, false, 16, 16,
                java.util.EnumSet.allOf(net.minecraft.core.Direction.class))), Map.of());
        var recording = new Recording();
        model.render(new PoseStack(), new BanishmentVertexConsumer(recording, 0), 0, 0, 0xFFFFFFFF);
        check(recording.vertices == 24, "Model geometry was lost");
        check(recording.red == 85 && recording.green == 221 && recording.blue == 255,
                "Initial Moonlight flash is not cyan");
        check(recording.alpha == 255 && recording.lightU == 240 && recording.lightV == 240,
                "Initial flash must remain opaque and visible in darkness");

        var middle = new BanishmentVertexConsumer(recording, .6F);
        middle.setColor(0x80643219).setLight(0x00700030).setUv(.25F, .75F).setNormal(0, 1, 0);
        check(Math.abs(recording.alpha - 64) <= 1, "Fade must multiply a mod's existing alpha");
        check(recording.red == 100 && recording.green == 50 && recording.blue == 25,
                "Fade must preserve a model's own tint after the flash");
        check(recording.lightU == 48 && recording.lightV == 112, "Normal light must be preserved after the flash");
        check(recording.u == .25F && recording.v == .75F && recording.normalY == 1,
                "Texture coordinates or normals were changed");

        model.render(new PoseStack(), new BanishmentVertexConsumer(recording, 1), 0, 0, 0xFFFFFFFF);
        check(recording.alpha == 0, "Banishment must finish fully transparent");
        new BanishmentVertexConsumer(recording, .2F).setColor(10, 20, 30, 170);
        check(recording.alpha == 170, "Body must not fade before the flash finishes");
        System.out.println("Banishment: real model vertices, packed color/light, cyan flash and smooth fade passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Recording implements VertexConsumer {
        int vertices, red, green, blue, alpha, lightU, lightV;
        float u, v, normalY;
        public VertexConsumer addVertex(float x, float y, float z) { vertices++; return this; }
        public VertexConsumer setColor(int r, int g, int b, int a) { red=r; green=g; blue=b; alpha=a; return this; }
        public VertexConsumer setUv(float u, float v) { this.u=u; this.v=v; return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { lightU=u; lightV=v; return this; }
        public VertexConsumer setNormal(float x, float y, float z) { normalY=y; return this; }
    }
}
