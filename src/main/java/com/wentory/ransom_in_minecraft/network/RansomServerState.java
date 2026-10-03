package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.RansomwareConfig;
import com.wentory.ransom_in_minecraft.StealerConfig;
import com.wentory.ransom_in_minecraft.InfectedZombies;
import com.wentory.ransom_in_minecraft.InfectedCreepers;
import com.wentory.ransom_in_minecraft.WormInfection;
import com.wentory.ransom_in_minecraft.client.WormEffects;
import com.wentory.ransom_in_minecraft.client.InfectedCreeperVisuals;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class RansomServerState {
    private static final String ACTIVE = "ransom_active";
    private static final String COINS = "ransom_coins";
    private static final String TARGET = "ransom_target";
    private static final String TICKS = "ransom_ticks";
    private static final String FAILURE_ARMED = "ransom_failure_armed";
    private static final String FAILURE_DELETE_HOTBAR = "ransom_failure_delete_hotbar";
    private static final String ENCRYPTED_SLOTS = "ransom_encrypted_slots";
    private static final String NATURAL_SPAWN_DISABLED = "ransom_natural_spawn_disabled";
    private static final int ALL_MAIN_INVENTORY_SLOTS = (1 << 27) - 1;
    private static final Map<UUID, LockedInventory> LOCKED_PLAYERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_GLITCH_HIT = new HashMap<>();
    private static final Map<UUID, Integer> NATURAL_SPAWN_TIMERS = new HashMap<>();

    private RansomServerState() {}

    public static boolean isEncounterActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(CommonSettingsPayload.TYPE, CommonSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::accept));
        registrar.playToClient(RansomwareSyncPayload.TYPE, RansomwareSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::accept));
        registrar.playToServer(RansomwareSettingsPayload.TYPE, RansomwareSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || !player.hasPermissions(2)
                            && !player.getServer().isSingleplayerOwner(player.getGameProfile())) return;
                    RansomwareConfig.JUMPSCARE_ENDS_AT.set(Math.clamp(payload.jumpscareEnd(), 0.05, 30.0));
                    RansomwareConfig.DOWNLOAD_STARTS_AT.set(Math.clamp(payload.downloadStart(), 0.0, 30.0));
                    RansomwareConfig.DOWNLOAD_ENDS_AT.set(Math.clamp(payload.downloadEnd(), 0.05, 30.0));
                    RansomwareConfig.DELETE_HOTBAR_ON_FAILURE.set(payload.deleteItems());
                    RansomwareConfig.INFECTION_VERTICAL_BELOW.set(Math.clamp(payload.below(), 0, 128));
                    RansomwareConfig.INFECTION_VERTICAL_ABOVE.set(Math.clamp(payload.above(), 0, 128));
                    RansomwareConfig.INFECTION_RADIUS.set(Math.clamp(payload.radius(), 8, 256));
                    int minimum = Math.clamp(payload.spawnMin(), 1, 86400);
                    RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS.set(minimum);
                    RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS.set(Math.clamp(payload.spawnMax(), minimum, 86400));
                    RansomwareConfig.BIOME_WHITELIST_ENABLED.set(payload.biomeWhitelistEnabled());
                    RansomwareConfig.BIOME_WHITELIST.set(payload.biomeWhitelist());
                    RansomwareConfig.SPEC.save();
                    NATURAL_SPAWN_TIMERS.clear();
                    RansomPackets.sendToAllPlayers(RansomwareSyncPayload.current());
                }));
        registrar.playToClient(WormInfectionPayload.TYPE, WormInfectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> WormEffects.accept(payload.value())));
        registrar.playToServer(WormSettingsPayload.TYPE, WormSettingsPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !player.hasPermissions(2) && !player.getServer().isSingleplayerOwner(player.getGameProfile())) return;
            StealerConfig.WORM_ENABLED.set(payload.enabled());
            StealerConfig.WORM_HEAL_TICKS.set(Math.clamp(payload.healingTicks(), 1, 72000));
            StealerConfig.HIJACK_ZOMBIE_CHANCE.set(Math.clamp(payload.zombieChance(), 0, 100));
            StealerConfig.HIJACK_DROWNED_CHANCE.set(Math.clamp(payload.drownedChance(), 0, 100));
            StealerConfig.HIJACK_CREEPER_CHANCE.set(Math.clamp(payload.creeperChance(), 0, 100));
            StealerConfig.SPEC.save();
            RansomPackets.sendToAllPlayers(CommonSettingsPayload.current());
        });
        RansomChestGame.registerPayloads(registrar);
        registrar.playToServer(StealerSettingsPayload.TYPE, StealerSettingsPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || !player.createCommandSourceStack().hasPermission(2)
                            && !player.getServer().isSingleplayerOwner(player.getGameProfile())) return;
                    StealerConfig.ENABLED.set(payload.enabled());
                    StealerConfig.INFECTION_CHANCE_PERCENT.set(
                            Math.max(0, Math.min(100, payload.chancePercent())));
                    StealerConfig.SPEC.save();
                    RansomPackets.sendToAllPlayers(CommonSettingsPayload.current());
                });
        registrar.playToServer(RansomStatePayload.TYPE, RansomStatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    if (payload.penaltyDamage() > 0) damage(player, payload.penaltyDamage(), payload.coins());
                    else if (payload.failure()) fail(player, payload.coins(),
                            player.getPersistentData().getBoolean(FAILURE_ARMED)
                                    ? player.getPersistentData().getBoolean(FAILURE_DELETE_HOTBAR)
                                    : RansomwareConfig.DELETE_HOTBAR_ON_FAILURE.get());
                    else if (payload.active()) lockHands(player);
                    else unlockHands(player);
                });
        registrar.playToClient(InfectionStatusPayload.TYPE, InfectionStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientInfectionTracker.accept(payload)));
        registrar.playToClient(InfectedZombiePayload.TYPE, InfectedZombiePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientInfectedZombies.accept(payload)));
        registrar.playToClient(WormVictimPayload.TYPE, WormVictimPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientInfectedZombies.accept(payload)));
        registrar.playToClient(ZombieStopPayload.TYPE, ZombieStopPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientInfectedZombies.accept(payload)));
        registrar.playToClient(WormCreeperPayload.TYPE, WormCreeperPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> InfectedCreeperVisuals.accept(payload)));
        registrar.playToClient(WormCloudPayload.TYPE, WormCloudPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> InfectedCreeperVisuals.accept(payload)));
        registrar.playToServer(RansomProgressPayload.TYPE, RansomProgressPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) saveProgress(player, payload);
                });
        registrar.playToClient(RansomResumePayload.TYPE, RansomResumePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientRansomResumeTracker.accept(payload)));
        registrar.playToServer(RansomFailureArmedPayload.TYPE, RansomFailureArmedPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    player.getPersistentData().putBoolean(FAILURE_ARMED, true);
                    player.getPersistentData().putBoolean(FAILURE_DELETE_HOTBAR,
                            RansomwareConfig.DELETE_HOTBAR_ON_FAILURE.get());
                    player.getPersistentData().putInt(COINS, Math.max(0, payload.coins()));
                });
        registrar.playToClient(RansomFailureScarePayload.TYPE, RansomFailureScarePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientRansomResumeTracker.acceptFailureScare(payload)));
        registrar.playToClient(RansomSummonPayload.TYPE, RansomSummonPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientRansomResumeTracker.acceptSummon(payload)));
        registrar.playToClient(RansomCancelPayload.TYPE, RansomCancelPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (payload.cancel()) com.wentory.ransom_in_minecraft.client.RansomEncounter.cancelByCommand();
                }));
        registrar.playToClient(RansomForcedAttackPayload.TYPE, RansomForcedAttackPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientRansomResumeTracker.acceptForcedAttack(payload)));
        registrar.playToServer(GlitchContactPayload.TYPE, GlitchContactPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) handleGlitchContact(player);
                });
        registrar.playToClient(EncryptedSlotsPayload.TYPE, EncryptedSlotsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientEncryptedSlots.accept(payload)));
        registrar.playToClient(PlayerGlitchPayload.TYPE, PlayerGlitchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPlayerGlitchTracker.accept(payload)));
        registrar.playToServer(PlayerVisualEffectRequestPayload.TYPE, PlayerVisualEffectRequestPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    int effect = Math.max(PlayerVisualEffectPayload.CLEAR,
                            Math.min(PlayerVisualEffectPayload.SUCCESS, payload.effect()));
                    RansomPackets.sendToPlayersTrackingEntityAndSelf(player,
                            new PlayerVisualEffectPayload(player.getUUID(), effect));
                });
        registrar.playToClient(PlayerVisualEffectPayload.TYPE, PlayerVisualEffectPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPlayerVisualEffectTracker.accept(payload)));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ransom")
                .requires(source -> source.hasPermission(2))
                .executes(context -> summonRansom(context.getSource().getPlayerOrException()))
                .then(WormInfection.command())
                .then(Commands.literal("drowned")
                        .executes(context -> InfectedZombies.spawnTestDrowned(
                                context.getSource().getPlayerOrException())))
                .then(Commands.literal("zombie")
                        .executes(context -> InfectedZombies.spawnTestZombie(
                                context.getSource().getPlayerOrException())))
                .then(Commands.literal("creeper")
                        .executes(context -> InfectedCreepers.spawnTestCreeper(
                                context.getSource().getPlayerOrException())))
                .then(Commands.literal("chest")
                        .executes(context -> RansomChestGame.spawnDungeonChest(
                                context.getSource().getPlayerOrException()))
                        .then(Commands.literal("infect")
                                .executes(context -> RansomChestGame.infectLookedAtChest(
                                        context.getSource().getPlayerOrException())))
                        .then(Commands.argument("loot_table", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().getServer().reloadableRegistries()
                                                .getKeys(Registries.LOOT_TABLE).stream()
                                                .filter(id -> id.getPath().startsWith("chests/")), builder))
                                .executes(context -> RansomChestGame.spawnChest(
                                        context.getSource().getPlayerOrException(),
                                        ResourceLocationArgument.getId(context, "loot_table")))))
                .then(Commands.literal("disable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setNaturalSpawn(context.getSource(),
                                        EntityArgument.getPlayers(context, "targets"), false))))
                .then(Commands.literal("enable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setNaturalSpawn(context.getSource(),
                                        EntityArgument.getPlayers(context, "targets"), true))))
                .then(Commands.literal("status")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> showNaturalSpawnStatus(context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("cancel")
                        .executes(context -> cancelRansom(context.getSource(),
                                java.util.List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> cancelRansom(context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> {
                            int summoned = 0;
                            for (ServerPlayer target : EntityArgument.getPlayers(context, "targets")) {
                                summoned += summonRansom(target);
                            }
                            return summoned;
                        })));
    }

    private static int setNaturalSpawn(net.minecraft.commands.CommandSourceStack source,
                                       java.util.Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer target : targets) {
            if (enabled) target.getPersistentData().remove(NATURAL_SPAWN_DISABLED);
            else target.getPersistentData().putBoolean(NATURAL_SPAWN_DISABLED, true);
            NATURAL_SPAWN_TIMERS.remove(target.getUUID());
        }
        source.sendSuccess(() -> Component.literal("Natural Ransom spawning "
                + (enabled ? "enabled" : "disabled") + " for " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int showNaturalSpawnStatus(net.minecraft.commands.CommandSourceStack source,
                                              java.util.Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            boolean enabled = !target.getPersistentData().getBoolean(NATURAL_SPAWN_DISABLED);
            source.sendSuccess(() -> Component.literal(target.getGameProfile().getName()
                    + ": natural Ransom spawning is " + (enabled ? "enabled" : "disabled") + "."), false);
        }
        return targets.size();
    }

    private static int cancelRansom(net.minecraft.commands.CommandSourceStack source,
                                    java.util.Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            unlockHands(target);
            RansomPackets.sendToPlayer(target, new RansomCancelPayload(true));
        }
        source.sendSuccess(() -> Component.literal("Cancelled Ransomware for "
                + targets.size() + " player(s)."), true);
        return targets.size();
    }

    static int summonRansom(ServerPlayer target) {
        if (target.getPersistentData().getBoolean(ACTIVE)) return 0;
        RansomPackets.sendToPlayer(target, new RansomSummonPayload(true));
        return 1;
    }

    @SubscribeEvent
    public static void keepMainHandEmpty(PlayerTickEvent.Post event) {
        if (ReplayCompatibility.isReplayServer(event.getEntity().getServer())) return;
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !LOCKED_PLAYERS.containsKey(player.getUUID())) return;
        player.getInventory().selected = Inventory.getSelectionSize();
    }

    @SubscribeEvent
    public static void naturalSpawnTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (!player.isAlive() || player.getPersistentData().getBoolean(NATURAL_SPAWN_DISABLED)
                || isEncounterActive(player) || RansomChestGame.hasActiveGame(player)) {
            NATURAL_SPAWN_TIMERS.remove(id);
            return;
        }
        int remaining = NATURAL_SPAWN_TIMERS.computeIfAbsent(id, ignored -> randomNaturalSpawnDelayTicks()) - 1;
        if (remaining > 0) {
            NATURAL_SPAWN_TIMERS.put(id, remaining);
            return;
        }
        if (!naturalSpawnAllowedInCurrentBiome(player)) {
            NATURAL_SPAWN_TIMERS.put(id, 20);
            return;
        }
        NATURAL_SPAWN_TIMERS.put(id, randomNaturalSpawnDelayTicks());
        summonRansom(player);
    }

    private static int randomNaturalSpawnDelayTicks() {
        int minimum = RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS.get();
        int maximum = Math.max(minimum, RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS.get());
        int seconds = java.util.concurrent.ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
        return seconds * 20;
    }

    private static boolean naturalSpawnAllowedInCurrentBiome(ServerPlayer player) {
        if (!RansomwareConfig.BIOME_WHITELIST_ENABLED.get()) return true;
        var biomeKey = player.serverLevel().getBiome(player.blockPosition()).unwrapKey();
        if (biomeKey.isEmpty()) return false;
        String currentBiome = biomeKey.get().location().toString();
        for (String configuredBiome : RansomwareConfig.BIOME_WHITELIST.get().split(",")) {
            if (currentBiome.equals(configuredBiome.trim())) return true;
        }
        return false;
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
        NATURAL_SPAWN_TIMERS.remove(player.getUUID());
        LAST_GLITCH_HIT.remove(player.getUUID());
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
        if (ReplayCompatibility.isReplayServer(event.getEntity().getServer())) return;
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        RansomPackets.sendToPlayer(joining, RansomwareSyncPayload.current());
        syncEncryptedSlots(joining, false);
        for (UUID infected : LOCKED_PLAYERS.keySet()) {
            RansomPackets.sendToPlayer(joining, new InfectionStatusPayload(infected, true));
        }
        if (joining.getPersistentData().getBoolean(FAILURE_ARMED)) {
            RansomPackets.sendToPlayer(joining, new RansomFailureScarePayload(
                    joining.getPersistentData().getInt(COINS),
                    joining.getPersistentData().getBoolean(FAILURE_DELETE_HOTBAR)));
        } else if (joining.getPersistentData().getBoolean(ACTIVE)) {
            RansomPackets.sendToPlayer(joining, new RansomResumePayload(
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
        var source = event.getOriginal().getPersistentData();
        var target = event.getEntity().getPersistentData();
        if (source.getBoolean(NATURAL_SPAWN_DISABLED)) {
            target.putBoolean(NATURAL_SPAWN_DISABLED, true);
        }
        if (!source.getBoolean(ACTIVE)) return;
        target.putBoolean(ACTIVE, true);
        target.putInt(COINS, source.getInt(COINS));
        target.putInt(TARGET, source.getInt(TARGET));
        target.putInt(TICKS, source.getInt(TICKS));
        target.putBoolean(FAILURE_ARMED, source.getBoolean(FAILURE_ARMED));
        target.putBoolean(FAILURE_DELETE_HOTBAR, source.getBoolean(FAILURE_DELETE_HOTBAR));
        target.putInt(ENCRYPTED_SLOTS, source.getInt(ENCRYPTED_SLOTS));
        syncEncryptedSlots((ServerPlayer) event.getEntity(), false);
    }

    private static void fail(ServerPlayer player, int coins, boolean deleteHotbar) {
        if (deleteHotbar) deleteEncryptedItems(player);
        else unlockHands(player);
        damage(player, 10, coins);
    }

    private static void handleGlitchContact(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(ACTIVE) || player.isDeadOrDying()) return;
        long now = player.serverLevel().getGameTime();
        long previous = LAST_GLITCH_HIT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < 40L || !damageGlitch(player)) return;
        LAST_GLITCH_HIT.put(player.getUUID(), now);
        WormInfection.add(player, 5);
        RansomPackets.sendToPlayersTrackingEntityAndSelf(player, new PlayerGlitchPayload(player.getUUID()));

        int mask = player.getPersistentData().getInt(ENCRYPTED_SLOTS) & ALL_MAIN_INVENTORY_SLOTS;
        int available = 27 - Integer.bitCount(mask);
        if (available > 0) {
            int selected = java.util.concurrent.ThreadLocalRandom.current().nextInt(available);
            for (int bit = 0; bit < 27; bit++) {
                if ((mask & 1 << bit) != 0) continue;
                if (selected-- == 0) {
                    mask |= 1 << bit;
                    break;
                }
            }
            player.getPersistentData().putInt(ENCRYPTED_SLOTS, mask);
        }
        RansomPackets.sendToPlayer(player, new EncryptedSlotsPayload(mask, true));
    }

    private static boolean damageGlitch(ServerPlayer player) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_glitch"));
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(key).orElse(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC));
        return player.hurt(new DamageSource(type, player.position()), 1.0F);
    }

    private static void damage(ServerPlayer player, int amount, int coins) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_debt"));
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
            RansomPackets.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), true));
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
            RansomPackets.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), false));
        }
        if (clearDebt) clearProgress(player);
    }

    private static void deleteEncryptedItems(ServerPlayer player) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        int encrypted = player.getPersistentData().getInt(ENCRYPTED_SLOTS);
        for (int bit = 0; bit < 27; bit++) {
            if ((encrypted & 1 << bit) != 0) player.getInventory().setItem(9 + bit, ItemStack.EMPTY);
        }
        player.getInventory().selected = locked != null ? locked.selectedSlot() : 0;
        if (locked != null) {
            RansomPackets.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), false));
        }
        clearProgress(player);
    }

    private static void saveProgress(ServerPlayer player, RansomProgressPayload payload) {
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
        player.getPersistentData().remove(ENCRYPTED_SLOTS);
        LAST_GLITCH_HIT.remove(player.getUUID());
        syncEncryptedSlots(player, false);
    }

    public static boolean isHotbarLocked(Player player) {
        return LOCKED_PLAYERS.containsKey(player.getUUID())
                || player.level().isClientSide() && ClientInfectionTracker.isInfected(player.getUUID());
    }

    public static boolean isInventorySlotEncrypted(Player player, int inventorySlot) {
        if (inventorySlot < 9 || inventorySlot >= 36) return false;
        if (player.level().isClientSide()) return ClientEncryptedSlots.isEncrypted(inventorySlot);
        int mask = player.getPersistentData().getInt(ENCRYPTED_SLOTS);
        return (mask & 1 << (inventorySlot - 9)) != 0;
    }

    public static boolean hasEncryptedInventorySlots(Player player) {
        if (player.level().isClientSide()) {
            for (int slot = 9; slot < 36; slot++) {
                if (ClientEncryptedSlots.isEncrypted(slot)) return true;
            }
            return false;
        }
        return (player.getPersistentData().getInt(ENCRYPTED_SLOTS) & ALL_MAIN_INVENTORY_SLOTS) != 0;
    }

    private static void syncEncryptedSlots(ServerPlayer player, boolean glitchHit) {
        int mask = player.getPersistentData().getInt(ENCRYPTED_SLOTS) & ALL_MAIN_INVENTORY_SLOTS;
        RansomPackets.sendToPlayer(player, new EncryptedSlotsPayload(mask, glitchHit));
    }

    private record LockedInventory(int selectedSlot) {}
}
