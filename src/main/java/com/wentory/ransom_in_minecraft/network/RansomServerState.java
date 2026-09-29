package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class RansomServerState {
    private static final String NATURAL_SPAWN_DISABLED = "ransom_natural_spawn_disabled";
    private static final String ACTIVE = "ransom_active";
    private static final String COINS = "ransom_coins";
    private static final String TARGET = "ransom_target";
    private static final String TICKS = "ransom_ticks";
    private static final String FAILURE_ARMED = "ransom_failure_armed";
    private static final String FAILURE_DELETE_HOTBAR = "ransom_failure_delete_hotbar";
    private static final Map<UUID, LockedInventory> LOCKED_PLAYERS = new HashMap<>();

    private RansomServerState() {}

    static void acceptState(ServerPlayer player, RansomStatePayload payload) {
        if (payload.penaltyDamage() > 0) damage(player, payload.penaltyDamage(), payload.coins());
        else if (payload.failure()) fail(player, payload.coins(), payload.deleteHotbar());
        else if (payload.active()) lockHands(player);
        else unlockHands(player);
    }

    static void armFailure(ServerPlayer player, RansomFailureArmedPayload payload) {
        player.getPersistentData().putBoolean(FAILURE_ARMED, true);
        player.getPersistentData().putBoolean(FAILURE_DELETE_HOTBAR, payload.deleteHotbar());
        player.getPersistentData().putInt(COINS, Math.max(0, payload.coins()));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ransom")
                .requires(source -> source.hasPermission(2))
                .executes(context -> summonRansom(context.getSource().getPlayerOrException()))
                .then(Commands.literal("disable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setNaturalSpawn(context.getSource(), EntityArgument.getPlayers(context, "targets"), false))))
                .then(Commands.literal("enable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setNaturalSpawn(context.getSource(), EntityArgument.getPlayers(context, "targets"), true))))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> {
                            int summoned = 0;
                            for (ServerPlayer target : EntityArgument.getPlayers(context, "targets")) {
                                summoned += summonRansom(target);
                            }
                            return summoned;
                        })));
    }

    private static int setNaturalSpawn(net.minecraft.commands.CommandSourceStack source, java.util.Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer target : targets) {
            if (enabled) target.getPersistentData().remove(NATURAL_SPAWN_DISABLED);
            else target.getPersistentData().putBoolean(NATURAL_SPAWN_DISABLED, true);
            syncNaturalSpawnState(target);
        }
        source.sendSuccess(() -> Component.literal("Natural Ransom spawning " + (enabled ? "enabled" : "disabled") + " for " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static void syncNaturalSpawnState(ServerPlayer player) {
        RansomNetwork.sendTo(player, new NaturalSpawnStatePayload(!player.getPersistentData().getBoolean(NATURAL_SPAWN_DISABLED)));
    }

    private static int summonRansom(ServerPlayer target) {
        if (target.getPersistentData().getBoolean(ACTIVE)) return 0;
        RansomNetwork.sendTo(target, new RansomSummonPayload(true));
        return 1;
    }

    @SubscribeEvent
    public static void keepMainHandEmpty(PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !LOCKED_PLAYERS.containsKey(player.getUUID())) return;
        player.getInventory().selected = Inventory.getSelectionSize();
    }

    @SubscribeEvent
    public static void blockItemUse(PlayerInteractEvent.RightClickItem event) {
        if (LOCKED_PLAYERS.containsKey(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void blockBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (LOCKED_PLAYERS.containsKey(event.getEntity().getUUID()) && !event.getItemStack().isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockEntityUse(PlayerInteractEvent.EntityInteract event) {
        if (LOCKED_PLAYERS.containsKey(event.getEntity().getUUID()) && !event.getItemStack().isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getPersistentData().getBoolean(FAILURE_ARMED)) {
            unlockHands(player, false);
            return;
        }
        if (player.getPersistentData().getBoolean(ACTIVE)) {
            player.getPersistentData().putInt(TARGET, player.getPersistentData().getInt(TARGET) + 30);
        }
        unlockHands(player, false);
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        syncNaturalSpawnState(joining);
        for (UUID infected : LOCKED_PLAYERS.keySet()) {
            RansomNetwork.sendTo(joining, new InfectionStatusPayload(infected, true));
        }
        if (joining.getPersistentData().getBoolean(FAILURE_ARMED)) {
            RansomNetwork.sendTo(joining, new RansomFailureScarePayload(
                    joining.getPersistentData().getInt(COINS),
                    joining.getPersistentData().getBoolean(FAILURE_DELETE_HOTBAR)));
        } else if (joining.getPersistentData().getBoolean(ACTIVE)) {
            RansomNetwork.sendTo(joining, new RansomResumePayload(
                    joining.getPersistentData().getInt(COINS),
                    joining.getPersistentData().getInt(TARGET),
                    joining.getPersistentData().getInt(TICKS)));
        }
    }

    @SubscribeEvent
    public static void playerDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) unlockHands(player, false);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (event.getOriginal().getPersistentData().getBoolean(NATURAL_SPAWN_DISABLED)) {
            event.getEntity().getPersistentData().putBoolean(NATURAL_SPAWN_DISABLED, true);
        }
        if (!event.getOriginal().getPersistentData().getBoolean(ACTIVE)) return;
        var source = event.getOriginal().getPersistentData();
        var target = event.getEntity().getPersistentData();
        target.putBoolean(ACTIVE, true);
        target.putInt(COINS, source.getInt(COINS));
        target.putInt(TARGET, source.getInt(TARGET));
        target.putInt(TICKS, source.getInt(TICKS));
        target.putBoolean(FAILURE_ARMED, source.getBoolean(FAILURE_ARMED));
        target.putBoolean(FAILURE_DELETE_HOTBAR, source.getBoolean(FAILURE_DELETE_HOTBAR));
    }

    private static void fail(ServerPlayer player, int coins, boolean deleteHotbar) {
        if (deleteHotbar) deleteHotbar(player);
        else unlockHands(player);
        damage(player, 10, coins);
    }

    private static void damage(ServerPlayer player, int amount, int coins) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                new ResourceLocation(RansomInMinecraft.MODID, "ransom_debt"));
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(key).orElse(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC));
        String messageKey = coins <= 0 ? "death.attack.ransom_debt_no_coins"
                : java.util.concurrent.ThreadLocalRandom.current().nextBoolean()
                        ? "death.attack.ransom_debt_inattentive" : "death.attack.ransom_debt_late";
        DamageSource source = new DamageSource(type, player.position()) {
            @Override public Component getLocalizedDeathMessage(LivingEntity entity) {
                return Component.translatable(messageKey, entity.getDisplayName());
            }
        };
        player.hurt(source, amount);
    }

    private static void lockHands(ServerPlayer player) {
        player.getPersistentData().putBoolean(ACTIVE, true);
        LockedInventory previous = LOCKED_PLAYERS.putIfAbsent(player.getUUID(), new LockedInventory(
                Inventory.isHotbarSlot(player.getInventory().selected) ? player.getInventory().selected : 0));
        if (previous == null) {
            RansomNetwork.sendToAll(new InfectionStatusPayload(player.getUUID(), true));
        }
        player.getInventory().selected = Inventory.getSelectionSize();
    }

    private static void unlockHands(ServerPlayer player) {
        unlockHands(player, true);
    }

    private static void unlockHands(ServerPlayer player, boolean clearDebt) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        if (locked != null) {
            player.getInventory().selected = locked.selectedSlot();
            RansomNetwork.sendToAll(new InfectionStatusPayload(player.getUUID(), false));
        }
        if (clearDebt) clearProgress(player);
    }

    private static void deleteHotbar(ServerPlayer player) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.getInventory().selected = locked != null ? locked.selectedSlot() : 0;
        if (locked != null) {
            RansomNetwork.sendToAll(new InfectionStatusPayload(player.getUUID(), false));
        }
        clearProgress(player);
    }

    static void saveProgress(ServerPlayer player, RansomProgressPayload payload) {
        player.getPersistentData().putBoolean(ACTIVE, true);
        player.getPersistentData().putInt(COINS, Math.max(0, payload.coins()));
        player.getPersistentData().putInt(TARGET, Math.max(1, payload.targetCoins()));
        player.getPersistentData().putInt(TICKS, Math.max(0, payload.phaseTicks()));
    }

    private static void clearProgress(ServerPlayer player) {
        player.getPersistentData().remove(ACTIVE);
        player.getPersistentData().remove(COINS);
        player.getPersistentData().remove(TARGET);
        player.getPersistentData().remove(TICKS);
        player.getPersistentData().remove(FAILURE_ARMED);
        player.getPersistentData().remove(FAILURE_DELETE_HOTBAR);
    }

    public static boolean isHotbarLocked(Player player) {
        return LOCKED_PLAYERS.containsKey(player.getUUID())
                || player.level().isClientSide() && ClientInfectionTracker.isInfected(player.getUUID());
    }

    private record LockedInventory(int selectedSlot) {}
}
