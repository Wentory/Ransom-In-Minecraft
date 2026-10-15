package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class CrucifixActions {
    public static final int MOB_BANISHMENT_TICKS = 20;
    private static final String BANISH_END = "ransom_crucifix_mob_end";
    private static final java.util.Set<net.minecraft.world.entity.Mob> BANISHING = new java.util.HashSet<>();
    private CrucifixActions() {}
    public static void damage(ItemStack stack, ServerPlayer player, int amount, EquipmentSlot slot) {
        if (player.getAbilities().instabuild) return;
        stack.hurtAndBreak(amount, player, entity -> entity.broadcastBreakEvent(slot));
        player.getInventory().setChanged();
    }
    @SubscribeEvent
    public static void banishMob(AttackEntityEvent event) {
        if (!event.getEntity().getMainHandItem().is(RansomItems.CRUCIFIX.get())) return;
        if (event.getTarget() instanceof ServerPlayer victim) {
            if (!RansomServerState.isEncounterActive(victim)) return;
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer wielder) {
                rescue(wielder, victim, wielder.getInventory().selected);
            }
            return;
        }
        boolean infected = event.getTarget() instanceof Zombie zombie && InfectedZombies.isInfected(zombie)
                || event.getTarget() instanceof Creeper creeper && InfectedCreepers.isInfected(creeper);
        if (!infected) return;
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)
                || ReplayCompatibility.isReplayServer(player.getServer())) return;
        var mob = (net.minecraft.world.entity.Mob) event.getTarget();
        if (isBanishing(mob)) return;
        mob.getPersistentData().putLong(BANISH_END, mob.level().getGameTime() + MOB_BANISHMENT_TICKS);
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.setNoGravity(true);
        mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        mob.getNavigation().stop();
        mob.setTarget(null);
        if (mob instanceof Zombie zombie) InfectedZombies.releaseBanishmentVictim(zombie);
        BANISHING.add(mob);
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                RansomInMinecraft.HIJACK_CRUCIFIXED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        com.wentory.ransom_in_minecraft.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(mob,
                new com.wentory.ransom_in_minecraft.network.MobBanishmentPayload(mob.getUUID(), mob.level().getGameTime()));
        damage(player.getMainHandItem(), player, 1, EquipmentSlot.MAINHAND);
    }

    @SubscribeEvent
    public static void useOnPlayer(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ServerPlayer victim)
                || !(event.getEntity() instanceof ServerPlayer wielder)
                || !event.getItemStack().is(RansomItems.CRUCIFIX.get())
                || !RansomServerState.isEncounterActive(victim)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        rescue(wielder, victim, event.getHand() == InteractionHand.OFF_HAND
                ? 40 : wielder.getInventory().selected);
    }

    private static void rescue(ServerPlayer wielder, ServerPlayer victim, int slot) {
        if (ReplayCompatibility.isReplayServer(wielder.getServer())
                || !RansomServerState.isEncounterActive(victim)) return;
        if (CrucifixCircleTest.banish(victim, wielder, slot)) {
            RansomServerState.finishCrucifixRescue(victim);
        }
    }
    public static boolean isBanishing(net.minecraft.world.entity.Mob mob) {
        return mob.getPersistentData().contains(BANISH_END);
    }
    @SubscribeEvent public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.Mob mob
                && isBanishing(mob)) BANISHING.add(mob);
    }
    @SubscribeEvent public static void tracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof net.minecraft.world.entity.Mob mob
                && isBanishing(mob)) com.wentory.ransom_in_minecraft.network.PacketDistributor.sendToPlayer(player,
                new com.wentory.ransom_in_minecraft.network.MobBanishmentPayload(mob.getUUID(),
                        mob.getPersistentData().getLong(BANISH_END) - MOB_BANISHMENT_TICKS));
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (ReplayCompatibility.isReplayServer(event.getServer())) return;
        BANISHING.removeIf(mob -> {
            if (mob.isRemoved()) return true;
            if (mob.level().getGameTime() < mob.getPersistentData().getLong(BANISH_END)) {
                mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                return false;
            }
            mob.discard();
            return true;
        });
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        BANISHING.clear();
    }
}
