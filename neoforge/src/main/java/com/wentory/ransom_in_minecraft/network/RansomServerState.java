package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
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
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.network.PacketDistributor;

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

    private RansomServerState() {}

    public static boolean isEncounterActive(ServerPlayer player) {
        return player.getPersistentData().getBooleanOr(ACTIVE, false);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(CommonSettingsPayload.TYPE, CommonSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::accept));
        registrar.playToClient(WormInfectionPayload.TYPE, WormInfectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> WormEffects.accept(payload.value())));
        registrar.playToServer(WormSettingsPayload.TYPE, WormSettingsPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER) && !player.level().getServer().isSingleplayerOwner(player.nameAndId())) return;
            StealerConfig.WORM_ENABLED.set(payload.enabled());
            StealerConfig.WORM_HEAL_TICKS.set(Math.clamp(payload.healingTicks(), 1, 72000));
            StealerConfig.HIJACK_ZOMBIE_CHANCE.set(Math.clamp(payload.zombieChance(), 0, 100));
            StealerConfig.HIJACK_DROWNED_CHANCE.set(Math.clamp(payload.drownedChance(), 0, 100));
            StealerConfig.HIJACK_CREEPER_CHANCE.set(Math.clamp(payload.creeperChance(), 0, 100));
            StealerConfig.SPEC.save();
            PacketDistributor.sendToAllPlayers(CommonSettingsPayload.current());
        });
        RansomChestGame.registerPayloads(registrar);
        registrar.playToServer(StealerSettingsPayload.TYPE, StealerSettingsPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || !player.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
                            && !player.level().getServer().isSingleplayerOwner(player.nameAndId())) return;
                    StealerConfig.ENABLED.set(payload.enabled());
                    StealerConfig.INFECTION_CHANCE_PERCENT.set(
                            Math.max(0, Math.min(100, payload.chancePercent())));
                    StealerConfig.SPEC.save();
                    PacketDistributor.sendToAllPlayers(CommonSettingsPayload.current());
                });
        registrar.playToServer(RansomStatePayload.TYPE, RansomStatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    if (payload.penaltyDamage() > 0) damage(player, payload.penaltyDamage(), payload.coins());
                    else if (payload.failure()) fail(player, payload.coins(), payload.deleteHotbar());
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
                    player.getPersistentData().putBoolean(FAILURE_DELETE_HOTBAR, payload.deleteHotbar());
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
        registrar.playToClient(NaturalSpawnStatePayload.TYPE, NaturalSpawnStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientNaturalSpawnState.accept(payload)));
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
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new PlayerVisualEffectPayload(player.getUUID(), effect));
                });
        registrar.playToClient(PlayerVisualEffectPayload.TYPE, PlayerVisualEffectPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPlayerVisualEffectTracker.accept(payload)));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ransom")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
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
                        .then(Commands.argument("loot_table", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().getServer().reloadableRegistries()
                                                .lookup().lookupOrThrow(Registries.LOOT_TABLE).listElementIds().map(net.minecraft.resources.ResourceKey::identifier)
                                                .filter(id -> id.getPath().startsWith("chests/")), builder))
                                .executes(context -> RansomChestGame.spawnChest(
                                        context.getSource().getPlayerOrException(),
                                        IdentifierArgument.getId(context, "loot_table")))))
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
            syncNaturalSpawnState(target);
        }
        source.sendSuccess(() -> Component.literal("Natural Ransom spawning "
                + (enabled ? "enabled" : "disabled") + " for " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int showNaturalSpawnStatus(net.minecraft.commands.CommandSourceStack source,
                                              java.util.Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            boolean enabled = !target.getPersistentData().getBooleanOr(NATURAL_SPAWN_DISABLED, false);
            source.sendSuccess(() -> Component.literal(target.getGameProfile().name()
                    + ": natural Ransom spawning is " + (enabled ? "enabled" : "disabled") + "."), false);
        }
        return targets.size();
    }

    private static int cancelRansom(net.minecraft.commands.CommandSourceStack source,
                                    java.util.Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            unlockHands(target);
            PacketDistributor.sendToPlayer(target, new RansomCancelPayload(true));
        }
        source.sendSuccess(() -> Component.literal("Cancelled Ransomware for "
                + targets.size() + " player(s)."), true);
        return targets.size();
    }

    static int summonRansom(ServerPlayer target) {
        if (target.getPersistentData().getBooleanOr(ACTIVE, false)) return 0;
        PacketDistributor.sendToPlayer(target, new RansomSummonPayload(true));
        return 1;
    }

    @SubscribeEvent
    public static void keepMainHandEmpty(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !LOCKED_PLAYERS.containsKey(player.getUUID())) return;
        ((com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor) player.getInventory()).ransom$setSelectedSlot(Inventory.getSelectionSize());
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
        LAST_GLITCH_HIT.remove(player.getUUID());
        if (player.getPersistentData().getBooleanOr(FAILURE_ARMED, false)) {
            unlockHands(player, false);
            return;
        }
        if (player.getPersistentData().getBooleanOr(ACTIVE, false)) {
            player.getPersistentData().putInt(TARGET, player.getPersistentData().getIntOr(TARGET, 0) + 30);
        }
        unlockHands(player, false);
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        syncNaturalSpawnState(joining);
        syncEncryptedSlots(joining, false);
        for (UUID infected : LOCKED_PLAYERS.keySet()) {
            PacketDistributor.sendToPlayer(joining, new InfectionStatusPayload(infected, true));
        }
        if (joining.getPersistentData().getBooleanOr(FAILURE_ARMED, false)) {
            PacketDistributor.sendToPlayer(joining, new RansomFailureScarePayload(
                    joining.getPersistentData().getIntOr(COINS, 0),
                    joining.getPersistentData().getBooleanOr(FAILURE_DELETE_HOTBAR, false)));
        } else if (joining.getPersistentData().getBooleanOr(ACTIVE, false)) {
            PacketDistributor.sendToPlayer(joining, new RansomResumePayload(
                    joining.getPersistentData().getIntOr(COINS, 0),
                    joining.getPersistentData().getIntOr(TARGET, 0),
                    joining.getPersistentData().getIntOr(TICKS, 0)));
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
        if (source.getBooleanOr(NATURAL_SPAWN_DISABLED, false)) {
            target.putBoolean(NATURAL_SPAWN_DISABLED, true);
        }
        if (!source.getBooleanOr(ACTIVE, false)) return;
        target.putBoolean(ACTIVE, true);
        target.putInt(COINS, source.getIntOr(COINS, 0));
        target.putInt(TARGET, source.getIntOr(TARGET, 0));
        target.putInt(TICKS, source.getIntOr(TICKS, 0));
        target.putBoolean(FAILURE_ARMED, source.getBooleanOr(FAILURE_ARMED, false));
        target.putBoolean(FAILURE_DELETE_HOTBAR, source.getBooleanOr(FAILURE_DELETE_HOTBAR, false));
        target.putInt(ENCRYPTED_SLOTS, source.getIntOr(ENCRYPTED_SLOTS, 0));
        syncEncryptedSlots((ServerPlayer) event.getEntity(), false);
    }

    private static void syncNaturalSpawnState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new NaturalSpawnStatePayload(
                !player.getPersistentData().getBooleanOr(NATURAL_SPAWN_DISABLED, false)));
    }

    private static void fail(ServerPlayer player, int coins, boolean deleteHotbar) {
        if (deleteHotbar) deleteEncryptedItems(player);
        else unlockHands(player);
        damage(player, 10, coins);
    }

    private static void handleGlitchContact(ServerPlayer player) {
        if (!player.getPersistentData().getBooleanOr(ACTIVE, false) || player.isDeadOrDying()) return;
        long now = player.level().getGameTime();
        long previous = LAST_GLITCH_HIT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < 40L || !damageGlitch(player)) return;
        LAST_GLITCH_HIT.put(player.getUUID(), now);
        WormInfection.add(player, 5);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new PlayerGlitchPayload(player.getUUID()));

        int mask = player.getPersistentData().getIntOr(ENCRYPTED_SLOTS, 0) & ALL_MAIN_INVENTORY_SLOTS;
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
        PacketDistributor.sendToPlayer(player, new EncryptedSlotsPayload(mask, true));
    }

    private static boolean damageGlitch(ServerPlayer player) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_glitch"));
        Holder<DamageType> type = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(key).orElse(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC));
        return player.hurtServer(player.level(), new DamageSource(type, player.position()), 1.0F);
    }

    private static void damage(ServerPlayer player, int amount, int coins) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_debt"));
        Holder<DamageType> type = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(key).orElse(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC));
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
                Inventory.isHotbarSlot(player.getInventory().getSelectedSlot()) ? player.getInventory().getSelectedSlot() : 0));
        if (previous == null) {
            PacketDistributor.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), true));
        }
        ((com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor) player.getInventory()).ransom$setSelectedSlot(Inventory.getSelectionSize());
    }

    private static void unlockHands(ServerPlayer player) {
        unlockHands(player, true);
    }

    private static void unlockHands(ServerPlayer player, boolean clearDebt) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        if (locked != null) {
            ((com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor) player.getInventory()).ransom$setSelectedSlot(locked.selectedSlot());
            PacketDistributor.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), false));
        }
        if (clearDebt) clearProgress(player);
    }

    private static void deleteEncryptedItems(ServerPlayer player) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        int encrypted = player.getPersistentData().getIntOr(ENCRYPTED_SLOTS, 0);
        for (int bit = 0; bit < 27; bit++) {
            if ((encrypted & 1 << bit) != 0) player.getInventory().setItem(9 + bit, ItemStack.EMPTY);
        }
        ((com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor) player.getInventory()).ransom$setSelectedSlot(locked != null ? locked.selectedSlot() : 0);
        if (locked != null) {
            PacketDistributor.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), false));
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
        int mask = player.getPersistentData().getIntOr(ENCRYPTED_SLOTS, 0);
        return (mask & 1 << (inventorySlot - 9)) != 0;
    }

    public static boolean hasEncryptedInventorySlots(Player player) {
        if (player.level().isClientSide()) {
            for (int slot = 9; slot < 36; slot++) {
                if (ClientEncryptedSlots.isEncrypted(slot)) return true;
            }
            return false;
        }
        return (player.getPersistentData().getIntOr(ENCRYPTED_SLOTS, 0) & ALL_MAIN_INVENTORY_SLOTS) != 0;
    }

    private static void syncEncryptedSlots(ServerPlayer player, boolean glitchHit) {
        int mask = player.getPersistentData().getIntOr(ENCRYPTED_SLOTS, 0) & ALL_MAIN_INVENTORY_SLOTS;
        PacketDistributor.sendToPlayer(player, new EncryptedSlotsPayload(mask, glitchHit));
    }

    private record LockedInventory(int selectedSlot) {}
}
