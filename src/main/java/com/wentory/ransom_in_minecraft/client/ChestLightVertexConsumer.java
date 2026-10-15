package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Adapts vanilla chest model vertices to the unlit POSITION_TEX_COLOR light layer. */
final class ChestLightVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    ChestLightVertexConsumer(VertexConsumer delegate) { this.delegate = delegate; }

    // ModelPart emits the entity-format bulk vertex. Our unlit textured layer requires UV before color.
    @Override public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                                 float u, float v, int overlay, int light, float nx, float ny, float nz) {
        delegate.vertex(x, y, z).uv(u, v).color(red, green, blue, alpha).endVertex();
    }

    @Override public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
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

    @Override public void endVertex() { delegate.endVertex(); }
    @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, a); }
    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    @Override public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }
}
