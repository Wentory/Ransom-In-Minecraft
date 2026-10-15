package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.HoneyPotInventoryPayload;
import com.wentory.ransom_in_minecraft.network.RansomPackets;
import com.wentory.ransom_in_minecraft.network.RansomServerState;
import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import com.wentory.ransom_in_minecraft.network.ResistanceInputPayload;
import com.wentory.ransom_in_minecraft.network.ResistanceStatePayload;
import com.wentory.ransom_in_minecraft.rules.ResistanceRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class FullHijack {
    public static final boolean ENABLED = true;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final String PROGRESS = "ransom_resistance_progress";
    private static final String INVENTORY_UNLOCKED = "ransom_resistance_inventory";
    private FullHijack() {}

    public static boolean active(Player player) {
        return ENABLED && player instanceof ServerPlayer serverPlayer && WormInfection.get(serverPlayer) == 100
                && StealerConfig.WORM_ENABLED.get() && player.isAlive() && !player.isSpectator();
    }

    public static boolean inventoryAllowed(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return !active(player) || session != null && session.rules.inventoryUnlocked;
    }

    public static boolean crucifixLocked(Player player, ItemStack stack) {
        return active(player) && stack.is(RansomItems.CRUCIFIX.get());
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(com.wentory.ransom_in_minecraft.network.HijackedBodyInput.TYPE,
                com.wentory.ransom_in_minecraft.network.HijackedBodyInput.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::accept));
        registrar.playToClient(com.wentory.ransom_in_minecraft.network.FullHijackVisualPayload.TYPE,
                com.wentory.ransom_in_minecraft.network.FullHijackVisualPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.network.ClientFullHijackTracker.accept(payload)));
        registrar.playToClient(ResistanceStatePayload.TYPE, ResistanceStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::accept));
        registrar.playToServer(ResistanceInputPayload.TYPE, ResistanceInputPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) input(player, payload);
                }));
    }

    private static void input(ServerPlayer player, ResistanceInputPayload input) {
        if (!active(player) || ReplayCompatibility.isReplayServer(player.level().getServer())) return;
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session(player));
        if (input.sequence() <= session.sequence || !Float.isFinite(input.dx()) || !Float.isFinite(input.dy())) return;
        session.sequence = input.sequence();
        session.dx = Math.clamp(session.dx + input.dx(), -0.35F, 0.35F);
        session.dy = Math.clamp(session.dy + input.dy(), -0.35F, 0.35F);
        session.click |= input.click();
        session.playing = input.playing();
        session.inventoryOpen = input.inventoryOpen() && session.rules.inventoryUnlocked;
        session.inputAt = player.level().getGameTime();
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(PROGRESS);
        player.getPersistentData().remove(INVENTORY_UNLOCKED);
        Session removed = SESSIONS.remove(player.getUUID());
        if (removed != null) {
            removed.body.clear(player);
            RansomPackets.sendToPlayer(player, new ResistanceStatePayload(false, 0, 0, .5F, .5F, .5F, .5F, 0, false, 0, 0, 0));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ReplayCompatibility.isReplayServer(player.level().getServer())) return;
        if (!active(player)) { clear(player); return; }
        Session session = SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session(player));
        if (!session.initialized) {
            session.initialized = true;
            CaveRansom.cancelForFullHijack(player);
            if (RansomServerState.isEncounterActive(player)) RansomServerState.finishCrucifixRescue(player);
        }
        session.body.tick(player);
        if (!inventoryAllowed(player) && player.containerMenu != player.inventoryMenu) player.closeContainer();
        if (session.playing && player.level().getGameTime() - session.inputAt <= 5) {
            session.rules.tick(session.dx, session.dy, session.click, session.inventoryOpen);
        }
        session.dx = session.dy = 0;
        session.click = false;
        if (session.rules.progress >= 1) {
            WormInfection.set(player, ResistanceRules.RECOVERED_HIJACK);
            return;
        }
        ResistanceRules r = session.rules;
        player.getPersistentData().putFloat(PROGRESS, r.progress);
        player.getPersistentData().putBoolean(INVENTORY_UNLOCKED, r.inventoryUnlocked);
        RansomPackets.sendToPlayer(player, new ResistanceStatePayload(true, session.sequence, r.ticks,
                r.cursorX, r.cursorY, r.targetX, r.targetY, r.progress, r.inventoryUnlocked, r.glitchTicks, r.feedback, r.soundEvents));
    }

    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if (blocksControl(event.getEntity())) event.setCanceled(true);
    }
    private static boolean blocksControl(Player player) {
        Session session = SESSIONS.get(player.getUUID());
        return active(player) && (session == null || !session.body.controlling());
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (blocksControl(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.LeftClickBlock event) {
        if (blocksControl(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (blocksControl(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickItem event) {
        if (!blocksControl(event.getEntity())) return;
        boolean honey = event.getEntity() instanceof ServerPlayer player && inventoryAllowed(player)
                && event.getItemStack().is(RansomItems.HONEY_POT.get());
        if (!honey) event.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (blocksControl(event.getPlayer())) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Session session = SESSIONS.remove(event.getEntity().getUUID());
        if (session != null && event.getEntity() instanceof ServerPlayer player) session.body.clear(player);
    }
    @SubscribeEvent public static void tracking(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            RansomPackets.sendToPlayer(viewer, new com.wentory.ransom_in_minecraft.network.FullHijackVisualPayload(
                    target.getUUID(), active(target)));
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }

    private static final class Session {
        final FullHijackBody body = new FullHijackBody();
        final ResistanceRules rules;
        int sequence;
        float dx, dy;
        boolean click, playing, initialized, inventoryOpen;
        long inputAt;
        Session(ServerPlayer player) {
            rules = new ResistanceRules(player.getUUID().getMostSignificantBits() ^ player.level().getGameTime());
            rules.progress = Math.clamp(player.getPersistentData().getFloatOr(PROGRESS, 0F), 0F, 1F);
            rules.inventoryUnlocked = player.getPersistentData().getBooleanOr(INVENTORY_UNLOCKED, false);
        }
    }
}
