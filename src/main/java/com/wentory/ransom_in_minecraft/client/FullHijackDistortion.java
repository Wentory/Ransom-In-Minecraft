package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/** Distorts the world and resistance game before ordinary HUD and screens are drawn. */
public final class FullHijackDistortion {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "shaders/post/full_hijack.json");
    private static PostChain chain;
    private static int width, height;
    private static boolean failed;
    private static boolean gameRendered;

    private FullHijackDistortion() {}

    public static void beginFrame() { gameRendered = false; }

    static boolean gameRendered() { return gameRendered; }

    public static void beforeHud(GuiGraphics graphics, DeltaTracker delta) {
        ResistanceParallax.update();
        Minecraft mc = Minecraft.getInstance();
        boolean inventory = FullHijackClient.isInventory(mc.screen);
        if ((FullHijackClient.active() || FullHijackClient.exiting())
                && (mc.screen instanceof ResistanceScreen || inventory
                    || mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            if (inventory) ResistanceScreen.renderBackdrop(graphics);
            ResistanceScreen.renderGame(graphics, inventory);
            gameRendered = true;
        }
        graphics.flush();
        render(delta.getGameTimeDeltaTicks());
    }

    public static void clear() {
        if (chain != null) { chain.close(); chain = null; }
        failed = false;
    }

    public static void render(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if ((!FullHijackClient.active() && !FullHijackClient.exiting()) || mc.level == null) {
            clear();
            return;
        }
        if (failed) return;
        try {
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), EFFECT);
                width = height = 0;
            }
            int newWidth = mc.getWindow().getWidth(), newHeight = mc.getWindow().getHeight();
            if (width != newWidth || height != newHeight) {
                width = newWidth; height = newHeight;
                chain.resize(width, height);
            }
            float amount = FullHijackClient.exiting()
                    ? 1 - Math.clamp(FullHijackClient.outroSeconds() / 1.8F, 0, 1)
                    : FullHijackClient.introProgress();
            amount = amount * amount * (3 - 2 * amount);
            chain.setUniform("Strength", amount * .18F);
            chain.setUniform("ViewX", ResistanceParallax.x() * amount);
            chain.setUniform("ViewY", ResistanceParallax.y() * amount);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(partialTick);
        } catch (java.io.IOException | RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger(FullHijackDistortion.class).error("Cannot render full Hijack distortion", exception);
            if (chain != null) { chain.close(); chain = null; }
            failed = true;
        } finally {
            mc.getMainRenderTarget().bindWrite(false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

}
