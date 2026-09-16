package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Maps ordinary 0..1 UVs into one vertical frame of a texture sheet. */
final class AnimatedSheetVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int frame;
    private final int frameCount;
    private final int alpha;

    AnimatedSheetVertexConsumer(VertexConsumer delegate, int frame, int frameCount) {
        this(delegate, frame, frameCount, 255);
    }

    AnimatedSheetVertexConsumer(VertexConsumer delegate, int frame, int frameCount, int alpha) {
        this.delegate = delegate;
        this.frame = Math.floorMod(frame, frameCount);
        this.frameCount = frameCount;
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(red, green, blue, alpha * this.alpha / 255);
        return this;
    }

    @Override public VertexConsumer setColor(int color) {
        int adjustedAlpha = ((color >>> 24) * this.alpha / 255) & 0xFF;
        delegate.setColor((color & 0x00FFFFFF) | (adjustedAlpha << 24));
        return this;
    }

    @Override public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, (frame + v) / frameCount);
        return this;
    }

    @Override public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }
}
