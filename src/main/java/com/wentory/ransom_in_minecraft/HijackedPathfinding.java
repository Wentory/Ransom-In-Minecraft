package com.wentory.ransom_in_minecraft;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/** Vanilla A* with a player-sized search body. The search body is never added to the world. */
final class HijackedPathfinding {
    private static final int SEARCH_RADIUS = 24;
    private static final int MAX_VISITED_NODES = 2048;
    private final PathFinder finder;
    private SearchBody searchBody;
    private Path path;
    private BlockPos goal;
    private long nextSearch;
    private Vec3 previousPosition;
    private int stuckTicks;

    HijackedPathfinding() {
        WalkNodeEvaluator evaluator = new WalkNodeEvaluator();
        evaluator.setCanFloat(true);
        evaluator.setCanPassDoors(true);
        evaluator.setCanOpenDoors(false);
        finder = new PathFinder(evaluator, MAX_VISITED_NODES);
    }

    Vec3 waypoint(ServerPlayer player, Vec3 destination) {
        long now = player.level().getGameTime();
        BlockPos requested = BlockPos.containing(destination);
        if (previousPosition != null && previousPosition.distanceToSqr(player.position()) < .0004) stuckTicks++;
        else stuckTicks = 0;
        previousPosition = player.position();
        boolean goalMoved = goal == null || goal.distSqr(requested) > 4;
        if (now >= nextSearch || goalMoved || stuckTicks >= 20) {
            nextSearch = now + 20;
            stuckTicks = 0;
            goal = requested;
            if (searchBody == null || searchBody.level() != player.level()) searchBody = new SearchBody(player);
            searchBody.setPos(player.position());
            searchBody.setOnGround(player.onGround());
            BlockPos origin = player.blockPosition();
            PathNavigationRegion region = new PathNavigationRegion(player.level(),
                    origin.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                    origin.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS));
            path = finder.findPath(region, searchBody, Set.of(requested), SEARCH_RADIUS, 1, 1);
        }
        while (path != null && !path.isDone()) {
            Vec3 next = path.getNextEntityPos(player);
            double dx = next.x - player.getX(), dz = next.z - player.getZ();
            if (dx * dx + dz * dz < .16 && Math.abs(next.y - player.getY()) < 1.2) path.advance();
            else return next;
        }
        // Only after the navigation search fails to reach the goal may terrain actions help.
        return destination;
    }

    void invalidate() { nextSearch = 0; path = null; goal = null; }

    private static final class SearchBody extends Zombie {
        private final ServerPlayer player;
        SearchBody(ServerPlayer player) {
            super(player.level());
            this.player = player;
            refreshDimensions();
            setMaxUpStep(player.maxUpStep());
            setPathfindingMalus(BlockPathTypes.WATER, 2);
            setPathfindingMalus(BlockPathTypes.LAVA, -1);
        }
        @Override public boolean isInWater() { return player != null && player.isInWater(); }
        @Override public EntityDimensions getDimensions(Pose pose) {
            return player == null ? super.getDimensions(pose) : player.getDimensions(Pose.STANDING);
        }
    }
}
