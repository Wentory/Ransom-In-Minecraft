package com.wentory.ransom_in_minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Bounded virtual viewpoint for the client-only resistance space. */
final class ResistanceParallax {
    private static final float RESPONSE_SECONDS = .18F;
    private static final float MOUSE_GAIN = 1.8F;
    private static final float MAX_LAYER_SHIFT = .016F;
    private static final float FOREGROUND_EXTRA_DEPTH = 1F;
    private static float targetX, targetY, x, y;
    private static long previousFrame;

    private ResistanceParallax() {}

    static void mouse(float dx, float dy) {
        targetX = Math.clamp(targetX + dx * MOUSE_GAIN, -1F, 1F);
        targetY = Math.clamp(targetY + dy * MOUSE_GAIN, -1F, 1F);
    }

    static void update() {
        long now = System.nanoTime();
        float seconds = previousFrame == 0 ? 0 : Math.min(.1F, (now - previousFrame) / 1_000_000_000F);
        previousFrame = now;
        boolean interactive = FullHijackClient.active() && Minecraft.getInstance().gui.screen() instanceof ResistanceScreen;
        float desiredX = interactive ? targetX : 0, desiredY = interactive ? targetY : 0;
        float blend = 1 - (float) Math.exp(-seconds / RESPONSE_SECONDS);
        x += (desiredX - x) * blend;
        y += (desiredY - y) * blend;
    }

    static float x() { return x; }
    static float y() { return y; }

    static void beginLayer(GuiGraphicsExtractor graphics, float depth) {
        graphics.pose().pushMatrix();
        float size = Math.min(graphics.guiWidth(), graphics.guiHeight());
        graphics.pose().translate(-x * size * MAX_LAYER_SHIFT * depth,
                -y * size * MAX_LAYER_SHIFT * depth);
    }

    static void endLayer(GuiGraphicsExtractor graphics) {
        
        graphics.pose().popMatrix();
    }

    static void beginForeground(GuiGraphicsExtractor graphics) { beginLayer(graphics, FOREGROUND_EXTRA_DEPTH); }

    static void clear() {
        targetX = targetY = x = y = 0;
        previousFrame = 0;
    }
}
