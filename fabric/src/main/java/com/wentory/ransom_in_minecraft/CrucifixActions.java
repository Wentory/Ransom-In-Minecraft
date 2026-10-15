package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.AttackEntityEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.PlayerInteractEvent;
import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

public final class CrucifixActions {
    public static final int MOB_BANISHMENT_TICKS = 20;
    private static final String BANISH_END = "ransom_crucifix_mob_end";
    private static final java.util.Set<net.minecraft.world.entity.Mob> BANISHING = new java.util.HashSet<>();
    private CrucifixActions() {}
    public static void damage(ItemStack stack, ServerPlayer player, int amount, EquipmentSlot slot) {
        if (player.getAbilities().instabuild) return;
        stack.hurtAndBreak(amount, player, slot);
        player.getInventory().setChanged();
    }
    public static void banishMob(AttackEntityEvent event) {
        if (!event.getEntity().getMainHandItem().is(RansomItems.CRUCIFIX.get())) return;
        if (event.getTarget() instanceof ServerPlayer victim) {
            if (!RansomServerState.isEncounterActive(victim)) return;
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer wielder) {
                rescue(wielder, victim, wielder.getInventory().getSelectedSlot());
            }
            return;
        }
        boolean infected = event.getTarget() instanceof Zombie zombie && InfectedZombies.isInfected(zombie)
                || event.getTarget() instanceof Creeper creeper && InfectedCreepers.isInfected(creeper);
        if (!infected) return;
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)
                || ReplayCompatibility.isReplayServer(player.level().getServer())) return;
        var mob = (net.minecraft.world.entity.Mob) event.getTarget();
        if (isBanishing(mob)) return;
        com.wentory.ransom_in_minecraft.platform.PersistentData.of(mob).putLong(BANISH_END, mob.level().getGameTime() + MOB_BANISHMENT_TICKS);
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.setNoGravity(true);
        mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        mob.getNavigation().stop();
        mob.setTarget(null);
        if (mob instanceof Zombie zombie) InfectedZombies.releaseBanishmentVictim(zombie);
        BANISHING.add(mob);
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                RansomFabric.HIJACK_CRUCIFIXED.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        com.wentory.ransom_in_minecraft.platform.FabricNetwork.sendToPlayersTrackingEntityAndSelf(mob,
                new com.wentory.ransom_in_minecraft.network.MobBanishmentPayload(mob.getUUID(), mob.level().getGameTime()));
        damage(player.getMainHandItem(), player, 1, EquipmentSlot.MAINHAND);
    }

    public static void useOnPlayer(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ServerPlayer victim)
                || !(event.getEntity() instanceof ServerPlayer wielder)
                || !event.getItemStack().is(RansomItems.CRUCIFIX.get())
                || !RansomServerState.isEncounterActive(victim)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        rescue(wielder, victim, event.getHand() == InteractionHand.OFF_HAND
                ? 40 : wielder.getInventory().getSelectedSlot());
    }

    private static void rescue(ServerPlayer wielder, ServerPlayer victim, int slot) {
        if (ReplayCompatibility.isReplayServer(wielder.level().getServer())
                || !RansomServerState.isEncounterActive(victim)) return;
        if (CrucifixCircleTest.banish(victim, wielder, slot)) {
            RansomServerState.finishCrucifixRescue(victim);
        }
    }
    public static boolean isBanishing(net.minecraft.world.entity.Mob mob) {
        return com.wentory.ransom_in_minecraft.platform.PersistentData.of(mob).contains(BANISH_END);
    }
    public static void joined(com.wentory.ransom_in_minecraft.platform.FabricEvents.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.Mob mob
                && isBanishing(mob)) BANISHING.add(mob);
    }
    public static void tracking(com.wentory.ransom_in_minecraft.platform.FabricEvents.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof net.minecraft.world.entity.Mob mob
                && isBanishing(mob)) com.wentory.ransom_in_minecraft.platform.FabricNetwork.sendToPlayer(player,
                new com.wentory.ransom_in_minecraft.network.MobBanishmentPayload(mob.getUUID(),
                        com.wentory.ransom_in_minecraft.platform.PersistentData.of(mob).getLongOr(BANISH_END, 0L) - MOB_BANISHMENT_TICKS));
    }
    public static void tick(com.wentory.ransom_in_minecraft.platform.FabricEvents.ServerTickEvent.Post event) {
        if (ReplayCompatibility.isReplayServer(event.getServer())) return;
        BANISHING.removeIf(mob -> {
            if (mob.isRemoved()) return true;
            if (mob.level().getGameTime() < com.wentory.ransom_in_minecraft.platform.PersistentData.of(mob).getLongOr(BANISH_END, 0L)) {
                mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                return false;
            }
            mob.discard();
            return true;
        });
    }
    public static void stopped(com.wentory.ransom_in_minecraft.platform.FabricEvents.ServerStoppedEvent event) {
        BANISHING.clear();
    }
}
