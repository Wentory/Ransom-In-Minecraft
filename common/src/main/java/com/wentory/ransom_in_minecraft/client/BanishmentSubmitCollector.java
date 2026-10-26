package com.wentory.ransom_in_minecraft.client;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;

/** Decorates model submissions instead of modifying another renderer's invocation. */
public final class BanishmentSubmitCollector implements SubmitNodeCollector {
    private final OrderedSubmitNodeCollector delegate;
    private final float progress, flash, opacity;
    public BanishmentSubmitCollector(OrderedSubmitNodeCollector delegate, float progress) {
        this.delegate = delegate;
        this.progress = progress;
        flash = Math.clamp((.2F-progress)/.1F, 0, 1);
        float p = Math.clamp((progress-.2F)/.8F, 0, 1);
        opacity = 1-p*p*(3-2*p);
    }
    private int tint(int original, int target) { return Math.round(original+(target-original)*flash); }
    private int color(int color) {
        return Math.round((color>>>24)*opacity)<<24
            | tint((color>>>16)&255,85)<<16 | tint((color>>>8)&255,221)<<8 | tint(color&255,255);
    }
    @Override public OrderedSubmitNodeCollector order(int order) {
        return new BanishmentSubmitCollector(delegate instanceof SubmitNodeCollector source ? source.order(order) : delegate, progress);
    }
    @Override public void submitShadow(com.mojang.blaze3d.vertex.PoseStack a0, float a1, java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece> a2) { delegate.submitShadow(a0, a1, a2); }
    @Override public void submitNameTag(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.world.phys.Vec3 a1, int a2, net.minecraft.network.chat.Component a3, boolean a4, int a5, net.minecraft.client.renderer.state.level.CameraRenderState a6) { delegate.submitNameTag(a0, a1, a2, a3, a4, a5, a6); }
    @Override public void submitText(com.mojang.blaze3d.vertex.PoseStack a0, float a1, float a2, net.minecraft.util.FormattedCharSequence a3, boolean a4, net.minecraft.client.gui.Font.DisplayMode a5, int a6, int a7, int a8, int a9) { delegate.submitText(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9); }
    @Override public void submitFlame(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.client.renderer.entity.state.EntityRenderState a1, org.joml.Quaternionf a2) { delegate.submitFlame(a0, a1, a2); }
    @Override public void submitLeash(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState a1) { delegate.submitLeash(a0, a1); }
    @Override public <S> void submitModel(net.minecraft.client.model.Model<? super S> a0, S a1, com.mojang.blaze3d.vertex.PoseStack a2, net.minecraft.client.renderer.rendertype.RenderType a3, int a4, int a5, int a6, net.minecraft.client.renderer.texture.TextureAtlasSprite a7, int a8, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay a9) { delegate.submitModel(a0, a1, a2, a3, flash > 0 ? 0xF000F0 : a4, a5, color(a6), a7, a8, a9); }
    @Override public void submitMovingBlock(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.client.renderer.block.MovingBlockRenderState a1, int a2) { delegate.submitMovingBlock(a0, a1, a2); }
    @Override public void submitBlockModel(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.client.renderer.rendertype.RenderType a1, java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> a2, int[] a3, int a4, int a5, int a6) { delegate.submitBlockModel(a0, a1, a2, a3, a4, a5, a6); }
    @Override public void submitBreakingBlockModel(com.mojang.blaze3d.vertex.PoseStack a0, java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> a1, int a2) { delegate.submitBreakingBlockModel(a0, a1, a2); }
    @Override public void submitShapeOutline(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.world.phys.shapes.VoxelShape a1, net.minecraft.client.renderer.rendertype.RenderType a2, int a3, float a4, boolean a5) { delegate.submitShapeOutline(a0, a1, a2, a3, a4, a5); }
    @Override public void submitItem(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.world.item.ItemDisplayContext a1, int a2, int a3, int a4, int[] a5, java.util.List<net.minecraft.client.resources.model.geometry.BakedQuad> a6, net.minecraft.client.renderer.item.ItemStackRenderState.FoilType a7) { delegate.submitItem(a0, a1, a2, a3, a4, a5, a6, a7); }
    @Override public void submitCustomGeometry(com.mojang.blaze3d.vertex.PoseStack a0, net.minecraft.client.renderer.rendertype.RenderType a1, net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer a2) { delegate.submitCustomGeometry(a0, a1, a2); }
    @Override public void submitQuadParticleGroup(net.minecraft.client.renderer.state.level.QuadParticleRenderState a0) { delegate.submitQuadParticleGroup(a0); }
    @Override public void submitGizmoPrimitives(net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Group a0, net.minecraft.client.renderer.state.level.CameraRenderState a1, boolean a2) { delegate.submitGizmoPrimitives(a0, a1, a2); }
}
