package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.WeakHashMap;

final class GuiCompat {
    private static final WeakHashMap<GuiGraphicsExtractor, Integer> COLORS = new WeakHashMap<>();
    static void setColor(GuiGraphicsExtractor g, float r, float b, float c, float a) {
        COLORS.put(g, (Math.round(a*255)<<24)|(Math.round(r*255)<<16)|(Math.round(b*255)<<8)|Math.round(c*255));
    }
    static void blit(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width, int height,
                     float u, float v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, sourceWidth, sourceHeight,
                textureWidth, textureHeight, COLORS.getOrDefault(g, -1));
    }
}
