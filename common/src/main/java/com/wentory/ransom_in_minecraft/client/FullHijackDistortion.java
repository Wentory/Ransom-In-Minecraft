package com.wentory.ransom_in_minecraft.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.WeakHashMap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import com.wentory.ransom_in_minecraft.mixin.HijackPostChainAccessor;
import com.wentory.ransom_in_minecraft.mixin.HijackPostPassAccessor;

/** Extract the game separately, then warp world/game before ordinary GUI is drawn. */
public final class FullHijackDistortion {
    private static final Identifier EFFECT = Identifier.fromNamespaceAndPath("ransom_in_minecraft", "full_hijack");
    private static final GuiRenderState GAME = new GuiRenderState();
    private static final WeakHashMap<PostChain,GpuBuffer> PARAMS = new WeakHashMap<>();
    private static boolean gameRendered;
    private static GuiRenderer gameRenderer;
    private FullHijackDistortion() {}
    static boolean gameRendered() { return gameRendered; }
    public static void clear() {
        GAME.reset();
        gameRendered = false;
        if (gameRenderer != null) { gameRenderer.close(); gameRenderer = null; }
    }
    public static void extract() {
        GAME.reset();
        gameRendered = false;
        if (!FullHijackClient.active() && !FullHijackClient.exiting()) return;
        var mc = Minecraft.getInstance();
        var screen = mc.gui.screen();
        boolean inventory = FullHijackClient.isInventory(screen);
        if (!(screen instanceof ResistanceScreen) && !inventory && !(screen instanceof net.minecraft.client.gui.screens.ChatScreen)) return;
        ResistanceParallax.update();
        var graphics = new GuiGraphicsExtractor(mc, GAME, 0, 0);
        if (inventory) ResistanceScreen.renderBackdrop(graphics);
        ResistanceScreen.renderGame(graphics, inventory);
        gameRendered = true;
    }
    public static void renderBeforeGui(GuiRenderer renderer, RenderTarget target, GraphicsResourceAllocator pool) {
        if (!FullHijackClient.active() && !FullHijackClient.exiting()) return;
        if (gameRendered) {
            if (gameRenderer == null) gameRenderer = new GuiRenderer(GAME,
                    Minecraft.getInstance().gameRenderer.featureRenderDispatcher(), java.util.List.of());
            gameRenderer.render();
            gameRenderer.endFrame();
        }
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        PostChain chain = mc.getShaderManager().getPostChain(EFFECT, LevelTargetBundle.MAIN_TARGETS);
        if (chain == null) return;
        float amount = FullHijackClient.exiting()
                ? 1 - net.minecraft.util.Mth.clamp(FullHijackClient.outroSeconds() / 1.8F, 0, 1)
                : FullHijackClient.introProgress();
        amount = amount * amount * (3 - 2 * amount);
        ByteBuffer data = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        data.putFloat(amount * .18F).putFloat(ResistanceParallax.x() * amount)
                .putFloat(ResistanceParallax.y() * amount).putFloat(0).flip();
        GpuBuffer buffer = PARAMS.get(chain);
        if (buffer == null) {
            var pass = ((HijackPostChainAccessor) chain).ransom$passes().getFirst();
            var uniforms = ((HijackPostPassAccessor) pass).ransom$uniforms();
            buffer = RenderSystem.getDevice().createBuffer(() -> "Ransom Hijack parameters",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
            var old = uniforms.put("HijackParams", buffer);
            if (old != null) old.close();
            PARAMS.put(chain, buffer);
        } else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
        chain.process(target, pool);
    }
}
