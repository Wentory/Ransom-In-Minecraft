package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterShadersEvent;

/** Pixelates only the bar's frame, filling and inventory threshold. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class ResistanceProgressBar {
    private static final ResourceLocation FRAME = texture("progress_frame");
    private static final ResourceLocation FILL = texture("progress_fill");
    private static final float MAX_PIXEL_SIZE = 12;
    private static ShaderInstance shader;

    private ResistanceProgressBar() {}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(RansomInMinecraft.MODID, "resistance_bar"),
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> shader = loaded);
    }

    static void render(GuiGraphics graphics, int x, int y, int width, float progress, float visibility) {
        if (visibility <= 0 || shader == null) return;
        float pixelSize = 1 + (MAX_PIXEL_SIZE - 1) * (1 - visibility);
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, FRAME);
        RenderSystem.setShaderTexture(1, FILL);
        shader.safeGetUniform("Cells").set(width / pixelSize, 16 / pixelSize);
        shader.safeGetUniform("Progress").set(progress);
        shader.safeGetUniform("InventoryThreshold").set(com.wentory.ransom_in_minecraft.rules.ResistanceRules.INVENTORY_THRESHOLD);
        shader.safeGetUniform("Opacity").set(visibility);
        var matrix = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, x, y, 0).uv(0, 0).color(-1).endVertex();
        buffer.vertex(matrix, x, y + 16, 0).uv(0, 1).color(-1).endVertex();
        buffer.vertex(matrix, x + width, y + 16, 0).uv(1, 1).color(-1).endVertex();
        buffer.vertex(matrix, x + width, y, 0).uv(1, 0).color(-1).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(RansomInMinecraft.MODID, "textures/gui/hijack/" + name + ".png");
    }
}
