package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/** Applies banishment to emitted vertices without intercepting model draw calls. */
public final class BanishmentVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float opacity, flash;

    public BanishmentVertexConsumer(VertexConsumer delegate, float progress) {
        this.delegate = delegate;
        float p = Mth.clamp((progress - .2F) / .8F, 0, 1);
        opacity = 1 - p * p * (3 - 2 * p);
        flash = Mth.clamp((.2F - progress) / .1F, 0, 1);
    }
    public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x,y,z); return this; }
    public VertexConsumer color(int r, int g, int b, int a) {
        delegate.color(tint(r,85), tint(g,221), tint(b,255), Math.round(a*opacity)); return this;
    }
    private int tint(int original, int target) { return Math.round(original+(target-original)*flash); }
    public VertexConsumer uv(float u, float v) { delegate.uv(u,v); return this; }
    public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u,v); return this; }
    public VertexConsumer uv2(int u, int v) { delegate.uv2(flash>0?240:u,flash>0?240:v); return this; }
    public VertexConsumer normal(float x, float y, float z) { delegate.normal(x,y,z); return this; }
    public void endVertex() { delegate.endVertex(); }
    public void defaultColor(int r, int g, int b, int a) {
        delegate.defaultColor(tint(r,85),tint(g,221),tint(b,255),Math.round(a*opacity));
    }
    public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
}
