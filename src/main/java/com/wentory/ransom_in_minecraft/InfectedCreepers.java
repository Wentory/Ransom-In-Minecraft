package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.WormCreeperPayload;
import com.wentory.ransom_in_minecraft.network.WormCloudPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import com.wentory.ransom_in_minecraft.network.PacketDistributor;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public final class InfectedCreepers {
    private static final String INFECTED = "ransom_infected_creeper";
    private static final String PHASE = "ransom_creeper_phase";
    private static final String END = "ransom_creeper_rise_end";
    private static final ResourceLocation RANGE_ID = new ResourceLocation(
            RansomInMinecraft.MODID, "infected_creeper_detection_range");
    private InfectedCreepers() {}

    public static boolean isInfected(Creeper creeper) { return creeper.getPersistentData().getBoolean(INFECTED); }
    public static int phase(Creeper creeper) { return creeper.getPersistentData().getInt(PHASE); }

    public static void infect(Creeper creeper) {
        if (isInfected(creeper)) return;
        InfectedZombies.releaseVictim(creeper);
        creeper.getPersistentData().putBoolean(INFECTED, true);
        configure(creeper);
        sync(creeper);
    }

    private static void configure(Creeper creeper) {
        creeper.goalSelector.removeAllGoals(goal -> true);
        creeper.targetSelector.removeAllGoals(goal -> true);
        var range = creeper.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null && range.getModifier(java.util.UUID.nameUUIDFromBytes(RANGE_ID.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))) == null) {
            range.addPermanentModifier(new AttributeModifier(java.util.UUID.nameUUIDFromBytes(RANGE_ID.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    RANGE_ID.toString(), Math.max(0.0D, 40.0D - range.getValue()), AttributeModifier.Operation.ADDITION));
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
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(creeper,
                new WormCreeperPayload(creeper.getUUID(), phase(creeper)));
    }

    @SubscribeEvent public static void spawn(FinalizeSpawn event) {
        if (event.getEntity() instanceof Creeper creeper && event.getSpawnType() == MobSpawnType.NATURAL
                && creeper.getRandom().nextFloat() * 100.0F < StealerConfig.HIJACK_CREEPER_CHANCE.get()) creeper.getPersistentData().putBoolean(INFECTED, true);
    }

    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Creeper creeper && isInfected(creeper)) {
            creeper.getPersistentData().putInt(PHASE, 0);
            creeper.setNoGravity(false);
            configure(creeper);
        }
    }

    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTarget() instanceof Creeper creeper && isInfected(creeper)) {
            PacketDistributor.sendToPlayer(player, new WormCreeperPayload(creeper.getUUID(), phase(creeper)));
        }
    }

    @SubscribeEvent public static void tick(LivingTickEvent event) {
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
                creeper.getPersistentData().putInt(PHASE, 0);
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
            creeper.getPersistentData().putInt(PHASE, 1);
            creeper.getPersistentData().putLong(END, now + 60);
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
            if (now >= creeper.getPersistentData().getLong(END)) {
                Vec3 center = creeper.getBoundingBox().getCenter();
                creeper.getPersistentData().putInt(PHASE, 2);
                creeper.refreshDimensions();
                creeper.setPos(center.x, center.y - creeper.getBbHeight() * 0.5D, center.z);
                Vec3 relative = player.getBoundingBox().getCenter().subtract(center);
                Vec3 velocity = readVector(creeper, "ransom_creeper_velocity");
                double intercept = interceptionTime(relative, velocity);
                Vec3 direction = relative.add(velocity.scale(intercept)).normalize();
                storeVector(creeper, "ransom_creeper_flight", direction);
                creeper.getPersistentData().putLong("ransom_creeper_flight_end", now + 200);
                creeper.setDeltaMovement(direction);
                creeper.playSound(RansomInMinecraft.CREEPER_CHARGE.get(), 1.0F,
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
                || creeper.level().getGameTime() >= creeper.getPersistentData().getLong("ransom_creeper_flight_end")) {
            explode(creeper);
        } else {
            long now = creeper.level().getGameTime();
            if (now >= creeper.getPersistentData().getLong("ransom_creeper_next_trail_sound")) {
                creeper.playSound(RansomInMinecraft.CREEPER_TRAIL.get(), 1.0F,
                        1.0F + (creeper.getRandom().nextFloat() - creeper.getRandom().nextFloat()) * 0.2F);
                creeper.getPersistentData().putLong("ransom_creeper_next_trail_sound", now + 3);
            }
            creeper.setDeltaMovement(readVector(creeper, "ransom_creeper_flight"));
            creeper.hasImpulse = true;
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
        creeper.getPersistentData().putDouble(key + "_x", vector.x);
        creeper.getPersistentData().putDouble(key + "_y", vector.y);
        creeper.getPersistentData().putDouble(key + "_z", vector.z);
    }

    private static Vec3 readVector(Creeper creeper, String key) {
        return new Vec3(creeper.getPersistentData().getDouble(key + "_x"),
                creeper.getPersistentData().getDouble(key + "_y"), creeper.getPersistentData().getDouble(key + "_z"));
    }

    private static void explode(Creeper creeper) {
        Level level = creeper.level();
        Vec3 position = creeper.position();
        double blastRadius = creeper.isPowered() ? 12.0D : 6.0D;
        for (Player player : level.getEntitiesOfClass(Player.class,
                creeper.getBoundingBox().inflate(blastRadius), player -> player.isAlive() && !player.isSpectator())) {
            if (player instanceof ServerPlayer serverPlayer && player.distanceToSqr(position) <= blastRadius * blastRadius
                    && Explosion.getSeenPercent(position, player) > 0) WormInfection.add(serverPlayer, 25);
        }
        level.explode(creeper, position.x, position.y, position.z,
                creeper.isPowered() ? 6.0F : 3.0F, false, Level.ExplosionInteraction.NONE);
        ServerLevel serverLevel = (ServerLevel) level;
        AABB bounds = new AABB(position, position).inflate(3.0D);
        for (Monster mob : level.getEntitiesOfClass(Monster.class, bounds,
                mob -> mob != creeper && mob.isAlive() && mob.position().distanceToSqr(position) <= 9.0D)) {
            if (mob instanceof Zombie zombie && InfectedZombies.isSupportedZombie(zombie)) InfectedZombies.infect(zombie);
            else if (mob instanceof Creeper other) infect(other);
        }
        PacketDistributor.sendToPlayersInDimension(serverLevel, new WormCloudPayload(
                level.dimension().location().toString(), position.x, position.y, position.z, 12));
        creeper.discard();
    }

    public static int spawnTestCreeper(ServerPlayer player) {
        Creeper creeper = EntityType.CREEPER.create(player.serverLevel());
        if (creeper == null) return 0;
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize().scale(3.0D);
        creeper.moveTo(player.getX() + forward.x, player.getY(), player.getZ() + forward.z,
                player.getYRot() + 180.0F, 0);
        creeper.getPersistentData().putBoolean(INFECTED, true);
        return player.serverLevel().addFreshEntity(creeper) ? 1 : 0;
    }
}
