package com.wentory.ransom_in_minecraft;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

/** Server-owned movement and attacks while the player fights for control. */
final class FullHijackBody {
    private static final double SEARCH_RADIUS = 16;
    private static final double WALK_SPEED = .13;
    private LivingEntity target;
    private Vec3 destination;
    private long nextDecision, nextAttack;
    private boolean attacking;
    private MobEffectInstance previousResistance, previousStrength;
    private long started;
    private boolean initialized;
    private final HijackedTerrain terrain = new HijackedTerrain();
    private final HijackedPathfinding navigation = new HijackedPathfinding();
    private boolean eating;
    private long nextMeal, chargedAt = -1, nextShot;
    private Vec3 steering = Vec3.ZERO;
    private boolean jumping;
    private float lookYaw, lookPitch;
    private BlockPos pillarBase;
    private long pillarStarted;

    void tick(ServerPlayer player) {
        attacking = true;
        steering = Vec3.ZERO;
        jumping = player.isInWater();
        lookYaw = player.getYRot();
        lookPitch = player.getXRot();
        try { control(player); }
        finally {
            attacking = false;
            com.wentory.ransom_in_minecraft.network.PacketDistributor.sendToPlayer(player,
                    new com.wentory.ransom_in_minecraft.network.HijackedBodyInput(
                            (float) steering.x, (float) steering.z, lookYaw, lookPitch, jumping));
        }
    }

