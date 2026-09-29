package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;

public final class ChestPreviewRenderer extends PictureInPictureRenderer<ChestPreviewRenderer.State> {
    public record State(ChestRenderState chest, float rotation, int x0, int y0, int x1, int y1,
                        float scale, Matrix3x2f pose, ScreenRectangle scissorArea) implements PictureInPictureRenderState {
        public ScreenRectangle bounds() {
            ScreenRectangle area = new ScreenRectangle(x0, y0, x1-x0, y1-y0).transformMaxBounds(pose);
            return scissorArea == null ? area : area.intersection(scissorArea);
        }
    }
    public static void register(net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) {
        event.register(State.class, ChestPreviewRenderer::new);
    }
    public Class<State> getRenderStateClass() { return State.class; }
    protected String getTextureLabel() { return "Ransom chest preview"; }
    protected float getTranslateY(int height, int guiScale) { return height / 2.0F; }
    protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
        // PiP uses screen-down Y and reversed Z; restore the chest's world axes.
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.mulPose(Axis.XP.rotationDegrees(15));
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
        pose.translate(-0.5F, -0.5F, -0.5F);
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockEntityRenderDispatcher().submit(state.chest, pose, collector,
                mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState);
    }
}
