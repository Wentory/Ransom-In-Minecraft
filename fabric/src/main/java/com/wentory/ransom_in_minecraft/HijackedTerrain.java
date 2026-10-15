package com.wentory.ransom_in_minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Uses normal player breaking/placing APIs; never directly replaces terrain. */
final class HijackedTerrain {
    private BlockPos mining;
    private float damage;
    private long nextPlacement;

    BlockPos miningPosition() { return mining; }

    boolean canPlace(ServerPlayer player) { return player.level().getGameTime() >= nextPlacement; }

    boolean mine(ServerPlayer player, BlockPos pos) {
        var level = player.level();
        var state = level.getBlockState(pos);
        if (!allowed(player, pos) || state.isAir() || state.getDestroySpeed(level, pos) < 0
                || state.getCollisionShape(level, pos).isEmpty()) {
            clear(player);
            return false;
        }
        if (!pos.equals(mining)) {
            clear(player);
            mining = pos.immutable();
        }
        HijackedInventory.tool(player, state);
        damage += state.getDestroyProgress(player, level, pos);
        level.destroyBlockProgress(player.getId(), pos, Math.min(9, (int) (damage * 10)));
        if (player.tickCount % 5 == 0) player.swing(InteractionHand.MAIN_HAND);
        if (damage >= 1) {
            player.gameMode.destroyBlock(pos);
            clear(player);
        }
        return true;
    }

    boolean place(ServerPlayer player, BlockPos pos) {
        if (player.level().getGameTime() < nextPlacement || !allowed(player, pos)
                || !player.level().getBlockState(pos).canBeReplaced()) return false;
        int slot = HijackedInventory.buildingBlock(player);
        if (slot < 0) return false;
        for (Direction face : Direction.values()) {
            BlockPos support = pos.relative(face.getOpposite());
            var state = player.level().getBlockState(support);
            if (state.hasBlockEntity() || !state.isFaceSturdy(player.level(), support, face) || !allowed(player, support)) continue;
            HijackedInventory.equip(player, slot);
            Vec3 hit = Vec3.atCenterOf(support).add(face.getUnitVec3().scale(.5));
            player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit, face, support, false));
            if (!player.level().getBlockState(pos).canBeReplaced()) {
                player.swing(InteractionHand.MAIN_HAND);
                nextPlacement = player.level().getGameTime() + 8;
                return true;
            }
        }
        return false;
    }

    private static boolean allowed(ServerPlayer player, BlockPos pos) {
        return player.mayBuild() && player.level().mayInteract(player, pos)
                && player.level().getWorldBorder().isWithinBounds(pos)
                && !player.level().isOutsideBuildHeight(pos)
                && player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= 20.25;
    }

    void clear(ServerPlayer player) {
        if (mining != null) player.level().destroyBlockProgress(player.getId(), mining, -1);
        mining = null;
        damage = 0;
    }
}