    private void control(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (!initialized) {
            initialized = true;
            started = now;
            previousResistance = copy(player.getEffect(MobEffects.DAMAGE_RESISTANCE));
            previousStrength = copy(player.getEffect(MobEffects.DAMAGE_BOOST));
        }
        protect(player, MobEffects.DAMAGE_RESISTANCE, 3);
        protect(player, MobEffects.DAMAGE_BOOST, 1);
        if (target != null && !valid(player, target)) {
            target = null;
            destination = null;
            terrain.clear(player);
            navigation.invalidate();
            pillarBase = null;
        }
        if (pillarBase != null) {
            if (player.getY() >= pillarBase.getY() + .95) {
                terrain.place(player, pillarBase);
                pillarBase = null;
            } else if (now - pillarStarted > 16) {
                pillarBase = null;
            } else {
                jumping = player.onGround();
                return;
            }
        }
        if (now >= nextDecision || target != null && !valid(player, target)) {
            nextDecision = now + 20;
            LivingEntity attacker = player.getLastHurtByMob();
            target = valid(player, attacker) ? attacker : valid(player, target) ? target : player.level().getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(SEARCH_RADIUS),
                    entity -> valid(player, entity) && player.hasLineOfSight(entity) && (entity instanceof Player
                            || entity instanceof Mob && !(entity instanceof Enemy)))
                    .stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        }
        if (eating && player.isUsingItem()) return;
        eating = false;
        if (now >= nextMeal && player.getFoodData().getFoodLevel() <= 16 && !player.isUsingItem()) {
            int food = HijackedInventory.food(player);
            nextMeal = now + 20;
            if (food >= 0) {
                terrain.clear(player);
                HijackedInventory.equip(player, food);
                player.gameMode.useItem(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
                eating = player.isUsingItem();
                if (eating) return;
            }
        }
        if (terrain.miningPosition() != null && target != null) {
            terrain.mine(player, terrain.miningPosition());
            if (terrain.miningPosition() == null) navigation.invalidate();
            return;
        }
        if (target != null) {
            destination = target.position();
            if (!player.isUsingItem()) HijackedInventory.weapon(player);
            if (rangedAttack(player, now)) return;
            if (player.distanceToSqr(target) < 6.25 && now >= nextAttack && player.hasLineOfSight(target)) {
                nextAttack = now + 16;
                face(player, player.position());
                player.attack(target);
                player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (destination == null || player.position().distanceToSqr(destination) < 1 || now % 80 == 0) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            destination = player.position().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
        }
        if (destination == null) return;
        Vec3 waypoint = navigation.waypoint(player, destination);
        if (target != null && waypoint.equals(destination) && target.getY() - player.getY() > 1.5
                && player.onGround() && player.distanceToSqr(target) < 25 && pillar(player)) return;
        Vec3 direction = waypoint.subtract(player.position()).multiply(1, 0, 1);
        if (direction.lengthSqr() < .04) return;
        direction = direction.normalize().scale(WALK_SPEED);
        Vec3 next = player.position().add(direction);
        jumping |= waypoint.y - player.getY() > .5;
        if (!player.level().noCollision(player, player.getBoundingBox().move(direction))) {
            Vec3 step = direction.add(0, 1, 0);
            if (player.onGround() && player.level().noCollision(player, player.getBoundingBox().move(step))) next = player.position().add(step);
            else {
                if (target != null) {
                    Vec3 ahead = player.position().add(direction.normalize().scale(.85));
                    BlockPos obstacle = BlockPos.containing(ahead);
                    if (player.level().getBlockState(obstacle).getCollisionShape(player.level(), obstacle).isEmpty()) obstacle = obstacle.above();
                    if (terrain.mine(player, obstacle)) return;
                }
                // Follow an obstacle's edge instead of continuously walking into it.
                Vec3 side = new Vec3(-direction.z, 0, direction.x);
                if (!player.level().noCollision(player, player.getBoundingBox().move(side))) side = side.scale(-1);
                if (!player.level().noCollision(player, player.getBoundingBox().move(side))) { destination = null; return; }
                next = player.position().add(side);
                direction = side;
            }
        }
        // Do not deliberately walk off ledges or into lava.
        if (player.level().getFluidState(BlockPos.containing(next)).is(net.minecraft.tags.FluidTags.LAVA)) return;
        if (!player.isInWater() && !player.level().getFluidState(BlockPos.containing(next)).is(net.minecraft.tags.FluidTags.WATER)
                && player.level().noCollision(player, player.getBoundingBox().move(next.subtract(player.position())).move(0, -1.2, 0))) {
            if (target == null) return;
            BlockPos bridge = BlockPos.containing(player.position().add(direction.normalize().scale(.85))).below();
            if (!terrain.place(player, bridge)) return;
        }
        face(player, next);
    }

    private void face(ServerPlayer player, Vec3 next) {
        Vec3 direction = target == null ? next.subtract(player.position()) : target.position().subtract(player.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = target == null ? 0 : (float) -Math.toDegrees(Math.atan2(
                target.getEyeY() - player.getEyeY(), Math.sqrt(player.distanceToSqr(target))));
        steering = next.subtract(player.position()).multiply(1, 0, 1).scale(1 / WALK_SPEED);
        jumping |= next.y - player.getY() > .5 || player.isInWater();
        lookYaw = yaw;
        lookPitch = pitch;
    }

    private boolean pillar(ServerPlayer player) {
        if (!terrain.canPlace(player) || HijackedInventory.buildingBlock(player) < 0
                || !player.level().noCollision(player, player.getBoundingBox().move(0, 1, 0))) return false;
        pillarBase = player.blockPosition();
        pillarStarted = player.level().getGameTime();
        jumping = true;
        return true;
    }

    private boolean rangedAttack(ServerPlayer player, long now) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)
                || !player.hasLineOfSight(target) || chargedAt < 0 && player.getProjectile(stack).isEmpty()
                    && !CrossbowItem.isCharged(stack)) {
            if (chargedAt >= 0) player.stopUsingItem();
            chargedAt = -1;
            return false;
        }
        face(player, player.position());
        if (chargedAt >= 0) {
            int duration = stack.getItem() instanceof CrossbowItem ? CrossbowItem.getChargeDuration(stack) : 20;
            if (now - chargedAt >= duration) {
                player.releaseUsingItem();
                if (stack.getItem() instanceof CrossbowItem)
                    player.gameMode.useItem(player, player.level(), stack, InteractionHand.MAIN_HAND);
                chargedAt = -1;
                nextShot = now + 20;
            }
        } else if (now >= nextShot && !player.getProjectile(stack).isEmpty()) {
            player.gameMode.useItem(player, player.level(), stack, InteractionHand.MAIN_HAND);
            if (player.isUsingItem()) chargedAt = now;
            else nextShot = now + 20;
        }
        return true;
    }

    boolean controlling() { return attacking; }

    private static boolean valid(ServerPlayer player, LivingEntity entity) {
        return entity != null && entity != player && entity.isAlive() && entity.level() == player.level()
                && !isAquatic(entity)
                && entity.getMobType() != net.minecraft.world.entity.MobType.UNDEAD
                && player.distanceToSqr(entity) <= SEARCH_RADIUS * SEARCH_RADIUS
                && (!(entity instanceof Player other) || !other.isSpectator() && !other.isCreative());
    }

    private static boolean isAquatic(LivingEntity entity) {
        return entity instanceof WaterAnimal || entity instanceof Axolotl || entity instanceof Turtle
                || entity instanceof Guardian || entity instanceof Drowned;
    }

    private static MobEffectInstance copy(MobEffectInstance effect) {
        return effect == null ? null : new MobEffectInstance(effect);
    }

    private static void protect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() <= 10)
            player.addEffect(new MobEffectInstance(effect, 30, amplifier, false, false, false));
    }

    void clear(ServerPlayer player) {
        terrain.clear(player);
        if (eating || chargedAt >= 0) player.stopUsingItem();
        if (!initialized) return;
        restore(player, MobEffects.DAMAGE_RESISTANCE, previousResistance, 3);
        restore(player, MobEffects.DAMAGE_BOOST, previousStrength, 1);
    }

    private void restore(ServerPlayer player, net.minecraft.world.effect.MobEffect effect,
                         MobEffectInstance previous, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() > 30) return;
        player.removeEffect(effect);
        if (previous != null) {
            int remaining = previous.isInfiniteDuration() ? -1 : previous.getDuration() - (int) (player.level().getGameTime() - started);
            if (remaining > 0 || remaining == -1) player.addEffect(new MobEffectInstance(effect, remaining,
                    previous.getAmplifier(), previous.isAmbient(), previous.isVisible(), previous.showIcon()));
        }
    }
}
