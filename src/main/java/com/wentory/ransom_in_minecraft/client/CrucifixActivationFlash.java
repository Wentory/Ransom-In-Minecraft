package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Local anticipation effect, independent of the encounter's subsequent reset. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class CrucifixActivationFlash {
    private static final long FADE_NANOS = 350_000_000L;
    private static long started;

    private CrucifixActivationFlash() {}

    static void start(Minecraft minecraft) {
        started = System.nanoTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderGuiEvent.Post event) {
        if (started == 0) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            started = 0;
            return;
        }
        float progress = (System.nanoTime() - started) / (float) FADE_NANOS;
        if (progress >= 1) {
            started = 0;
            return;
        }
        int alpha = Math.round(230 * (1 - progress) * (1 - progress));
        event.getGuiGraphics().fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), (alpha << 24) | 0xB9F3FF);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        started = 0;
    }
}
