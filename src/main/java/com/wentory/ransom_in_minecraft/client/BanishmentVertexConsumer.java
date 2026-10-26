package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Applies banishment after a model (including a modded model) emits its vertices. */
public final class BanishmentVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float opacity;
    private final float flash;

    public BanishmentVertexConsumer(VertexConsumer delegate, float progress) {
        this.delegate = delegate;
        float p = Math.clamp((progress - 0.2F) / 0.8F, 0, 1);
        opacity = 1 - p * p * (3 - 2 * p);
        flash = Math.clamp((0.2F - progress) / 0.1F, 0, 1);
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(tint(red, 85), tint(green, 221), tint(blue, 255), Math.round(alpha * opacity));
        return this;
    }

    private int tint(int original, int target) {
        return Math.round(original + (target - original) * flash);
    }

    @Override public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(flash > 0 ? 240 : u, flash > 0 ? 240 : v);
        return this;
    }

    @Override public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
