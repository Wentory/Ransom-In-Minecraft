package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.InfectedZombiePayload;
import com.wentory.ransom_in_minecraft.network.WormVictimPayload;
import com.wentory.ransom_in_minecraft.network.ZombieStopPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import com.wentory.ransom_in_minecraft.network.PacketDistributor;

import java.util.Comparator;
import java.util.UUID;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class InfectedZombies {
    private static final String INFECTED = "ransom_infected_zombie";
    private static final String NEXT_GROWL = "ransom_zombie_next_growl";
    private static final String NEXT_STEP = "ransom_zombie_next_step";
    private static final String WORM_TARGET = "ransom_worm_target";
    private static final String WORM_SOURCE = "ransom_worm_source";
    private static final String WORM_END = "ransom_worm_end";
    private static final String WORM_PREVIOUS_AI = "ransom_worm_previous_ai";
    private static final String NEXT_ATTACK = "ransom_worm_next_attack";
    private static final String NEXT_STOP = "ransom_worm_next_stop";
    private static final String STOP_END = "ransom_worm_stop_end";
    private static final int INFECTION_TICKS = 30 * 20;
    private static final ResourceLocation SPEED_ID = new ResourceLocation(
            RansomInMinecraft.MODID, "infected_zombie_speed");
    private static final ResourceLocation STEP_ID = new ResourceLocation(
            RansomInMinecraft.MODID, "infected_zombie_step_height");

    private InfectedZombies() {}

    public static boolean isInfected(Zombie zombie) {
        return zombie.getPersistentData().getBoolean(INFECTED);
    }

    public static boolean isBeingInfected(Mob mob) { return mob.getPersistentData().hasUUID(WORM_SOURCE); }

    private static boolean canInfect(Mob mob) {
        return !isBeingInfected(mob) && (isSupportedZombie(mob) && mob instanceof Zombie zombie && !isInfected(zombie)
                || mob instanceof Creeper creeper && !InfectedCreepers.isInfected(creeper));
    }

    public static void infect(Zombie zombie) {
        if (isInfected(zombie)) return;
        releaseVictim(zombie);
        zombie.getPersistentData().putBoolean(INFECTED, true);
        configureInfected(zombie);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(zombie, new InfectedZombiePayload(zombie.getUUID()));
    }

    public static boolean isSupportedZombie(Mob mob) {
        return mob.getType() == EntityType.ZOMBIE || mob.getType() == EntityType.DROWNED;
    }

    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Zombie zombie
                && isInfected(zombie) && event.getOutcome() instanceof Drowned drowned) infect(drowned);
    }

    public static int spawnTestDrowned(ServerPlayer player) {
        return spawnTestZombie(player, EntityType.DROWNED);
    }

    public static int spawnTestZombie(ServerPlayer player) {
        return spawnTestZombie(player, EntityType.ZOMBIE);
    }

    private static int spawnTestZombie(ServerPlayer player, EntityType<? extends Zombie> type) {
        Zombie zombie = type.create(player.serverLevel());
        if (zombie == null) return 0;
        Vec3 look = player.getLookAngle();
        double horizontal = Math.hypot(look.x, look.z);
        double forwardX = horizontal > 0.001D ? look.x / horizontal : 0.0D;
        double forwardZ = horizontal > 0.001D ? look.z / horizontal : 1.0D;
        zombie.moveTo(player.getX() + forwardX * 3.0D, player.getY(),
                player.getZ() + forwardZ * 3.0D, player.getYRot() + 180.0F, 0.0F);
        zombie.getPersistentData().putBoolean(INFECTED, true);
        return player.serverLevel().addFreshEntity(zombie) ? 1 : 0;
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawn event) {
        if (!(event.getEntity() instanceof Zombie zombie) || !isSupportedZombie(zombie)
                || event.getSpawnType() != MobSpawnType.NATURAL) return;
        int chance = zombie instanceof Drowned ? StealerConfig.HIJACK_DROWNED_CHANCE.get()
                : StealerConfig.HIJACK_ZOMBIE_CHANCE.get();
        if (zombie.getRandom().nextFloat() * 100.0F < chance) zombie.getPersistentData().putBoolean(INFECTED, true);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        if (isBeingInfected(mob)) releaseVictim(mob);
        if (!(mob instanceof Zombie zombie)) return;
        // An interrupted session cannot leave a loaded victim frozen indefinitely.
        if (zombie.getPersistentData().hasUUID(WORM_SOURCE)) releaseVictim(zombie);
        zombie.getPersistentData().remove(WORM_TARGET);
        zombie.getPersistentData().remove(WORM_END);
        if (!isInfected(zombie)) return;
        configureInfected(zombie);
    }

    private static void configureInfected(Zombie zombie) {
        zombie.setMaxUpStep(1.0F);
        var movement = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(java.util.UUID.nameUUIDFromBytes(SPEED_ID.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            movement.addPermanentModifier(new AttributeModifier(java.util.UUID.nameUUIDFromBytes(SPEED_ID.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    SPEED_ID.toString(), 2.0D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        zombie.goalSelector.removeAllGoals(goal -> goal.getFlags().contains(Goal.Flag.MOVE));
        zombie.targetSelector.removeAllGoals(goal -> true);
        zombie.goalSelector.addGoal(7, new LookAtPlayerGoal(zombie, Player.class, 48.0F, 1.0F));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Mob mob)) return;
        if (mob instanceof Zombie zombie && isInfected(zombie)) PacketDistributor.sendToPlayer(player, new InfectedZombiePayload(zombie.getUUID()));
        long remaining = mob.getPersistentData().getLong(STOP_END) - mob.level().getGameTime();
        if (remaining > 0) PacketDistributor.sendToPlayer(player, new ZombieStopPayload(mob.getUUID(), (int) remaining));
        if (isBeingInfected(mob)) {
            PacketDistributor.sendToPlayer(player, new WormVictimPayload(mob.getUUID(), true));
        }
    }

    @SubscribeEvent
    public static void onTick(LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        if (isBeingInfected(mob)) {
            tickVictim(mob);
            return;
        }
        if (!(mob instanceof Zombie zombie)) return;
        if (!zombie.isAlive() || !isInfected(zombie)) return;
        tickWorm(zombie);
        if (zombie instanceof Drowned) return;
        long now = zombie.level().getGameTime();
        double dx = zombie.getX() - zombie.xo;
        double dz = zombie.getZ() - zombie.zo;
        if (zombie.onGround() && dx * dx + dz * dz > 0.0001D
                && now >= zombie.getPersistentData().getLong(STOP_END)
                && now >= zombie.getPersistentData().getLong(NEXT_STEP)) {
            zombie.playSound(RansomInMinecraft.ZOMBIE_STEP.get(), 0.65F, soundPitch(zombie));
            int delay = 3 + zombie.getRandom().nextInt(4);
            zombie.getPersistentData().putLong(NEXT_STEP, now + delay);
        }
        if (now < zombie.getPersistentData().getLong(NEXT_GROWL)) return;
        zombie.playSound(RansomInMinecraft.ZOMBIE_SAY.get(), 0.48F, soundPitch(zombie));
        zombie.getPersistentData().putLong(NEXT_GROWL, now + 60 + zombie.getRandom().nextInt(81));
    }

    private static void tickWorm(Zombie worm) {
        ServerLevel level = (ServerLevel) worm.level();
        long now = level.getGameTime();
        if (now < worm.getPersistentData().getLong(STOP_END)) {
            freezeMovement(worm);
            return;
        }
        if (worm.getPersistentData().hasUUID(WORM_TARGET)) {
            var entity = level.getEntity(worm.getPersistentData().getUUID(WORM_TARGET));
            if (!(entity instanceof Mob victim) || !victim.isAlive()
                    || !victim.getPersistentData().hasUUID(WORM_SOURCE)
                    || !victim.getPersistentData().getUUID(WORM_SOURCE).equals(worm.getUUID())
                    || worm.hurtTime > 0 || nearbyPlayer(level, worm, 10.0D) != null) {
                abortInfection(worm, entity instanceof Mob victim ? victim : null);
                return;
            }
            worm.getNavigation().stop();
            worm.setDeltaMovement(0.0D, worm.getDeltaMovement().y, 0.0D);
            worm.getLookControl().setLookAt(victim, 180.0F, 180.0F);
            if (now >= worm.getPersistentData().getLong(WORM_END)) {
                finishInfection(worm, victim);
            }
            return;
        }

        LivingEntity target = worm.getTarget();
        if (now % 10 == worm.getId() % 10 || target == null || !target.isAlive()
                || target instanceof Mob mob && !canInfect(mob)
                || worm.distanceToSqr(target) > 35.0D * 35.0D) {
            target = findTarget(level, worm);
            worm.setTarget(target);
        }
        if (target == null) {
            worm.getNavigation().stop();
            worm.setDeltaMovement(0.0D, worm.getDeltaMovement().y, 0.0D);
            return;
        }
        if (target instanceof Player) {
            if (!worm.getPersistentData().contains(NEXT_STOP)) {
                worm.getPersistentData().putLong(NEXT_STOP, now + 80 + worm.getRandom().nextInt(81));
            } else if (now >= worm.getPersistentData().getLong(NEXT_STOP)) {
                startStop(worm);
                return;
            }
        }
        worm.getLookControl().setLookAt(target, 180.0F, 180.0F);
        if (target instanceof Mob victim && canInfect(victim)
                && !victim.getPersistentData().hasUUID(WORM_SOURCE)
                && worm.distanceToSqr(victim) <= 2.25D * 2.25D && worm.hurtTime == 0
                && nearbyPlayer(level, worm, 10.0D) == null) {
            startInfection(worm, victim, now);
            return;
        }
        if (worm instanceof Drowned drowned && target instanceof Player
                && drowned.getMainHandItem().is(Items.TRIDENT) && worm.getSensing().hasLineOfSight(target)
                && now >= worm.getPersistentData().getLong(NEXT_ATTACK)) {
            drowned.performRangedAttack(target, 1.0F);
            worm.getPersistentData().putLong(NEXT_ATTACK, now + 40);
        }
        if (worm.distanceToSqr(target) > 2.25D * 2.25D) {
            if (now % 10 == worm.getId() % 10 || worm.getNavigation().isDone()) {
                worm.getNavigation().moveTo(target, 1.0D);
            }
        } else {
            worm.getNavigation().stop();
            if (target instanceof Player && now >= worm.getPersistentData().getLong(NEXT_ATTACK)) {
                worm.doHurtTarget(target);
                worm.getPersistentData().putLong(NEXT_ATTACK, now + 20);
            }
        }
    }

    private static LivingEntity findTarget(ServerLevel level, Zombie worm) {
        Player player = level.getEntitiesOfClass(Player.class, worm.getBoundingBox().inflate(35.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator() && !candidate.isCreative()
                        && worm.getSensing().hasLineOfSight(candidate))
                .stream().min(Comparator.comparingDouble(worm::distanceToSqr)).orElse(null);
        if (player != null) return player;
        return level.getEntitiesOfClass(Monster.class, worm.getBoundingBox().inflate(35.0D),
                candidate -> candidate != worm && candidate.isAlive()
                        && canInfect(candidate)
                        && !candidate.getPersistentData().hasUUID(WORM_SOURCE)
                        && worm.getSensing().hasLineOfSight(candidate))
                .stream().min(Comparator.comparingDouble(worm::distanceToSqr)).orElse(null);
    }

    private static Player nearbyPlayer(ServerLevel level, Zombie zombie, double distance) {
        return level.getEntitiesOfClass(Player.class, zombie.getBoundingBox().inflate(distance),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
                        && zombie.distanceToSqr(player) <= distance * distance)
                .stream().findFirst().orElse(null);
    }

    private static void startInfection(Zombie worm, Mob victim, long now) {
        worm.getNavigation().stop();
        worm.setTarget(null);
        worm.getPersistentData().putUUID(WORM_TARGET, victim.getUUID());
        worm.getPersistentData().putLong(WORM_END, now + INFECTION_TICKS);
        victim.getNavigation().stop();
        victim.setTarget(null);
        victim.getPersistentData().putBoolean(WORM_PREVIOUS_AI, victim.isNoAi());
        victim.getPersistentData().putUUID(WORM_SOURCE, worm.getUUID());
        victim.setNoAi(true);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(victim,
                new WormVictimPayload(victim.getUUID(), true));
    }

    private static void tickVictim(Mob victim) {
        ServerLevel level = (ServerLevel) victim.level();
        var entity = level.getEntity(victim.getPersistentData().getUUID(WORM_SOURCE));
        if (!victim.isAlive() || !(entity instanceof Zombie worm) || !worm.isAlive()
                || !worm.getPersistentData().hasUUID(WORM_TARGET)
                || !worm.getPersistentData().getUUID(WORM_TARGET).equals(victim.getUUID())) {
            releaseVictim(victim);
            return;
        }
        victim.setDeltaMovement(0.0D, victim.getDeltaMovement().y, 0.0D);
        if (victim instanceof Creeper creeper) creeper.setSwellDir(-1);
    }

    private static void finishInfection(Zombie worm, Mob victim) {
        releaseVictim(victim);
        worm.getPersistentData().remove(WORM_TARGET);
        worm.getPersistentData().remove(WORM_END);
        if (victim instanceof Zombie zombie) infect(zombie);
        else if (victim instanceof Creeper creeper) InfectedCreepers.infect(creeper);
    }

    private static void abortInfection(Zombie worm, Mob victim) {
        worm.getPersistentData().remove(WORM_TARGET);
        worm.getPersistentData().remove(WORM_END);
        if (victim != null && victim.getPersistentData().hasUUID(WORM_SOURCE)
                && victim.getPersistentData().getUUID(WORM_SOURCE).equals(worm.getUUID())) {
            releaseVictim(victim);
        }
        worm.setTarget(null);
    }

    public static void releaseVictim(Mob victim) {
        if (!victim.getPersistentData().hasUUID(WORM_SOURCE)) return;
        victim.setNoAi(victim.getPersistentData().getBoolean(WORM_PREVIOUS_AI));
        victim.getPersistentData().remove(WORM_SOURCE);
        victim.getPersistentData().remove(WORM_PREVIOUS_AI);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(victim,
                new WormVictimPayload(victim.getUUID(), false));
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.level().isClientSide()
                || !zombie.isAlive() || !isInfected(zombie) || event.getAmount() <= 0) return;
        if (zombie.getPersistentData().hasUUID(WORM_TARGET)) {
            var victim = ((ServerLevel) zombie.level()).getEntity(zombie.getPersistentData().getUUID(WORM_TARGET));
            abortInfection(zombie, victim instanceof Mob mob ? mob : null);
        }
        startStop(zombie);
    }

    public static boolean isStopped(Zombie zombie) {
        return zombie.level().getGameTime() < zombie.getPersistentData().getLong(STOP_END);
    }

    public static boolean isInfecting(Zombie zombie) {
        return zombie.getPersistentData().hasUUID(WORM_TARGET);
    }

    private static void freezeMovement(Zombie zombie) {
        zombie.getNavigation().stop();
        zombie.setDeltaMovement(0.0D, zombie.getDeltaMovement().y, 0.0D);
        zombie.setZza(0);
        zombie.setXxa(0);
    }

    private static void startStop(Zombie zombie) {
        long now = zombie.level().getGameTime();
        int duration = 20 + zombie.getRandom().nextInt(21);
        zombie.getPersistentData().putLong(STOP_END, now + duration);
        zombie.getPersistentData().putLong(NEXT_STOP, now + duration + 80 + zombie.getRandom().nextInt(81));
        freezeMovement(zombie);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(zombie, new ZombieStopPayload(zombie.getUUID(), duration));
        zombie.level().playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
                RansomInMinecraft.ZOMBIE_STOP.get(), SoundSource.HOSTILE, 1.0F, soundPitch(zombie));
    }

    public static float soundPitch(Zombie zombie) {
        return 1.0F + (zombie.getRandom().nextFloat() - zombie.getRandom().nextFloat()) * 0.2F;
    }
}
