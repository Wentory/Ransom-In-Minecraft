package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.Creeper;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import java.util.Random;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class WormEffects {
    private static int value;
    private static int tick;
    private static int nextImpulse;
    private static int movementEnd;
    private static int duplicateAt;
    private static int suppressDuplicates;
    private static KeyMapping forcedMovement;
    private static KeyMapping duplicate;
    private static final Random RANDOM = new Random();
    private static final Identifier GLITCH = Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "textures/glitch2.png");
    private WormEffects() {}
    public static void accept(int infection) { value = Math.clamp(infection, 0, 100); if (value < 75) releaseMovement(); }
    public static void clear() { value = 0; tick = 0; nextImpulse = 0; duplicate = null; releaseMovement(); }
    public static boolean illusion(LivingEntity mob) {
        if (mob instanceof Zombie && ClientInfectedZombies.contains(mob.getUUID())
                || mob instanceof Creeper && InfectedCreeperVisuals.phase(mob.getUUID()) >= 0) return false;
        return value >= 50 && mob instanceof Mob && Math.floorMod(hash(mob.getUUID().hashCode() ^ tick / 100), 7) == 0;
    }

    private static void releaseMovement() {
        if (forcedMovement != null) {
            Minecraft mc = Minecraft.getInstance();
            InputConstants.Key key = forcedMovement.getKey();
            boolean actual = key.getType() == InputConstants.Type.KEYSYM
                    && InputConstants.isKeyDown(mc.getWindow(), key.getValue());
            forcedMovement.setDown(actual);
            forcedMovement = null;
        }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        tick++;
        if (value < 75 || mc.player == null || !mc.player.isAlive() || mc.gui.screen() != null) {
            releaseMovement(); duplicate = null; nextImpulse = tick + 100; return;
        }
        if (forcedMovement != null) {
            if (tick >= movementEnd) releaseMovement();
            else forcedMovement.setDown(true);
        }
        if (duplicate != null && tick >= duplicateAt) {
            suppressDuplicates = tick + 5;
            KeyMapping.click(duplicate.getKey());
            duplicate = null;
        }
        if (tick < nextImpulse) return;
        nextImpulse = tick + 120 + RANDOM.nextInt(121);
        int choice = RANDOM.nextInt(6);
        if (choice < 4) {
            releaseMovement();
            KeyMapping[] movement = {mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight};
            forcedMovement = movement[choice];
            forcedMovement.setDown(true);
            movementEnd = tick + 20 + RANDOM.nextInt(21);
        } else {
            suppressDuplicates = tick + 5;
            KeyMapping.click((choice == 4 ? mc.options.keyAttack : mc.options.keyUse).getKey());
        }
    }

    @SubscribeEvent public static void doubleClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (value < 75 || mc.gui.screen() != null || mc.player == null || tick < suppressDuplicates
                || duplicate != null || RANDOM.nextInt(8) != 0) return;
        if (event.isAttack()) duplicate = mc.options.keyAttack;
        else if (event.isUseItem()) duplicate = mc.options.keyUse;
        if (duplicate != null) duplicateAt = tick + 2;
    }

    @SubscribeEvent public static void falseHud(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (value == 0 || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        boolean health = event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH);
        boolean food = event.getName().equals(VanillaGuiLayers.FOOD_LEVEL);
        if (!health && !food) return;
        event.setCanceled(true);
        GuiGraphicsExtractor g = event.getGuiGraphics();
        int width = g.guiWidth();
        int y = g.guiHeight() - (health ? mc.gui.hud.leftHeight : mc.gui.hud.rightHeight);
        // Replacing the vanilla layer must still reserve its row for armor and air.
        if (health) mc.gui.hud.leftHeight += 10;
        else mc.gui.hud.rightHeight += 10;
        int fake = 1 + Math.floorMod(hash((health ? 17 : 79) + tick / 17), 20);
        boolean distort = tick % 43 < 5;
        for (int i = 0; i < 10; i++) {
            int x = health ? width / 2 - 91 + i * 8 : width / 2 + 82 - i * 8;
            int shift = distort ? Math.floorMod(hash(i + tick), 5) - 2 : 0;
            String empty = health ? "hud/heart/container" : "hud/food_empty";
            String full = health ? "hud/heart/full" : "hud/food_full";
            String half = health ? "hud/heart/half" : "hud/food_half";
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(empty), x + shift, y, 9, 9);
            if (fake > i * 2) g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(fake == i * 2 + 1 ? half : full),
                    x + shift, y + (distort ? shift : 0), 9, 9);
            if (distort && i % 3 == 0) g.fill(x - 1, y + 3, x + 10, y + 5, 0xA0FF0808);
        }
    }

    @SubscribeEvent public static void overlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (value < 25 || mc.player == null || !mc.player.isAlive()) return;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        int width = g.guiWidth(), height = g.guiHeight();
        if (value >= 50) {

            GuiCompat.setColor(g, 1, 1, 1, 0.1F);
            try {
                GuiCompat.blit(g, GLITCH, 0, 0, width, height, 0, (tick / 2 % 6) * 128, 128, 128, 128, 768);
            } finally { GuiCompat.setColor(g, 1, 1, 1, 1); }
        }
        int count = value >= 75 ? 45 : 12;
        for (int i = 0; i < count; i++) {
            int h = hash(tick / 2 * 971 + i * 137);
            int x = Math.floorMod(h, Math.max(1, width));
            int y = Math.floorMod(h >>> 8, Math.max(1, height));
            int length = 2 + Math.floorMod(h >>> 16, value >= 75 ? 45 : 12);
            int color = (value >= 75 ? 0x38000000 : 0x18000000) | ((h & 3) == 0 ? 0x00FF1010 : 0x00FFFFFF);
            g.fill(x, y, x + length, y + 1, color);
        }
        if (value >= 75 && tick % 37 < 3) {
            int y = Math.floorMod(hash(tick / 37), Math.max(1, height - 10));
            g.fill(0, y, width, y + 3, 0x50FF1010);
            g.fill(0, y + 4, width, y + 9, 0x50000000);
        }
    }
    private static int hash(int x) { x ^= x >>> 16; x *= 0x7feb352d; x ^= x >>> 15; x *= 0x846ca68b; return x ^ x >>> 16; }
}
