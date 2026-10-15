package com.wentory.ransom_in_minecraft.client;

import java.util.Optional;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.GpuFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import com.wentory.ransom_in_minecraft.mixin.HijackGuiStateAccessor;
import com.wentory.ransom_in_minecraft.mixin.RansomPipelineRegistry;

/** Per-vertex control data retains the bar's own pixelation under deferred GUI rendering. */
final class ResistanceProgressBar {
    private static final Identifier FRAME = texture("progress_frame");
    private static final Identifier FILL = texture("progress_fill");
    private static final VertexFormat FORMAT = VertexFormat.builder(0).addAttribute("Position",GpuFormat.RGB32_FLOAT)
            .addAttribute("UV0",GpuFormat.RG32_FLOAT).addAttribute("Color",GpuFormat.RGBA8_UNORM)
            .addAttribute("UV1",GpuFormat.RG16_SINT).build();
    private static final RenderPipeline PIPELINE = RansomPipelineRegistry.ransom$register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("ransom_in_minecraft","pipeline/resistance_bar"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("ransom_in_minecraft","core/resistance_bar"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("ransom_in_minecraft","core/resistance_bar"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withVertexBinding(0,FORMAT).withDepthStencilState(Optional.empty()).build());
    private ResistanceProgressBar() {}
    static void preload(){}
    static void render(GuiGraphicsExtractor graphics, int x, int y, int width, float progress, float visibility) {
        if (visibility <= 0) return;
        var textures = Minecraft.getInstance().getTextureManager();
        var frame = textures.getTexture(FRAME);
        var fill = textures.getTexture(FILL);
        var setup = TextureSetup.doubleTexture(frame.getTextureView(), frame.getSampler(), fill.getTextureView(), fill.getSampler());
        var access = (HijackGuiStateAccessor) graphics;
        var bounds = new ScreenRectangle(x,y,width,16).transformMaxBounds(graphics.pose());
        // This bar is extracted only into the independent full-screen game layer.
        ScreenRectangle scissor = null;
        access.ransom$guiState().addGuiElement(new Bar(setup,new Matrix3x2f(graphics.pose()),x,y,width,
                Math.round(Math.clamp(progress,0,1)*32767), Math.round(Math.clamp(visibility,0,1)*255), scissor,bounds));
    }
    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("ransom_in_minecraft","textures/gui/hijack/"+name+".png");
    }
    private record Bar(TextureSetup textureSetup, Matrix3x2f pose, int x,int y,int width,
                       int progress,int alpha,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline() { return PIPELINE; }
        public void buildVertices(VertexConsumer vertices) {
            vertex(vertices,x,y,0,0); vertex(vertices,x,y+16,0,1);
            vertex(vertices,x+width,y+16,1,1); vertex(vertices,x+width,y,1,0);
        }
        private void vertex(VertexConsumer vertices,int px,int py,float u,float v) {
            vertices.addVertexWith2DPose(pose,px,py).setUv(u,v).setColor(255,255,255,alpha).setUv1(progress,width);
        }
    }
}
