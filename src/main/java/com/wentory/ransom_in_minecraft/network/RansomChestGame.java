package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.StealerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import com.wentory.ransom_in_minecraft.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class RansomChestGame {
    private static final String INFECTED = "ransom_infected_chest";
    private static final String OWNER = "ransom_infected_owner";
    private static final String SESSION = "ransom_chest_session";
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final List<PendingHit> PENDING_HITS = new ArrayList<>();
    private static final List<PendingLidClose> PENDING_LIDS = new ArrayList<>();
    private static final String[] KEYS = {"A", "S", "D", "F", "G", "H", "J", "K", "L", "W", "E", "R"};
    private static final float ALT_F4_CHANCE = 0.02F;
    private static final int INITIAL_OPEN_TICKS = 49;

    private RansomChestGame() {
    }

    public static void registerPayloads(PayloadRegistrar registrar) {
        registrar.playToClient(ChestInfectionPayload.TYPE, ChestInfectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.client.RansomChestVisuals.accept(payload)));
        registrar.playToClient(ChestQteStartPayload.TYPE, ChestQteStartPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.client.RansomChestScreen.open(payload)));
        registrar.playToClient(ChestQteAdvancePayload.TYPE, ChestQteAdvancePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.client.RansomChestScreen.advance(payload)));
        registrar.playToClient(ChestQteAbortPayload.TYPE, ChestQteAbortPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.client.RansomChestScreen.abort(payload)));
        registrar.playToClient(ChestOutcomePayload.TYPE, ChestOutcomePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.client.RansomChestVisuals.showOutcome(payload)));
        registrar.playToClient(ChestLostItemPayload.TYPE, ChestLostItemPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.wentory.ransom_in_minecraft.client.RansomChestVisuals.showLostItem(payload)));
        registrar.playToServer(ChestQteResultPayload.TYPE, ChestQteResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) handleResult(player, payload);
                });
        registrar.playToServer(ChestQteProgressPayload.TYPE, ChestQteProgressPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) saveProgress(player, payload);
                });
        registrar.playToServer(ChestQteFinishPayload.TYPE, ChestQteFinishPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) finish(player, payload);
                });
    }

    @SubscribeEvent
    public static void openChest(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof ChestBlock chestBlock)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof ChestBlockEntity clicked)) return;

        List<ChestBlockEntity> chests = chestParts((ServerLevel) event.getLevel(), event.getPos(), state, clicked);
        boolean infected = chests.stream().anyMatch(RansomChestGame::isInfected);
        if (!StealerConfig.ENABLED.get()) {
            if (infected) {
                Session existing = findSession((ServerLevel) event.getLevel(), chests);
                if (existing != null) abortSession(existing);
                else clearInfection((ServerLevel) event.getLevel(), chests);
            }
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            if (infected) event.setCanceled(true);
            return;
        }
        Session existing = findSession((ServerLevel) event.getLevel(), chests);
        if (existing != null) {
            event.setCanceled(true);
            advanceExpired(existing, System.currentTimeMillis());
            if (existing.round >= existing.groups.size()) {
                finishSession(existing, null);
                openVanillaChest(player, event.getPos());
            } else if (!existing.player.equals(player.getUUID())) {
                player.displayClientMessage(Component.literal("RANSOM is still playing with this chest."), true);
            } else {
                openSealed(player, existing, true);
            }
            return;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            event.setCanceled(true);
            return;
        }
        boolean hasLootTable = chests.stream().anyMatch(chest -> chest.saveWithoutMetadata().contains("LootTable"));
        if (RansomServerState.isEncounterActive(player) && (infected || hasLootTable)) {
            event.setCanceled(true);
            return;
        }
        if (!infected && !hasLootTable) return;
        if (!infected && ThreadLocalRandom.current().nextInt(100)
                >= StealerConfig.INFECTION_CHANCE_PERCENT.get()) return;

        event.setCanceled(true);
        UUID owner = infectedOwner(chests);
        if (owner != null && !owner.equals(player.getUUID()) && SESSIONS.containsKey(owner)) {
            player.displayClientMessage(Component.literal("RANSOM is already scanning this chest."), true);
            return;
        }

        for (ChestBlockEntity chest : chests) chest.unpackLootTable(player);
        List<LootGroup> groups = groupLoot(chests);
        if (groups.isEmpty()) {
            clearInfection((ServerLevel) event.getLevel(), chests);
            openVanillaChest(player, event.getPos());
            return;
        }

        UUID sessionId = UUID.randomUUID();
        Session session = new Session(sessionId, player.getUUID(), (ServerLevel) event.getLevel(),
                event.getPos().immutable(), List.copyOf(chests), groups);
        session.sequence = createSequence();
        session.deadlineMillis = System.currentTimeMillis()
                + (INITIAL_OPEN_TICKS + timeForRound(sequenceLength(session.sequence))) * 50L
                + latencyGraceMillis(player);
        SESSIONS.put(player.getUUID(), session);
        markInfected(session, player.getUUID());
        saveSession(session);
        openSealed(player, session, false);
    }

    private static void openSealed(ServerPlayer player, Session session, boolean resumed) {
        int rows = session.chests.size() == 2 ? 6 : 3;
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new SealedChestMenu(id, inventory, rows),
                rows == 6 ? Component.translatable("container.chestDouble")
                        : Component.translatable("container.chest")));
        PacketDistributor.sendToPlayer(player, new ChestQteStartPayload(session.id, session.pos.asLong(),
                session.round + 1, session.groups.size(), session.sequence, session.keyIndex,
                resumed ? Math.max(0L, session.deadlineMillis - System.currentTimeMillis()
                        - latencyGraceMillis(player))
                        : (INITIAL_OPEN_TICKS + timeForRound(sequenceLength(session.sequence))) * 50L,
                resumed));
    }

    public static void handleResult(ServerPlayer player, ChestQteResultPayload payload) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id.equals(payload.sessionId()) || session.waitingForFinish
                || payload.round() != session.round + 1) return;
        long now = System.currentTimeMillis();
        if (now >= session.deadlineMillis) {
            advanceExpired(session, now);
            return;
        }
        resolveRound(session, player, payload.success(), now);
    }

    private static void resolveRound(Session session, ServerPlayer player, boolean success, long now) {
        LootGroup group = session.groups.get(session.round);
        boolean altTrap = success && session.sequence.endsWith("ALT+F4");
        String lostItem = "";
        int lostCount = 0;
        if (success) {
            session.savedRounds++;
        } else {
            lostItem = BuiltInRegistries.ITEM.getKey(group.item).toString();
            lostCount = removeItem(session.chests, group.item);
            PacketDistributor.sendToAllPlayers(new ChestLostItemPayload(
                    session.level.dimension().location().toString(), session.pos.asLong(), lostItem));
            openChestLid(session, now, 1500L);
        }
        session.round++;
        session.keyIndex = 0;

        int outcome = ChestQteAdvancePayload.NOT_FINISHED;
        String next = "";
        if (session.round >= session.groups.size()) {
            outcome = session.savedRounds == session.groups.size() ? ChestQteAdvancePayload.SAVED_ALL
                    : session.savedRounds == 0 ? ChestQteAdvancePayload.SAVED_NONE
                    : ChestQteAdvancePayload.SAVED_SOME;
            session.finalOutcome = outcome;
            session.waitingForFinish = true;
            session.sequence = "";
            session.deadlineMillis = now + (success ? altTrap ? 14L * 50L : 0L : 30L * 50L)
                    + (outcome == ChestQteAdvancePayload.SAVED_ALL ? 26L : 42L) * 50L
                    + (player == null ? 0L : latencyGraceMillis(player));
        } else {
            next = createSequence();
            session.sequence = next;
            session.deadlineMillis = now + (success ? altTrap ? 14L : 8L : 30L) * 50L
                    + timeForRound(sequenceLength(next)) * 50L
                    + (player == null ? 0L : latencyGraceMillis(player));
        }
        saveSession(session);
        if (player != null && player.containerMenu instanceof SealedChestMenu) {
            PacketDistributor.sendToPlayer(player, new ChestQteAdvancePayload(session.id, success,
                    lostItem, lostCount, session.round + 1, session.groups.size(), next, outcome,
                    next.isEmpty() ? 0L : timeForRound(sequenceLength(next)) * 50L));
        }
    }

    private static void saveProgress(ServerPlayer player, ChestQteProgressPayload payload) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id.equals(payload.sessionId()) || session.waitingForFinish
                || payload.round() != session.round + 1 || System.currentTimeMillis() >= session.deadlineMillis
                || payload.keyIndex() < session.keyIndex || payload.keyIndex() >= 10
                || payload.sequence().length() > 100) return;
        session.keyIndex = payload.keyIndex();
        session.sequence = payload.sequence();
        saveSession(session);
    }

    public static int spawnDungeonChest(ServerPlayer player) {
        return spawnChest(player, new ResourceLocation("chests/simple_dungeon"));
    }

    public static int infectLookedAtChest(ServerPlayer player) {
        if (!StealerConfig.ENABLED.get()) {
            player.displayClientMessage(Component.literal("RANSOM: STEALER is disabled in this world."), false);
            return 0;
        }
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.displayClientMessage(Component.literal("Look at an unopened loot-table chest."), false);
            return 0;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            player.displayClientMessage(Component.literal("Look at an unopened loot-table chest."), false);
            return 0;
        }
        List<ChestBlockEntity> parts = chestParts(level, pos, state, chest);
        if (parts.stream().anyMatch(RansomChestGame::isInfected)) {
            player.displayClientMessage(Component.literal("This chest is already infected."), false);
            return 0;
        }
        if (parts.stream().noneMatch(part -> part.saveWithoutMetadata().contains("LootTable"))) {
            player.displayClientMessage(Component.literal("This chest has no unopened loot table."), false);
            return 0;
        }
        for (ChestBlockEntity part : parts) {
            part.getPersistentData().putBoolean(INFECTED, true);
            part.setChanged();
            broadcast(level, part.getBlockPos(), true);
        }
        player.displayClientMessage(Component.literal("Infected the unopened chest."), false);
        return 1;
    }

    public static int spawnChest(ServerPlayer player, ResourceLocation lootTableId) {
        if (!StealerConfig.ENABLED.get()) {
            player.displayClientMessage(Component.literal("RANSOM: STEALER is disabled in this world."), false);
            return 0;
        }
        ServerLevel level = player.serverLevel();
        if (!lootTableId.getPath().startsWith("chests/")
                || !level.getServer().getLootData().getKeys(net.minecraft.world.level.storage.loot.LootDataType.TABLE).contains(lootTableId)) {
            player.displayClientMessage(Component.literal("Unknown chest loot table: " + lootTableId), false);
            return 0;
        }
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
        for (int offset = 0; offset < 4 && !level.getBlockState(pos).canBeReplaced(); offset++) {
            pos = pos.above();
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            player.displayClientMessage(Component.literal("Could not find space for the infected chest."), false);
            return 0;
        }

        BlockState state = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, player.getDirection().getOpposite());
        if (!level.setBlock(pos, state, 3)
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return 0;

        chest.setLootTable(lootTableId, level.random.nextLong());
        chest.getPersistentData().putBoolean(INFECTED, true);
        chest.setChanged();
        broadcast(level, pos, true);
        player.displayClientMessage(Component.literal("Spawned an infected chest with loot table "
                + lootTableId + "."), false);
        return 1;
    }

    public static void finish(ServerPlayer player, ChestQteFinishPayload payload) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.id.equals(payload.sessionId()) || !session.waitingForFinish) return;
        finishSession(session, player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        Session session = SESSIONS.get(event.getEntity().getUUID());
        if (session != null) saveSession(session);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 5 != 0) return;
        if (!StealerConfig.ENABLED.get()) {
            for (Session session : List.copyOf(SESSIONS.values())) {
                if (session.level.getServer() == event.getServer()) abortSession(session);
            }
        }
        long now = System.currentTimeMillis();
        for (var iterator = PENDING_HITS.iterator(); iterator.hasNext();) {
            PendingHit hit = iterator.next();
            if (now < hit.whenMillis()) continue;
            ServerPlayer target = event.getServer().getPlayerList().getPlayer(hit.player());
            if (target != null && target.serverLevel() == hit.level())
                PacketDistributor.sendToPlayer(target, new RansomForcedAttackPayload(true));
            iterator.remove();
        }
        for (var iterator = PENDING_LIDS.iterator(); iterator.hasNext();) {
            PendingLidClose close = iterator.next();
            if (now < close.whenMillis()) continue;
            BlockState state = close.level().getBlockState(close.pos());
            if (state.getBlock() instanceof ChestBlock) close.level().blockEvent(close.pos(), state.getBlock(), 1, 0);
            iterator.remove();
        }
        for (Session session : List.copyOf(SESSIONS.values())) {
            if (session.level.getServer() != event.getServer() || session.chests.stream().noneMatch(
                    chest -> chest.getBlockPos().equals(session.pos)
                            && session.level.getBlockEntity(session.pos) == chest)) {
                SESSIONS.remove(session.player);
                continue;
            }
            advanceExpired(session, now);
        }
    }

    @SubscribeEvent
    public static void syncChunkInfections(ChunkWatchEvent.Watch event) {
        for (var blockEntity : event.getChunk().getBlockEntities().values()) {
            if (blockEntity instanceof ChestBlockEntity chest && isInfected(chest)) {
                BlockState state = event.getLevel().getBlockState(chest.getBlockPos());
                if (!StealerConfig.ENABLED.get()) {
                    if (state.getBlock() instanceof ChestBlock)
                        clearInfection(event.getLevel(), chestParts(event.getLevel(), chest.getBlockPos(), state, chest));
                    continue;
                }
                if (state.getBlock() instanceof ChestBlock) {
                    Session session = findSession(event.getLevel(), chestParts(event.getLevel(), chest.getBlockPos(), state, chest));
                    if (session != null) advanceExpired(session, System.currentTimeMillis());
                }
                if (!isInfected(chest)) continue;
                PacketDistributor.sendToPlayer(event.getPlayer(), new ChestInfectionPayload(
                        event.getLevel().dimension().location().toString(), chest.getBlockPos().asLong(), true));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakInfectedChest(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            ServerPlayer nearest = destroyInfectedChest(level, event.getPos());
            if (nearest != null) PacketDistributor.sendToPlayer(nearest, new RansomForcedAttackPayload(true));
        }
    }

    @SubscribeEvent
    public static void explodeInfectedChests(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Set<UUID> attacked = new HashSet<>();
        for (BlockPos pos : event.getAffectedBlocks()) {
            ServerPlayer nearest = destroyInfectedChest(level, pos);
            if (nearest != null && attacked.add(nearest.getUUID()))
                PacketDistributor.sendToPlayer(nearest, new RansomForcedAttackPayload(true));
        }
    }

    private static ServerPlayer destroyInfectedChest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)
                || !isInfected(chest)) return null;

        List<ChestBlockEntity> parts = chestParts(level, pos, state, chest);
        for (Session session : List.copyOf(SESSIONS.values())) {
            if (session.level != level || session.chests.stream()
                    .noneMatch(part -> part.getBlockPos().equals(pos))) continue;
            abortSession(session);
        }
        clearInfection(level, parts);
        if (!StealerConfig.ENABLED.get()) return null;
        ServerPlayer nearest = null;
        double nearestDistance = 12.0D * 12.0D;
        for (ServerPlayer candidate : level.players()) {
            if (candidate.isSpectator()) continue;
            double distance = candidate.distanceToSqr(pos.getCenter());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static void abortSession(Session session) {
        ServerPlayer participant = session.level.getServer().getPlayerList().getPlayer(session.player);
        if (participant != null) {
            PacketDistributor.sendToPlayer(participant, new ChestQteAbortPayload(session.id));
            participant.closeContainer();
        }
        SESSIONS.remove(session.player, session);
        clearInfection(session.level, session.chests);
    }

    private static List<ChestBlockEntity> chestParts(ServerLevel level, BlockPos pos, BlockState state,
                                                      ChestBlockEntity clicked) {
        List<ChestBlockEntity> result = new ArrayList<>();
        result.add(clicked);
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
            if (level.getBlockEntity(otherPos) instanceof ChestBlockEntity other) result.add(other);
        }
        return result;
    }

    private static List<LootGroup> groupLoot(Collection<ChestBlockEntity> chests) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ChestBlockEntity chest : chests) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        List<LootGroup> result = new ArrayList<>();
        counts.forEach((item, count) -> result.add(new LootGroup(item, count)));
        return result;
    }

    private static int removeItem(Collection<ChestBlockEntity> chests, Item item) {
        int removed = 0;
        for (ChestBlockEntity chest : chests) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (!stack.isEmpty() && stack.is(item)) {
                    removed += stack.getCount();
                    chest.setItem(slot, ItemStack.EMPTY);
                }
            }
            chest.setChanged();
        }
        return removed;
    }

    private static Session findSession(ServerLevel level, List<ChestBlockEntity> chests) {
        for (ChestBlockEntity chest : chests) {
            if (!chest.getPersistentData().contains(SESSION)) continue;
            CompoundTag data = chest.getPersistentData().getCompound(SESSION);
            if (!data.hasUUID("player") || !data.hasUUID("id")) continue;
            UUID owner = data.getUUID("player");
            Session active = SESSIONS.get(owner);
            if (active != null && active.level == level && active.id.equals(data.getUUID("id"))
                    && active.chests.stream().anyMatch(part -> part.getBlockPos().equals(active.pos)
                    && level.getBlockEntity(active.pos) == part)) return active;
            List<LootGroup> groups = new ArrayList<>();
            for (String id : data.getString("groups").split(",")) {
                ResourceLocation location = ResourceLocation.tryParse(id);
                if (location != null) groups.add(new LootGroup(BuiltInRegistries.ITEM.get(location), 0));
            }
            if (groups.isEmpty()) return null;
            Session loaded = new Session(data.getUUID("id"), owner, level,
                    BlockPos.of(data.getLong("pos")), List.copyOf(chests), groups);
            loaded.round = data.getInt("round");
            loaded.savedRounds = data.getInt("saved");
            loaded.keyIndex = data.getInt("keyIndex");
            loaded.sequence = data.getString("sequence");
            loaded.deadlineMillis = data.getLong("deadline");
            loaded.finalOutcome = data.getInt("outcome");
            loaded.waitingForFinish = data.getBoolean("ending");
            SESSIONS.put(owner, loaded);
            return loaded;
        }
        return null;
    }

    private static void saveSession(Session session) {
        CompoundTag data = new CompoundTag();
        data.putUUID("id", session.id);
        data.putUUID("player", session.player);
        data.putLong("pos", session.pos.asLong());
        data.putString("groups", String.join(",", session.groups.stream()
                .map(group -> BuiltInRegistries.ITEM.getKey(group.item).toString()).toList()));
        data.putInt("round", session.round);
        data.putInt("saved", session.savedRounds);
        data.putInt("keyIndex", session.keyIndex);
        data.putString("sequence", session.sequence);
        data.putLong("deadline", session.deadlineMillis);
        data.putInt("outcome", session.finalOutcome);
        data.putBoolean("ending", session.waitingForFinish);
        for (ChestBlockEntity chest : session.chests) {
            chest.getPersistentData().put(SESSION, data.copy());
            chest.setChanged();
        }
    }

    private static void advanceExpired(Session session, long now) {
        while (!session.waitingForFinish && session.round < session.groups.size()
                && now >= session.deadlineMillis) {
            ServerPlayer player = session.level.getServer().getPlayerList().getPlayer(session.player);
            resolveRound(session, player, false, session.deadlineMillis);
        }
        if (session.waitingForFinish && now >= session.deadlineMillis) finishSession(session, null);
    }

    private static void finishSession(Session session, ServerPlayer player) {
        if (SESSIONS.get(session.player) != session) return;
        SESSIONS.remove(session.player);
        clearInfection(session.level, session.chests);
        ServerPlayer target = null;
        if (session.finalOutcome == ChestQteAdvancePayload.SAVED_NONE) {
            double nearest = 12.0D * 12.0D;
            for (ServerPlayer candidate : session.level.players()) {
                if (candidate.isSpectator() || RansomServerState.isEncounterActive(candidate)) continue;
                double distance = candidate.distanceToSqr(session.pos.getCenter());
                if (distance < nearest) {
                    nearest = distance;
                    target = candidate;
                }
            }
            openChestLid(session, System.currentTimeMillis(), 1100L);
            if (target != null) PENDING_HITS.add(new PendingHit(session.level, target.getUUID(),
                    System.currentTimeMillis() + 1100L));
        }
        PacketDistributor.sendToAllPlayers(new ChestOutcomePayload(
                session.level.dimension().location().toString(), session.pos.asLong(),
                session.finalOutcome, target == null ? -1 : target.getId()));
        if (player != null && player.containerMenu instanceof SealedChestMenu) {
            if (session.finalOutcome == ChestQteAdvancePayload.SAVED_NONE) player.closeContainer();
            else openVanillaChest(player, session.pos);
        }
    }

    private record PendingHit(ServerLevel level, UUID player, long whenMillis) {
    }

    private record PendingLidClose(ServerLevel level, BlockPos pos, long whenMillis) {
    }

    private static int timeForRound(int length) {
        int ticks = 0;
        for (int index = 0; index < length; index++) ticks += index < 5 ? 10 : 12 + (index - 5) * 2;
        return ticks;
    }

    private static long latencyGraceMillis(ServerPlayer player) {
        return Math.min(2500L, Math.max(150L, player.latency + 100L));
    }

    private static int sequenceLength(String sequence) {
        return sequence.split("\\|").length;
    }

    private static void openChestLid(Session session, long now, long durationMillis) {
        for (ChestBlockEntity chest : session.chests) {
            BlockPos pos = chest.getBlockPos();
            BlockState state = session.level.getBlockState(pos);
            if (!(state.getBlock() instanceof ChestBlock)) continue;
            PENDING_LIDS.removeIf(close -> close.level() == session.level && close.pos().equals(pos));
            session.level.blockEvent(pos, state.getBlock(), 1, 1);
            PENDING_LIDS.add(new PendingLidClose(session.level, pos, now + durationMillis));
        }
    }

    private static void markInfected(Session session, UUID owner) {
        for (ChestBlockEntity chest : session.chests) {
            chest.getPersistentData().putBoolean(INFECTED, true);
            chest.getPersistentData().putUUID(OWNER, owner);
            chest.setChanged();
            broadcast(session.level, chest.getBlockPos(), true);
        }
    }

    private static void clearInfection(ServerLevel level, Collection<ChestBlockEntity> chests) {
        for (ChestBlockEntity chest : chests) {
            chest.getPersistentData().remove(INFECTED);
            chest.getPersistentData().remove(OWNER);
            chest.getPersistentData().remove(SESSION);
            chest.setChanged();
            broadcast(level, chest.getBlockPos(), false);
        }
    }

    private static boolean isInfected(ChestBlockEntity chest) {
        return chest.getPersistentData().getBoolean(INFECTED);
    }

    private static UUID infectedOwner(Collection<ChestBlockEntity> chests) {
        for (ChestBlockEntity chest : chests) {
            if (chest.getPersistentData().hasUUID(OWNER)) return chest.getPersistentData().getUUID(OWNER);
        }
        return null;
    }

    private static void broadcast(ServerLevel level, BlockPos pos, boolean infected) {
        PacketDistributor.sendToAllPlayers(new ChestInfectionPayload(level.dimension().location().toString(),
                pos.asLong(), infected));
    }

    private static String createSequence() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(3, 11);
        List<String> sequence = new ArrayList<>();
        for (int i = 0; i < length; i++) sequence.add(KEYS[random.nextInt(KEYS.length)]);
        if (random.nextFloat() < ALT_F4_CHANCE) sequence.set(length - 1, "ALT+F4");
        return String.join("|", sequence);
    }

    private static void openVanillaChest(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) return;
        Container container = ChestBlock.getContainer(chestBlock, state, player.level(), pos, false);
        if (container == null) return;
        int rows = container.getContainerSize() / 9;
        MenuProvider provider = new SimpleMenuProvider((id, inventory, ignored) ->
                rows == 6 ? ChestMenu.sixRows(id, inventory, container)
                        : ChestMenu.threeRows(id, inventory, container),
                rows == 6 ? Component.translatable("container.chestDouble")
                        : Component.translatable("container.chest"));
        player.openMenu(provider);
    }

    private record LootGroup(Item item, int count) {
    }

    private static final class Session {
        private final UUID id;
        private final UUID player;
        private final ServerLevel level;
        private final BlockPos pos;
        private final List<ChestBlockEntity> chests;
        private final List<LootGroup> groups;
        private int round;
        private int savedRounds;
        private int finalOutcome;
        private boolean waitingForFinish;
        private int keyIndex;
        private String sequence = "";
        private long deadlineMillis;

        private Session(UUID id, UUID player, ServerLevel level, BlockPos pos,
                        List<ChestBlockEntity> chests, List<LootGroup> groups) {
            this.id = id;
            this.player = player;
            this.level = level;
            this.pos = pos;
            this.chests = chests;
            this.groups = groups;
        }
    }
}
