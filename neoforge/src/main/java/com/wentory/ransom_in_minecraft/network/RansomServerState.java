package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
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
    private static final Map<UUID, LockedInventory> LOCKED_PLAYERS = new HashMap<>();

    private RansomServerState() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
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
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ransom")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(context -> summonRansom(context.getSource().getPlayerOrException()))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> {
                            int summoned = 0;
                            for (ServerPlayer target : EntityArgument.getPlayers(context, "targets")) {
                                summoned += summonRansom(target);
                            }
                            return summoned;
                        })));
    }

    private static int summonRansom(ServerPlayer target) {
        if (target.getPersistentData().getBoolean(ACTIVE).orElse(false)) return 0;
        PacketDistributor.sendToPlayer(target, new RansomSummonPayload(true));
        return 1;
    }

    @SubscribeEvent
    public static void keepMainHandEmpty(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !LOCKED_PLAYERS.containsKey(player.getUUID())) return;
        InventorySelectionAccessor.selectHidden(player.getInventory());
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
        if (player.getPersistentData().getBoolean(FAILURE_ARMED).orElse(false)) {
            unlockHands(player, false);
            return;
        }
        if (player.getPersistentData().getBoolean(ACTIVE).orElse(false)) {
            player.getPersistentData().putInt(TARGET, player.getPersistentData().getInt(TARGET).orElse(0) + 30);
        }
        unlockHands(player, false);
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        for (UUID infected : LOCKED_PLAYERS.keySet()) {
            PacketDistributor.sendToPlayer(joining, new InfectionStatusPayload(infected, true));
        }
        if (joining.getPersistentData().getBoolean(FAILURE_ARMED).orElse(false)) {
            PacketDistributor.sendToPlayer(joining, new RansomFailureScarePayload(
                    joining.getPersistentData().getInt(COINS).orElse(0),
                    joining.getPersistentData().getBoolean(FAILURE_DELETE_HOTBAR).orElse(false)));
        } else if (joining.getPersistentData().getBoolean(ACTIVE).orElse(false)) {
            PacketDistributor.sendToPlayer(joining, new RansomResumePayload(
                    joining.getPersistentData().getInt(COINS).orElse(0),
                    joining.getPersistentData().getInt(TARGET).orElse(100),
                    joining.getPersistentData().getInt(TICKS).orElse(0)));
        }
    }

    @SubscribeEvent
    public static void playerDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) unlockHands(player, false);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !event.getOriginal().getPersistentData().getBoolean(ACTIVE).orElse(false)) return;
        var source = event.getOriginal().getPersistentData();
        var target = event.getEntity().getPersistentData();
        target.putBoolean(ACTIVE, true);
        target.putInt(COINS, source.getInt(COINS).orElse(0));
        target.putInt(TARGET, source.getInt(TARGET).orElse(100));
        target.putInt(TICKS, source.getInt(TICKS).orElse(0));
        target.putBoolean(FAILURE_ARMED, source.getBoolean(FAILURE_ARMED).orElse(false));
        target.putBoolean(FAILURE_DELETE_HOTBAR, source.getBoolean(FAILURE_DELETE_HOTBAR).orElse(false));
    }

    private static void fail(ServerPlayer player, int coins, boolean deleteHotbar) {
        damage(player, 10, coins);
        if (deleteHotbar) deleteHotbar(player);
        else unlockHands(player);
    }

    private static void damage(ServerPlayer player, int amount, int coins) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_debt"));
        Holder<DamageType> type = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(key).orElse(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
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
                Inventory.isHotbarSlot(player.getInventory().getSelectedSlot()) ? player.getInventory().getSelectedSlot() : 0));
        if (previous == null) {
            PacketDistributor.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), true));
        }
        InventorySelectionAccessor.selectHidden(player.getInventory());
    }

    private static void unlockHands(ServerPlayer player) {
        unlockHands(player, true);
    }

    private static void unlockHands(ServerPlayer player, boolean clearDebt) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        if (locked != null) {
            player.getInventory().setSelectedSlot(locked.selectedSlot());
            PacketDistributor.sendToAllPlayers(new InfectionStatusPayload(player.getUUID(), false));
        }
        if (clearDebt) clearProgress(player);
    }

    private static void deleteHotbar(ServerPlayer player) {
        LockedInventory locked = LOCKED_PLAYERS.remove(player.getUUID());
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.getInventory().setSelectedSlot(locked != null ? locked.selectedSlot() : 0);
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
    }

    public static boolean isHotbarLocked(Player player) {
        return LOCKED_PLAYERS.containsKey(player.getUUID())
                || player.level().isClientSide() && ClientInfectionTracker.isInfected(player.getUUID());
    }

    private record LockedInventory(int selectedSlot) {}
}
