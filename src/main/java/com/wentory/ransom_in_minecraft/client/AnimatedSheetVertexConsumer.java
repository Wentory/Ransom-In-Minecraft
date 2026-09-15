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

    @Override public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha * this.alpha / 255);
        return this;
    }

    @Override public VertexConsumer uv(float u, float v) {
        delegate.uv(u, (frame + v) / frameCount);
        return this;
    }

    @Override public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override public void endVertex() { delegate.endVertex(); }

    @Override public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(red, green, blue, alpha * this.alpha / 255);
    }

    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
}
