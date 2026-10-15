package com.wentory.ransom_in_minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Searches for a clear 3-by-3 horizontal footprint with one block of height. */
final class CrucifixPlacement {
    private static final double HALF_WIDTH = 1.5;
    private static final double HEIGHT = 1;
    private static final int SEARCH_RADIUS = 8;

    private CrucifixPlacement() {}

    static Vec3 find(ServerPlayer player) {
        Vec3 direction = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 preferred = player.position().add(direction.scale(5)).add(0, 0.02, 0);
        Vec3 groundedPreferred = ground(player, preferred);
        if (groundedPreferred != null && clear(player, groundedPreferred)) return groundedPreferred;
        Vec3 best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        Vec3 airborne = clear(player, preferred) ? preferred : null;
        double airborneScore = airborne != null ? 0 : Double.POSITIVE_INFINITY;
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    Vec3 candidate = preferred.add(dx, dy, dz);
                    if (candidate.distanceToSqr(player.position()) > 100) continue;
                    double playerDistanceX = candidate.x - player.getX();
                    double playerDistanceZ = candidate.z - player.getZ();
                    if (playerDistanceX * playerDistanceX + playerDistanceZ * playerDistanceZ < 16) continue;
                    double score = candidate.distanceToSqr(preferred);
                    if (score < airborneScore && clear(player, candidate)) {
                        airborne = candidate;
                        airborneScore = score;
                    }
                    Vec3 grounded = ground(player, candidate);
                    if (grounded == null || grounded.distanceToSqr(player.position()) > 100) continue;
                    score = grounded.distanceToSqr(preferred);
                    if (score >= bestScore || !clear(player, grounded)) continue;
                    best = grounded;
                    bestScore = score;
                }
            }
        }
        // Lack of clearance must never prevent the Crucifix from protecting its owner.
        return best != null ? best : airborne != null ? airborne : preferred;
    }

    private static Vec3 ground(ServerPlayer player, Vec3 candidate) {
        var level = player.serverLevel();
        BlockPos column = BlockPos.containing(candidate);
        for (int y = column.getY(); y >= Math.max(level.getMinBuildHeight(), column.getY() - 6); y--) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (!level.hasChunkAt(pos)) return null;
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) continue;
            double surfaceY = y + shape.max(Direction.Axis.Y);
            Vec3 grounded = new Vec3(candidate.x, surfaceY + 0.02, candidate.z);
            // Require support across the footprint, rather than balancing on a single post.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    AABB support = new AABB(candidate.x + dx - 0.1, surfaceY - 0.08,
                            candidate.z + dz - 0.1, candidate.x + dx + 0.1, surfaceY,
                            candidate.z + dz + 0.1);
                    if (level.noCollision(support)) return null;
                }
            }
            return grounded;
        }
        return null;
    }

    private static boolean clear(ServerPlayer player, Vec3 position) {
        var level = player.serverLevel();
        AABB space = new AABB(position.x - HALF_WIDTH, position.y, position.z - HALF_WIDTH,
                position.x + HALF_WIDTH, position.y + HEIGHT, position.z + HALF_WIDTH);
        if (space.minY < level.getMinBuildHeight() || space.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(space)) return false;
        for (double x : new double[] {space.minX, space.maxX}) {
            for (double z : new double[] {space.minZ, space.maxZ}) {
                if (!level.hasChunkAt(BlockPos.containing(x, position.y, z))) return false;
            }
        }
        if (!level.noCollision(space)) return false;
        // Do not relocate the ritual behind a wall, out of the player's view.
        return level.clip(new ClipContext(player.getEyePosition(), position.add(0, HEIGHT / 2, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }
}
