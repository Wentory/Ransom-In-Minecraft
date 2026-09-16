package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.mixin.InventorySelectionAccessor;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RansomServerState {
    public static final AttachmentType<RansomPlayerData> DATA = AttachmentRegistry
            .<RansomPlayerData>builder().persistent(RansomPlayerData.CODEC).copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath(RansomFabric.MODID, "player_state"));
    private static final Map<UUID, Integer> LOCKED_PLAYERS = new HashMap<>();

    private RansomServerState() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(RansomStatePayload.TYPE, RansomStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RansomProgressPayload.TYPE, RansomProgressPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RansomFailureArmedPayload.TYPE, RansomFailureArmedPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InfectionStatusPayload.TYPE, InfectionStatusPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RansomResumePayload.TYPE, RansomResumePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RansomFailureScarePayload.TYPE, RansomFailureScarePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RansomSummonPayload.TYPE, RansomSummonPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RansomStatePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (payload.penaltyDamage() > 0) damage(player, payload.penaltyDamage(), payload.coins());
            else if (payload.failure()) fail(player, payload.coins(), payload.deleteHotbar());
            else if (payload.active()) lockHands(player);
            else unlockHands(player);
        });
        ServerPlayNetworking.registerGlobalReceiver(RansomProgressPayload.TYPE,
                (payload, context) -> saveProgress(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(RansomFailureArmedPayload.TYPE, (payload, context) -> {
            RansomPlayerData old = data(context.player());
            save(context.player(), new RansomPlayerData(old.active(), Math.max(0, payload.coins()),
                    old.target(), old.ticks(), true, payload.deleteHotbar()));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("ransom")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                        .executes(context -> summon(context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    int summoned = 0;
                                    for (ServerPlayer target : EntityArgument.getPlayers(context, "targets"))
                                        summoned += summon(target);
                                    return summoned;
                                }))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (LOCKED_PLAYERS.containsKey(player.getUUID()))
                    ((InventorySelectionAccessor) player.getInventory()).ransom$setSelectedSlot(Inventory.getSelectionSize());
            }
        });
        UseItemCallback.EVENT.register((player, level, hand) ->
                isHotbarLocked(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                isHotbarLocked(player) && !player.getItemInHand(hand).isEmpty()
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                isHotbarLocked(player) && !player.getItemInHand(hand).isEmpty()
                        ? InteractionResult.FAIL : InteractionResult.PASS);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> playerLoggedIn(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> playerLoggedOut(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) unlockHands(player, false);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> playerLoggedIn(newPlayer));
    }

    private static RansomPlayerData data(ServerPlayer player) {
        return ((AttachmentTarget) player).getAttachedOrElse(DATA, RansomPlayerData.EMPTY);
    }

    private static void save(ServerPlayer player, RansomPlayerData value) {
        ((AttachmentTarget) player).setAttached(DATA, value);
    }

    private static int summon(ServerPlayer target) {
        if (data(target).active()) return 0;
        send(target, new RansomSummonPayload(true));
        return 1;
    }

    private static void playerLoggedIn(ServerPlayer joining) {
        MinecraftServer server = joining.level().getServer();
        if (server != null) {
            for (UUID infected : LOCKED_PLAYERS.keySet())
                send(joining, new InfectionStatusPayload(infected, true));
        }
        RansomPlayerData state = data(joining);
        if (state.failureArmed())
            send(joining, new RansomFailureScarePayload(state.coins(), state.deleteHotbar()));
        else if (state.active())
            send(joining, new RansomResumePayload(state.coins(), state.target(), state.ticks()));
    }

    private static void playerLoggedOut(ServerPlayer player) {
        RansomPlayerData state = data(player);
        if (!state.failureArmed() && state.active())
            save(player, new RansomPlayerData(true, state.coins(), state.target() + 30,
                    state.ticks(), false, state.deleteHotbar()));
        unlockHands(player, false);
    }

    private static void fail(ServerPlayer player, int coins, boolean deleteHotbar) {
        damage(player, 10, coins);
        if (deleteHotbar) deleteHotbar(player);
        else unlockHands(player);
    }

    private static void damage(ServerPlayer player, int amount, int coins) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(RansomFabric.MODID, "ransom_debt"));
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
        RansomPlayerData old = data(player);
        save(player, new RansomPlayerData(true, old.coins(), old.target(), old.ticks(),
                old.failureArmed(), old.deleteHotbar()));
        Integer previous = LOCKED_PLAYERS.putIfAbsent(player.getUUID(),
                Inventory.isHotbarSlot(player.getInventory().getSelectedSlot())
                        ? player.getInventory().getSelectedSlot() : 0);
        if (previous == null) broadcast(player.level().getServer(), new InfectionStatusPayload(player.getUUID(), true));
        ((InventorySelectionAccessor) player.getInventory()).ransom$setSelectedSlot(Inventory.getSelectionSize());
    }

    private static void unlockHands(ServerPlayer player) { unlockHands(player, true); }

    private static void unlockHands(ServerPlayer player, boolean clearDebt) {
        Integer slot = LOCKED_PLAYERS.remove(player.getUUID());
        if (slot != null) {
            player.getInventory().setSelectedSlot(slot);
            broadcast(player.level().getServer(), new InfectionStatusPayload(player.getUUID(), false));
        }
        if (clearDebt) save(player, RansomPlayerData.EMPTY);
    }

    private static void deleteHotbar(ServerPlayer player) {
        Integer slot = LOCKED_PLAYERS.remove(player.getUUID());
        for (int i = 0; i < Inventory.getSelectionSize(); i++) player.getInventory().setItem(i, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(slot != null ? slot : 0);
        if (slot != null) broadcast(player.level().getServer(), new InfectionStatusPayload(player.getUUID(), false));
        save(player, RansomPlayerData.EMPTY);
    }

    private static void saveProgress(ServerPlayer player, RansomProgressPayload payload) {
        RansomPlayerData old = data(player);
        save(player, new RansomPlayerData(true, Math.max(0, payload.coins()),
                Math.max(1, payload.targetCoins()), Math.max(0, payload.phaseTicks()),
                old.failureArmed(), old.deleteHotbar()));
    }

    private static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
        if (ServerPlayNetworking.canSend(player, packet.type())) ServerPlayNetworking.send(player, packet);
    }

    private static void broadcast(MinecraftServer server, net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, packet);
    }

    public static boolean isHotbarLocked(Player player) {
        return LOCKED_PLAYERS.containsKey(player.getUUID())
                || player.level().isClientSide() && ClientInfectionTracker.isInfected(player.getUUID());
    }
}