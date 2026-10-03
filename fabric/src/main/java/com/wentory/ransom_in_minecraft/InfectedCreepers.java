package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.WormCreeperPayload;
import com.wentory.ransom_in_minecraft.network.WormCloudPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.EntityJoinLevelEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.FinalizeSpawnEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.PlayerEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.EntityTickEvent;
import com.wentory.ransom_in_minecraft.platform.FabricNetwork;
public final class InfectedCreepers {
    private static final String INFECTED = "ransom_infected_creeper";
    private static final String PHASE = "ransom_creeper_phase";
    private static final String END = "ransom_creeper_rise_end";
    private static final Identifier RANGE_ID = Identifier.fromNamespaceAndPath(
            RansomFabric.MODID, "infected_creeper_detection_range");
    private InfectedCreepers() {}

    public static boolean isInfected(Creeper creeper) { return com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getBooleanOr(INFECTED, false); }
    public static int phase(Creeper creeper) { return com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getIntOr(PHASE, 0); }

    public static void infect(Creeper creeper) {
        if (isInfected(creeper)) return;
        InfectedZombies.releaseVictim(creeper);
        com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putBoolean(INFECTED, true);
        configure(creeper);
        sync(creeper);
    }

    private static void configure(Creeper creeper) {
        creeper.goalSelector.removeAllGoals(goal -> true);
        creeper.targetSelector.removeAllGoals(goal -> true);
        var range = creeper.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null && !range.hasModifier(RANGE_ID)) {
            range.addPermanentModifier(new AttributeModifier(RANGE_ID,
                    Math.max(0.0D, 40.0D - range.getValue()), AttributeModifier.Operation.ADD_VALUE));
        }
        // Visibility is required to acquire a player, but the attack keeps its acquired target.
        creeper.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(creeper, Player.class, true) {
            @Override public boolean canContinueToUse() {
                return creeper.getTarget() instanceof Player player && player.isAlive()
                        && !player.isCreative() && !player.isSpectator() && player.level() == creeper.level();
            }
        });
        creeper.getNavigation().stop();
        creeper.setSwellDir(-1);
    }

    private static void sync(Creeper creeper) {
        FabricNetwork.sendToPlayersTrackingEntityAndSelf(creeper,
                new WormCreeperPayload(creeper.getUUID(), phase(creeper)));
    }

    public static void spawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof Creeper creeper && event.getSpawnType() == EntitySpawnReason.NATURAL
                && creeper.getRandom().nextFloat() * 100.0F < StealerConfig.HIJACK_CREEPER_CHANCE.get()) com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putBoolean(INFECTED, true);
    }

    public static void join(EntityJoinLevelEvent event) {
        if (com.wentory.ransom_in_minecraft.network.ReplayCompatibility.isReplayServer(event.getEntity().level().getServer())) return;
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Creeper creeper && isInfected(creeper)) {
            com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putInt(PHASE, 0);
            creeper.setNoGravity(false);
            configure(creeper);
        }
    }

    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTarget() instanceof Creeper creeper && isInfected(creeper)) {
            FabricNetwork.sendToPlayer(player, new WormCreeperPayload(creeper.getUUID(), phase(creeper)));
        }
    }

    public static void tick(EntityTickEvent.Post event) {
        if (com.wentory.ransom_in_minecraft.network.ReplayCompatibility.isReplayServer(event.getEntity().level().getServer())) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Creeper creeper) || !creeper.isAlive() || !isInfected(creeper)) return;
        if (InfectedZombies.isBeingInfected(creeper)) return;
        creeper.setSwellDir(-1);
        creeper.getNavigation().stop();
        if (phase(creeper) == 2) {
            tickFlight(creeper);
            return;
        }
        if (!(creeper.getTarget() instanceof Player player) || !player.isAlive()
                || player.isCreative() || player.isSpectator()) {
            if (phase(creeper) != 0) {
                com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putInt(PHASE, 0);
                creeper.refreshDimensions();
                creeper.setNoGravity(false);
                sync(creeper);
            }
            creeper.setDeltaMovement(0, creeper.getDeltaMovement().y, 0);
            return;
        }
        creeper.getLookControl().setLookAt(player, 180.0F, 180.0F);
        long now = creeper.level().getGameTime();
        if (phase(creeper) == 0) {
            com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putInt(PHASE, 1);
            com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putLong(END, now + 60);
            storeVector(creeper, "ransom_creeper_sample", player.position());
            storeVector(creeper, "ransom_creeper_velocity", Vec3.ZERO);
            creeper.setNoGravity(true);
            sync(creeper);
        }
        if (phase(creeper) == 1) {
            Vec3 movement = player.position().subtract(readVector(creeper, "ransom_creeper_sample"));
            if (movement.lengthSqr() > 4.0D) movement = Vec3.ZERO;
            storeVector(creeper, "ransom_creeper_velocity",
                    readVector(creeper, "ransom_creeper_velocity").lerp(movement, 0.5D));
            storeVector(creeper, "ransom_creeper_sample", player.position());
            creeper.setDeltaMovement(0, 0.1D, 0);
            if (now >= com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getLongOr(END, 0)) {
                Vec3 center = creeper.getBoundingBox().getCenter();
                com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putInt(PHASE, 2);
                creeper.refreshDimensions();
                creeper.setPos(center.x, center.y - creeper.getBbHeight() * 0.5D, center.z);
                Vec3 relative = player.getBoundingBox().getCenter().subtract(center);
                Vec3 velocity = readVector(creeper, "ransom_creeper_velocity");
                double intercept = interceptionTime(relative, velocity);
                Vec3 direction = relative.add(velocity.scale(intercept)).normalize();
                storeVector(creeper, "ransom_creeper_flight", direction);
                com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putLong("ransom_creeper_flight_end", now + 200);
                creeper.setDeltaMovement(direction);
                creeper.playSound(RansomFabric.CREEPER_CHARGE.get(), 1.0F,
                        1.0F + (creeper.getRandom().nextFloat() - creeper.getRandom().nextFloat()) * 0.2F);
                sync(creeper);
            }
            return;
        }
    }

    private static void tickFlight(Creeper creeper) {
        boolean hitPlayer = !creeper.level().getEntitiesOfClass(Player.class,
                creeper.getBoundingBox().inflate(0.15D),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator()).isEmpty();
        if (hitPlayer || creeper.horizontalCollision || creeper.verticalCollision
                || creeper.level().getGameTime() >= com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getLongOr("ransom_creeper_flight_end", 0)) {
            explode(creeper);
        } else {
            long now = creeper.level().getGameTime();
            if (now >= com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getLongOr("ransom_creeper_next_trail_sound", 0)) {
                creeper.playSound(RansomFabric.CREEPER_TRAIL.get(), 1.0F,
                        1.0F + (creeper.getRandom().nextFloat() - creeper.getRandom().nextFloat()) * 0.2F);
                com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putLong("ransom_creeper_next_trail_sound", now + 3);
            }
            creeper.setDeltaMovement(readVector(creeper, "ransom_creeper_flight"));
            creeper.hurtMarked = true;
        }
    }

    private static double interceptionTime(Vec3 relative, Vec3 velocity) {
        double a = velocity.lengthSqr() - 1.0D;
        double b = 2.0D * relative.dot(velocity);
        double c = relative.lengthSqr();
        double time = Math.sqrt(c);
        if (Math.abs(a) < 1.0E-6D) {
            if (Math.abs(b) > 1.0E-6D && -c / b > 0) time = -c / b;
        } else {
            double discriminant = b * b - 4.0D * a * c;
            if (discriminant >= 0) {
                double first = (-b - Math.sqrt(discriminant)) / (2.0D * a);
                double second = (-b + Math.sqrt(discriminant)) / (2.0D * a);
                if (first > 0 && second > 0) time = Math.min(first, second);
                else if (first > 0) time = first;
                else if (second > 0) time = second;
            }
        }
        return Math.min(80.0D, time);
    }

    private static void storeVector(Creeper creeper, String key, Vec3 vector) {
        com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putDouble(key + "_x", vector.x);
        com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putDouble(key + "_y", vector.y);
        com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putDouble(key + "_z", vector.z);
    }

    private static Vec3 readVector(Creeper creeper, String key) {
        return new Vec3(com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getDoubleOr(key + "_x", 0),
                com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getDoubleOr(key + "_y", 0), com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).getDoubleOr(key + "_z", 0));
    }

    private static void explode(Creeper creeper) {
        Level level = creeper.level();
        Vec3 position = creeper.position();
        double blastRadius = creeper.isPowered() ? 12.0D : 6.0D;
        for (Player player : level.getEntitiesOfClass(Player.class,
                creeper.getBoundingBox().inflate(blastRadius), player -> player.isAlive() && !player.isSpectator())) {
            if (player instanceof ServerPlayer serverPlayer && player.distanceToSqr(position) <= blastRadius * blastRadius
                    && net.minecraft.world.level.ServerExplosion.getSeenPercent(position, player) > 0) WormInfection.add(serverPlayer, 25);
        }
        level.explode(creeper, Explosion.getDefaultDamageSource(level, creeper), null,
                position.x, position.y, position.z, creeper.isPowered() ? 6.0F : 3.0F,
                false, Level.ExplosionInteraction.NONE,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, net.minecraft.util.random.WeightedList.<net.minecraft.core.particles.ExplosionParticleInfo>builder().build(), net.minecraft.core.Holder.direct(RansomFabric.CREEPER_EXPLODE.get()));
        ServerLevel serverLevel = (ServerLevel) level;
        AABB bounds = new AABB(position, position).inflate(3.0D);
        for (Monster mob : level.getEntitiesOfClass(Monster.class, bounds,
                mob -> mob != creeper && mob.isAlive() && mob.position().distanceToSqr(position) <= 9.0D)) {
            if (mob instanceof Zombie zombie && InfectedZombies.isSupportedZombie(zombie)) InfectedZombies.infect(zombie);
            else if (mob instanceof Creeper other) infect(other);
        }
        FabricNetwork.sendToPlayersInDimension(serverLevel, new WormCloudPayload(
                level.dimension().identifier().toString(), position.x, position.y, position.z, 12));
        creeper.discard();
    }

    public static int spawnTestCreeper(ServerPlayer player) {
        Creeper creeper = net.minecraft.world.entity.EntityTypes.CREEPER.create(player.level(), EntitySpawnReason.COMMAND);
        if (creeper == null) return 0;
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize().scale(3.0D);
        creeper.snapTo(player.getX() + forward.x, player.getY(), player.getZ() + forward.z,
                player.getYRot() + 180.0F, 0);
        com.wentory.ransom_in_minecraft.platform.PersistentData.of(creeper).putBoolean(INFECTED, true);
        return player.level().addFreshEntity(creeper) ? 1 : 0;
    }
}
