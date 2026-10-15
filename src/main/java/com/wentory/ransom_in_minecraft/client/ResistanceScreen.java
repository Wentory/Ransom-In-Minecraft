package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.rules.ResistanceRules;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** Transparent, non-pausing screen: only the concentration marker responds to mouse movement. */
final class ResistanceScreen extends Screen {
    private static final int MOONLIGHT = 0xFF93EFFF;
    private static final float VIGNETTE_BREATH_PERIOD_SECONDS = 4.5F;
    private static final float VIGNETTE_BREATH_AMOUNT = .035F;
    private static final ResourceLocation CURSOR = texture("cursor");
    private static final ResourceLocation TARGET = texture("target");
    private static final ResourceLocation TIMING_RING = texture("timing_ring");
    private static final ResourceLocation VIGNETTE = texture("vignette");
    private static final ResourceLocation GLITCH = new ResourceLocation(
            RansomInMinecraft.MODID, "textures/glitch.png");
    ResistanceScreen() { super(Component.translatable("ransom.hijack.resist")); }
    @Override protected void init() {
        FullHijackClient.resetMouse();
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        FullHijackClient.recenterMouse();
    }
    @Override public void removed() {
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        FullHijackClient.resetMouse();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        minecraft.setScreen(null);
        minecraft.pauseGame(false);
    }
    @Override public void mouseMoved(double x, double y) { FullHijackClient.mouse(x, y, arenaSize(width, height)); }
    @Override public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        FullHijackClient.mouse(x, y, arenaSize(width, height));
        return true;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) FullHijackClient.click();
        else FullHijackClient.notify("ransom.hijack.cannot_interact");
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (FullHijackClient.exiting()) return true;
        var options = minecraft.options;
        if (options.keyChat.matches(key, scan)) {
            minecraft.setScreen(new ChatScreen(""));
        } else if (options.keyCommand.matches(key, scan)) {
            minecraft.setScreen(new ChatScreen("/"));
        } else if (options.keyTogglePerspective.matches(key, scan)) {
            options.setCameraType(options.getCameraType().cycle());
            minecraft.gameRenderer.checkEntityPostEffect(minecraft.getCameraEntity());
        } else if (options.keySmoothCamera.matches(key, scan)) {
            options.smoothCamera = !options.smoothCamera;
        } else if (options.keyInventory.matches(key, scan)) {
            if (FullHijackClient.inventoryUnlocked()) minecraft.setScreen(new InventoryScreen(minecraft.player));
            else FullHijackClient.notify("ransom.hijack.cannot_inventory");
        } else if (options.keyAttack.matches(key, scan)) {
            FullHijackClient.notify("ransom.hijack.cannot_attack");
        } else if (options.keyUse.matches(key, scan)) {
            FullHijackClient.notify("ransom.hijack.cannot_interact");
        } else if (options.keyUp.matches(key, scan) || options.keyDown.matches(key, scan)
                || options.keyLeft.matches(key, scan) || options.keyRight.matches(key, scan)
                || options.keyJump.matches(key, scan) || options.keyShift.matches(key, scan)
                || options.keySprint.matches(key, scan)) {
            FullHijackClient.notify("ransom.hijack.cannot_move");
        } else FullHijackClient.notify("ransom.hijack.cannot_control");
        return true;
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!FullHijackDistortion.gameRendered()) renderGame(graphics, false);
    }

    static float arenaSize(int width, int height) { return Math.min(width * .80F, height * .68F); }

    static void renderGame(GuiGraphics g, boolean inventory) {
        if (!inventory) renderBackdrop(g);
        ResistanceParallax.beginLayer(g, .75F);
        try { renderGameLayers(g, inventory); }
        finally { ResistanceParallax.endLayer(g); }
    }

    private static void renderGameLayers(GuiGraphics g, boolean inventory) {
        var state = FullHijackClient.state();
        if (state == null) return;
        var mc = Minecraft.getInstance();
        int width = g.guiWidth(), height = g.guiHeight();
        float size = arenaSize(width, height);
        float intro = FullHijackClient.introProgress();
        float reveal = smooth(net.minecraft.util.Mth.clamp((intro - .2F) / .35F, 0F, 1F));
        float departure = FullHijackClient.exiting()
                ? smooth(net.minecraft.util.Mth.clamp(FullHijackClient.outroSeconds() / .6F, 0F, 1F)) : 0F;
        float opacity = 1 - departure;
        float left = (width - size) / 2, top = (height - size) / 2 - 8;
        float tx = left + state.targetX() * size, ty = top + state.targetY() * size;
        float radius = ResistanceRules.TARGET_RADIUS * size;
        if (!inventory) {
            float travel = smooth(net.minecraft.util.Mth.clamp((intro - .45F) / .4F, 0F, 1F));
            float textY = ty - radius - 22 + (float) Math.sin(Math.min(1F, intro / .45F) * Math.PI) * 7;
            textY += (25 - textY) * travel;
            textY += (-mc.font.lineHeight - 4 - textY) * departure;
            boolean visible = intro >= .45F || ((int) (intro * 30) % 2 == 0);
            if (visible && reveal > 0) foregroundText(g,
                    Component.translatable("ransom.hijack.instructions"), width / 2, Math.round(textY),
                    Math.max(4, Math.round(reveal * 255)) << 24 | 0xB8C5C8);
        }
        int alpha = Math.round(reveal * opacity * (inventory ? 128 : 255)) << 24;
        float appearanceRadius = radius * (1 + 2 * (1 - reveal));
        if (reveal > 0 && opacity > 0) sprite(g, TARGET, Math.round(tx - appearanceRadius), Math.round(ty - appearanceRadius),
                Math.round(appearanceRadius * 2), Math.round(appearanceRadius * 2), 64, 64, alpha | 0x93EFFF);
        int pulse = state.ticks() % ResistanceRules.PULSE_PERIOD;
        if (pulse >= ResistanceRules.PULSE_START && pulse < ResistanceRules.PULSE_END && !FullHijackClient.ringConsumed()) {
            float ring = radius * ResistanceRules.timingRingScale(pulse);
            if (opacity > 0) sprite(g, TIMING_RING, Math.round(tx - ring), Math.round(ty - ring),
                    Math.round(ring * 2), Math.round(ring * 2), 64, 64,
                    Math.round(255 * opacity) << 24 | 0xD8D8D8);
        }
        if (FullHijackClient.ringConsumed() && FullHijackClient.ringReactionAge() < .55F && opacity > 0) {
            float age = FullHijackClient.ringReactionAge();
            float dissolve = smooth(net.minecraft.util.Mth.clamp((age - .12F) / .43F, 0F, 1F));
            float ring = radius * FullHijackClient.reactedScale() * (1 + dissolve * .65F);
            float rx = left + FullHijackClient.reactedX() * size;
            float ry = top + FullHijackClient.reactedY() * size;
            sprite(g, TIMING_RING, Math.round(rx - ring), Math.round(ry - ring),
                    Math.round(ring * 2), Math.round(ring * 2), 64, 64,
                    Math.round(255 * opacity * (1 - dissolve)) << 24 | FullHijackClient.ringColor());
        }
        if (state.glitchTicks() > 0 && !FullHijackClient.exiting()) {
            int jitter = (state.ticks() % 3 - 1) * 5;
            sprite(g, TARGET, Math.round(tx + jitter - radius), Math.round(ty - 2 - radius),
                    Math.round(radius * 2), Math.round(radius * 2), 64, 64, 0xA0FF3030);
            sprite(g, TARGET, Math.round(tx - jitter - radius), Math.round(ty + 2 - radius),
                    Math.round(radius * 2), Math.round(radius * 2), 64, 64, 0xA020BFFF);
        }
        float cx = left + FullHijackClient.cursorX() * size, cy = top + FullHijackClient.cursorY() * size;
        if (intro < 1F) {
            cx = left + size / 2;
            cy = top + size / 2;
        }
        int marker = FullHijackClient.feedback() < 0 ? 0xFFFF4545 : 0xFFE7FAFF;
        int markerSize = Math.max(1, Math.round(size * ResistanceRules.MARKER_RADIUS * 2));
        // The last second of the introduction unfolds the marker from the arena's center.
        float cursorAge = FullHijackClient.cursorRevealAge();
        int markerWidth = Math.round(markerSize * smooth(net.minecraft.util.Mth.clamp(cursorAge / .5F, 0F, 1F)));
        int markerHeight = Math.round(markerSize * smooth(net.minecraft.util.Mth.clamp(cursorAge, 0F, 1F)));
        if (markerWidth > 0 && markerHeight > 0 && opacity > 0) sprite(g, CURSOR,
                Math.round(cx - markerWidth / 2F), Math.round(cy - markerHeight / 2F),
                markerWidth, markerHeight, 16, 16, Math.round(255 * opacity) << 24 | (marker & 0xFFFFFF));
        int barWidth = Math.min(260, width - 40), barX = (width - barWidth) / 2, barY = height - 39;
        float barVisibility = smooth(net.minecraft.util.Mth.clamp((intro - .55F) / .4F, 0F, 1F)) * opacity;
        ResistanceParallax.beginForeground(g);
        try {
            ResistanceProgressBar.render(g, barX, barY - 4, barWidth, state.progress(), barVisibility);
        } finally { ResistanceParallax.endLayer(g); }
        if (state.inventoryUnlocked()) {
            if (barVisibility > 0) foregroundText(g, Component.translatable("ransom.hijack.inventory_available"),
                    width / 2, barY + 21, Math.max(4, Math.round(255 * barVisibility)) << 24 | (MOONLIGHT & 0xFFFFFF));
        }
        String notice = FullHijackClient.notice();
        if (!notice.isEmpty()) foregroundText(g, Component.translatable(notice), width / 2, 42 - Math.round(departure * 60),
                notice.endsWith("inventory_available") ? MOONLIGHT : 0xFFFF6565);
    }

    private static void foregroundText(GuiGraphics graphics, Component text, int x, int y, int color) {
        ResistanceParallax.beginForeground(graphics);
        try { graphics.drawCenteredString(Minecraft.getInstance().font, text, x, y, color); }
        finally { ResistanceParallax.endLayer(graphics); }
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(RansomInMinecraft.MODID, "textures/gui/hijack/" + name + ".png");
    }

    private static float smooth(float value) {
        return value * value * (3 - 2 * value);
    }

    static void renderBackdrop(GuiGraphics graphics) {
        ResistanceParallax.beginLayer(graphics, .2F);
        try { renderBackdropLayer(graphics); }
        finally { ResistanceParallax.endLayer(graphics); }
    }

    private static void renderBackdropLayer(GuiGraphics graphics) {
        var state = FullHijackClient.state();
        if (state == null) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        float buildup = smooth(net.minecraft.util.Mth.clamp(FullHijackClient.introProgress() / .55F, 0F, 1F));
        float noise = buildup;
        float vignetteAlpha = buildup;
        float expansion = 1 - buildup;
        if (FullHijackClient.exiting()) {
            float seconds = FullHijackClient.outroSeconds();
            noise = 1 - smooth(net.minecraft.util.Mth.clamp(seconds / .6F, 0F, 1F));
            float release = smooth(net.minecraft.util.Mth.clamp((seconds - .6F) / 1.2F, 0F, 1F));
            expansion = release * 3;
            vignetteAlpha = 1 - release;
        }
        graphics.fill(-64, -64, width + 64, height + 64, Math.round(112 * noise) << 24 | 0x080000);
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 0.25F, 0.25F, 0.65F * noise);
        try {
            int frame = state.ticks() / 2 % 6;
            // Repeat small copies instead of stretching one frame across the screen.
            int tileSize = 64;
            graphics.enableScissor(0, 0, width, height);
            try {
                for (int y = -tileSize; y < height + tileSize; y += tileSize) {
                    for (int x = -tileSize; x < width + tileSize; x += tileSize) {
                        graphics.blit(GLITCH, x, y, tileSize, tileSize,
                                0, frame * 128, 128, 128, 128, 768);
                    }
                }
            } finally {
                graphics.disableScissor();
            }
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        // The vignette covers the interference, while gameplay markers stay readable above it.
        int extraX = Math.round(width * .45F * expansion);
        int extraY = Math.round(height * .45F * expansion);
        float breath = 1 + VIGNETTE_BREATH_AMOUNT * buildup * vignetteAlpha
                * (1 - (float) Math.cos(FullHijackClient.elapsedSeconds() * Math.PI * 2 / VIGNETTE_BREATH_PERIOD_SECONDS));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(width / 2F, height / 2F, 0);
            graphics.pose().scale(breath, breath, 1);
            graphics.pose().translate(-width / 2F, -height / 2F, 0);
            sprite(graphics, VIGNETTE, -extraX, -extraY, width + 2 * extraX, height + 2 * extraY,
                    128, 128, Math.max(4, Math.round(255 * vignetteAlpha)) << 24 | 0xFFFFFF);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void sprite(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                               int width, int height, int sourceWidth, int sourceHeight, int color) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((color >>> 16 & 255) / 255F, (color >>> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        try {
            graphics.blit(texture, x, y, width, height, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
