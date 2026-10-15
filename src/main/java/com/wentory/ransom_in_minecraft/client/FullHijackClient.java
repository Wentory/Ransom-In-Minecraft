package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.RansomItems;
import com.wentory.ransom_in_minecraft.network.HoneyPotInventoryPayload;
import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import com.wentory.ransom_in_minecraft.network.ResistanceInputPayload;
import com.wentory.ransom_in_minecraft.network.ResistanceStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import com.wentory.ransom_in_minecraft.network.PacketDistributor;
import java.util.ArrayDeque;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class FullHijackClient {
    private static final java.util.Set<net.minecraft.resources.ResourceLocation> GAMEPLAY_HUD = java.util.Set.of(
            VanillaGuiOverlay.HOTBAR.id(), VanillaGuiOverlay.CROSSHAIR.id(), VanillaGuiOverlay.ITEM_NAME.id(),
            VanillaGuiOverlay.PLAYER_HEALTH.id(), VanillaGuiOverlay.ARMOR_LEVEL.id(), VanillaGuiOverlay.FOOD_LEVEL.id(),
            VanillaGuiOverlay.AIR_LEVEL.id(), VanillaGuiOverlay.EXPERIENCE_BAR.id(), 
            VanillaGuiOverlay.JUMP_BAR.id(), VanillaGuiOverlay.MOUNT_HEALTH.id(), VanillaGuiOverlay.POTION_ICONS.id(),
            VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), VanillaGuiOverlay.SCOREBOARD.id());
    private static ResistanceStatePayload state;
    private static final ArrayDeque<Pending> PENDING = new ArrayDeque<>();
    private static float dx, dy;
    private static boolean click;
    private static int sequence;
    private static double lastMouseX = Double.NaN, lastMouseY;
    private static long noticeUntil;
    private static String notice = "";
    private static long feedbackUntil;
    private static int feedback;
    private static long ringReactedAt;
    private static int reactedCycle = -1;
    private static float reactedScale, reactedX, reactedY;
    static boolean ringConsumed() { return state.ticks() / com.wentory.ransom_in_minecraft.rules.ResistanceRules.PULSE_PERIOD == reactedCycle; }
    static float ringReactionAge() { return (System.nanoTime() - ringReactedAt) / 1_000_000_000F; }
    static float reactedScale() { return reactedScale; }
    static float reactedX() { return reactedX; }
    static float reactedY() { return reactedY; }
    static int ringColor() { return feedback > 0 ? 0x80FF98 : 0xFF5555; }
    private static long introStarted;
    private static long outroStarted;
    static boolean exiting() { return outroStarted != 0; }
    static float outroSeconds() { return (System.nanoTime() - outroStarted) / 1_000_000_000F; }
    private static final float INTRO_SECONDS = (float) com.wentory.ransom_in_minecraft.rules.HijackAudioTimeline.LOOP_START_SECONDS;

    static float introProgress() {
        return net.minecraft.util.Mth.clamp((System.nanoTime() - introStarted) / 1_000_000_000F / INTRO_SECONDS, 0F, 1F);
    }
    static float elapsedSeconds() { return (System.nanoTime() - introStarted) / 1_000_000_000F; }
    static float cursorRevealAge() { return Math.max(0F, elapsedSeconds() - (INTRO_SECONDS - 1F)); }
    static void musicStarted() {
        if (active()) introStarted = System.nanoTime();
    }

    private FullHijackClient() {}
    public static boolean active() { return state != null && state.active() && !exiting(); }
    public static boolean inventoryUnlocked() { return active() && state.inventoryUnlocked(); }
    static ResistanceStatePayload state() { return state; }
    static boolean isInventory(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
    static int feedback() { return System.nanoTime() < feedbackUntil ? feedback : 0; }
    static String notice() { return System.nanoTime() < noticeUntil ? notice : ""; }

    public static void clear() {
        clear(false);
    }
    private static void clear(boolean preserveEnding) {
        FullHijackDistortion.clear();
        ResistanceParallax.clear();
        bodyInput = null;
        bodyX = bodyZ = 0;
        if (!preserveEnding) FullHijackMusic.stop();
        state = null;
        introStarted = 0;
        outroStarted = 0;
        ringReactedAt = 0;
        reactedCycle = -1;
        PENDING.clear();
        dx = dy = 0;
        click = false;
        sequence = 0;
        resetMouse();
        noticeUntil = feedbackUntil = 0;
    }

    private static void accept(ResistanceStatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (ReplayCompatibility.isReplayServer(mc.getSingleplayerServer())) return;
        boolean wasActive = active(), wasUnlocked = inventoryUnlocked();
        if (!payload.active()) {
            if (exiting()) return;
            if (wasActive && mc.player != null && mc.player.isAlive()) {
                outroStarted = System.nanoTime();
                FullHijackMusic.finish();
                PENDING.clear();
                dx = dy = 0;
                click = false;
                if (!(mc.screen instanceof ChatScreen)) mc.setScreen(new ResistanceScreen());
                return;
            }
            clear();
            if (mc.screen instanceof ResistanceScreen) mc.setScreen(null);
            return;
        }
        boolean newTick = state != null && payload.ticks() > state.ticks();
        if (newTick && introProgress() >= 1F) {
            if ((payload.soundEvents() & 1) != 0) playSound("hijack_target_teleport");
            if ((payload.soundEvents() & 2) != 0) playSound("hijack_cursor_teleport");
            int period = com.wentory.ransom_in_minecraft.rules.ResistanceRules.PULSE_PERIOD;
            int start = com.wentory.ransom_in_minecraft.rules.ResistanceRules.PULSE_START;
            if (payload.ticks() % period == start) playSound("hijack_ring");
            if (payload.feedback() != 0) playSound(payload.feedback() > 0 ? "hijack_ring_success" : "hijack_ring_fail");
        }
        state = payload;
        outroStarted = 0;
        if (!wasActive) {
            introStarted = System.nanoTime();
            FullHijackMusic.start();
        }
        while (!PENDING.isEmpty() && PENDING.peekFirst().sequence <= payload.acknowledged()) PENDING.removeFirst();
        if (payload.feedback() != 0) {
            feedback = payload.feedback();
            feedbackUntil = System.nanoTime() + 500_000_000L;
            var period = com.wentory.ransom_in_minecraft.rules.ResistanceRules.PULSE_PERIOD;
            int pulse = payload.ticks() % period;
            if (pulse >= com.wentory.ransom_in_minecraft.rules.ResistanceRules.PULSE_START
                    && pulse < com.wentory.ransom_in_minecraft.rules.ResistanceRules.PULSE_END
                    && reactedCycle != payload.ticks() / period) {
                reactedCycle = payload.ticks() / period;
                ringReactedAt = System.nanoTime();
                reactedScale = com.wentory.ransom_in_minecraft.rules.ResistanceRules.timingRingScale(pulse);
                reactedX = payload.targetX();
                reactedY = payload.targetY();
            }
        }
        if (!wasUnlocked && payload.inventoryUnlocked()) notify("ransom.hijack.inventory_available");
        if (!wasActive && mc.player != null && !(mc.screen instanceof PauseScreen)
                && !(mc.screen instanceof DeathScreen) && !(mc.screen instanceof ChatScreen)) {
            mc.setScreen(new ResistanceScreen());
        }
    }

    static float cursorX() {
        float value = state.cursorX() + dx;
        for (Pending p : PENDING) value += p.dx;
        return net.minecraft.util.Mth.clamp(value, .02F, .98F);
    }
    static float cursorY() {
        float value = state.cursorY() + dy;
        for (Pending p : PENDING) value += p.dy;
        return net.minecraft.util.Mth.clamp(value, .02F, .98F);
    }
    static void mouse(double x, double y, double arenaSize) {
        if (exiting()) return;
        if (introProgress() < 1F) { lastMouseX = x; lastMouseY = y; return; }
        if (!Double.isNaN(lastMouseX)) {
            ResistanceParallax.mouse((float) ((x - lastMouseX) / arenaSize),
                    (float) ((y - lastMouseY) / arenaSize));
            dx = net.minecraft.util.Mth.clamp(dx + (float) ((x - lastMouseX) / arenaSize), -.35F, .35F);
            dy = net.minecraft.util.Mth.clamp(dy + (float) ((y - lastMouseY) / arenaSize), -.35F, .35F);
        }
        lastMouseX = x; lastMouseY = y;
    }
    static void resetMouse() { lastMouseX = Double.NaN; }
    static void recenterMouse() {
        Minecraft mc = Minecraft.getInstance();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() / 2D,
                mc.getWindow().getScreenHeight() / 2D);
        lastMouseX = mc.getWindow().getGuiScaledWidth() / 2D;
        lastMouseY = mc.getWindow().getGuiScaledHeight() / 2D;
    }
    static void click() { if (!exiting() && introProgress() >= 1F) click = true; }
    static void notify(String key) { notice = key; noticeUntil = System.nanoTime() + 2_000_000_000L; }

    private static void playSound(String name) {
        var sound = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                new net.minecraft.resources.ResourceLocation(RansomInMinecraft.MODID, name));
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1F));
    }

    @SubscribeEvent public static void tick(ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (exiting()) {
            if (outroSeconds() >= 1.8F || mc.player == null || !mc.player.isAlive()) {
                clear(mc.player != null && mc.player.isAlive() && outroSeconds() >= 1.8F);
                if (mc.screen instanceof ResistanceScreen) mc.setScreen(null);
            }
            return;
        }
        if (!active() || mc.player == null || mc.level == null) return;
        if (!mc.player.isAlive()) return;
        if (mc.screen == null) mc.setScreen(new ResistanceScreen());
        boolean playing = introProgress() >= 1F && (mc.screen instanceof ResistanceScreen
                || isInventory(mc.screen) || mc.screen instanceof ChatScreen);
        if (!mc.isPaused()) {
            int id = ++sequence;
            boolean inventory = isInventory(mc.screen);
            PacketDistributor.sendToServer(new ResistanceInputPayload(id, playing && !inventory ? dx : 0,
                    playing && !inventory ? dy : 0, playing && !inventory && click, playing, inventory));
            if (playing && !inventory) PENDING.addLast(new Pending(id, dx, dy));
            while (PENDING.size() > 40) PENDING.removeFirst();
            dx = dy = 0; click = false;
        }
        if (mc.screen instanceof ResistanceScreen) recenterMouse();
    }

    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        if (!active()) return;
        var input = event.getInput();
        input.leftImpulse = input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
        if (bodyInput == null || Minecraft.getInstance().isPaused()) return;
        var player = event.getEntity();
        player.setYRot(net.minecraft.util.Mth.rotLerp(.18F, player.getYRot(), bodyInput.yaw()));
        player.setXRot(net.minecraft.util.Mth.lerp(.15F, player.getXRot(), bodyInput.pitch()));
        bodyX = net.minecraft.util.Mth.lerp(.25F, bodyX, bodyInput.x());
        bodyZ = net.minecraft.util.Mth.lerp(.25F, bodyZ, bodyInput.z());
        float yaw = player.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
        input.forwardImpulse = (bodyZ * net.minecraft.util.Mth.cos(yaw) - bodyX * net.minecraft.util.Mth.sin(yaw)) * .65F;
        input.leftImpulse = (bodyX * net.minecraft.util.Mth.cos(yaw) + bodyZ * net.minecraft.util.Mth.sin(yaw)) * .65F;
        // Ordinary liquid jump gives buoyancy even while pursuing or using an item.
        // Stop sprint swimming so aiming down at a victim cannot drive the body underwater.
        player.setSprinting(false);
        input.jumping = bodyInput.jump() || player.isInWater();
    }

    private static com.wentory.ransom_in_minecraft.network.HijackedBodyInput bodyInput;
    private static float bodyX, bodyZ;

    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        if (!active()) return;
        if (isInventory(event.getNewScreen()) || event.getNewScreen() instanceof ChatScreen) {
            dx = dy = 0;
            click = false;
            PENDING.clear();
        }
        if (isInventory(event.getNewScreen()) && !inventoryUnlocked()) {
            event.setCanceled(true);
            notify("ransom.hijack.cannot_inventory");
        }
        resetMouse();
    }

    @SubscribeEvent public static void inventoryRender(ScreenEvent.Render.Post event) {
        if (!isInventory(event.getScreen()) || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (var slot : screen.getMenu().slots) {
            if (slot.container != mc.player.getInventory()) continue;
            int x = screen.getGuiLeft() + slot.x, y = screen.getGuiTop() + slot.y;
            if (active() && slot.getItem().is(RansomItems.HONEY_POT.get())) {
                var g = event.getGuiGraphics();
                int pulse = 125 + (int) (Math.sin(System.nanoTime() / 300_000_000D) * 35);
                g.fill(x - 1, y - 1, x + 17, y, pulse << 24 | 0x93EFFF);
                g.fill(x - 1, y + 16, x + 17, y + 17, pulse << 24 | 0x93EFFF);
                g.fill(x - 1, y, x, y + 16, pulse << 24 | 0x93EFFF);
                g.fill(x + 16, y, x + 17, y + 16, pulse << 24 | 0x93EFFF);
            }
            if (active() && slot.getItem().is(RansomItems.CRUCIFIX.get())) {
                event.getGuiGraphics().fill(x, y, x + 16, y + 16, 0xA0000000);
                event.getGuiGraphics().blit(new net.minecraft.resources.ResourceLocation(
                        RansomInMinecraft.MODID, "textures/block.png"), x, y, 16, 16, 0, 0, 200, 200, 200, 200);
            }
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hideGameplayHud(RenderGuiOverlayEvent.Pre event) {
        if ((active() || exiting()) && GAMEPLAY_HUD.contains(event.getOverlay().id())) event.setCanceled(true);
    }

    @SubscribeEvent public static void inventoryClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isInventory(event.getScreen()) || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var slot = screen.getSlotUnderMouse();
        if (slot == null || slot.container != mc.player.getInventory()) return;
        if (active() && slot.getItem().is(RansomItems.CRUCIFIX.get())) { event.setCanceled(true); return; }
        if (event.getButton() == 1 && slot.getItem().is(RansomItems.HONEY_POT.get())) {
            if (active() && !inventoryUnlocked()) return;
            event.setCanceled(true);
            int inventorySlot = slot.getContainerSlot();
            if (screen instanceof CreativeModeInventoryScreen) {
                inventorySlot = -1;
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    if (mc.player.getInventory().getItem(i) == slot.getItem()) { inventorySlot = i; break; }
                }
            }
            if (inventorySlot >= 0) PacketDistributor.sendToServer(new HoneyPotInventoryPayload(inventorySlot));
        }
    }

    @SubscribeEvent public static void inventoryKey(ScreenEvent.KeyPressed.Pre event) {
        if (!active() || !isInventory(event.getScreen())) return;
        int key = event.getKeyCode(), scan = event.getScanCode();
        var options = Minecraft.getInstance().options;
        if (key == GLFW.GLFW_KEY_ESCAPE || options.keyInventory.matches(key, scan)) return;
        if (options.keyChat.matches(key, scan) || options.keyCommand.matches(key, scan)) {
            Minecraft.getInstance().setScreen(new ChatScreen(options.keyCommand.matches(key, scan) ? "/" : ""));
        } else if (options.keyTogglePerspective.matches(key, scan)) {
            options.setCameraType(options.getCameraType().cycle());
        } else if (options.keySmoothCamera.matches(key, scan)) {
            options.smoothCamera = !options.smoothCamera;
        } else {
            notify(options.keyAttack.matches(key, scan) ? "ransom.hijack.cannot_attack"
                    : options.keyUse.matches(key, scan) ? "ransom.hijack.cannot_interact" : "ransom.hijack.cannot_control");
        }
        event.setCanceled(true);
    }

    public static void bindReceivers() {
        ResistanceStatePayload.bind(FullHijackClient::accept);
        com.wentory.ransom_in_minecraft.network.HijackedBodyInput.bind(packet -> bodyInput = packet);
    }
    private record Pending(int sequence, float dx, float dy) {}
}
