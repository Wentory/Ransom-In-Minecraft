package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import com.wentory.ransom_in_minecraft.network.PacketDistributor;
import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks Crucifix banishment effects and synchronizes them with players. */
@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class CrucifixCircleTest {
    private static final Map<UUID, CrucifixCirclePayload> CIRCLES = new HashMap<>();
    private CrucifixCircleTest() {}
    public static boolean isBanishing(ServerPlayer player) {
        var circle = CIRCLES.get(player.getUUID());
        return circle != null && circle.banishment()
                && circle.dimension().equals(player.level().dimension().location().toString())
                && player.level().getGameTime() - circle.startTick() < 100;
    }
    public static boolean banish(ServerPlayer player) {
        return banish(player, player, -1);
    }

    public static boolean banish(ServerPlayer player, ServerPlayer wielder, int requiredSlot) {
        return banish(player, wielder, requiredSlot, false);
    }

    public static boolean banishCave(ServerPlayer player) {
        return banish(player, player, -1, true);
    }

    private static boolean banish(ServerPlayer player, ServerPlayer wielder, int requiredSlot, boolean cave) {
        if (FullHijack.active(player) || FullHijack.active(wielder)) return false;
        if (!wielder.isAlive() || wielder.isSpectator() || wielder.level() != player.level()) return false;
        if (!player.isAlive() || ReplayCompatibility.isReplayServer(player.getServer())) return false;
        if (!cave && player.isUnderWater()) return false;
        var aboveHead = net.minecraft.core.BlockPos.containing(player.getEyePosition());
        for (int offset = 1; offset <= 2; offset++) {
            if (!cave && player.level().getFluidState(aboveHead.above(offset)).is(net.minecraft.tags.FluidTags.WATER)) return false;
        }
        var previous = CIRCLES.get(player.getUUID());
        if (previous != null && previous.banishment()
                && player.level().getGameTime() - previous.startTick() < 100) return false;
        for (int slot = 0; slot < wielder.getInventory().getContainerSize(); slot++) {
            if (requiredSlot >= 0 && slot != requiredSlot) continue;
            var stack = wielder.getInventory().getItem(slot);
            if (!stack.is(RansomItems.CRUCIFIX.get())) continue;
            var position = CrucifixPlacement.find(player);
            CrucifixActions.damage(stack, wielder, 10, slot == 40
                    ? net.minecraft.world.entity.EquipmentSlot.OFFHAND : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            wielder.getInventory().setChanged();
            var circle = new CrucifixCirclePayload(player.getUUID(),
                    player.level().dimension().location().toString(), position.x, position.y,
                    position.z, player.level().getGameTime(), true, true, false,
                    wielder.getX(), wielder.getY() + 1.2, wielder.getZ());
            CIRCLES.put(player.getUUID(), circle);
            RansomAdvancements.banished(wielder);
            com.wentory.ransom_in_minecraft.network.RansomServerState.beginCrucifixSpawnCooldown(player);
            PacketDistributor.sendToAllPlayers(circle);
            player.serverLevel().playSound(null, circle.x(), circle.y(), circle.z(),
                    RansomInMinecraft.CRUCIFIX_SUCCESS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.serverLevel().playSound(null, circle.x(), circle.y() + 1.1, circle.z(),
                    RansomInMinecraft.RANSOM_BANISHMENT_SCREAM.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }
        return false;
    }
    @SubscribeEvent public static void expire(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        CIRCLES.values().removeIf(circle -> {
            if (!circle.banishment()) return false;
            var level = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(circle.dimension())));
            if (level != null && level.getGameTime() - circle.startTick() < CrucifixBanishmentTiming.LIGHT_END) return false;
            PacketDistributor.sendToAllPlayers(new CrucifixCirclePayload(circle.owner(), circle.dimension(),
                    circle.x(), circle.y(), circle.z(), circle.startTick(), false, true));
            return true;
        });
    }
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }
    private static void sync(ServerPlayer player) {
        for (var circle : CIRCLES.values()) PacketDistributor.sendToPlayer(player, circle);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CIRCLES.clear(); }
}
