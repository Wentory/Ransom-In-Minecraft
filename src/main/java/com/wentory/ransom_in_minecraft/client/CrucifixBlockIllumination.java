package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.CrucifixBanishmentTiming;
import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Additive blue tint on real block-model faces; no light disk or replacement blocks. */
final class CrucifixBlockIllumination {
    private static final int RADIUS = 20;
    private static final int REFRESH_TICKS = 10;
    private static final int LIGHT_START = 66;
    private static final int DESCENT_START = 70;
    private final List<Surface> surfaces = new ArrayList<>();
    private final RandomSource random = RandomSource.create(0);
    private long refreshed = Long.MIN_VALUE;
    private BlockPos emittingChest;
    private BlockPos emittingPartner;

    void render(CrucifixCirclePayload circle, float age, PoseStack pose,
                MultiBufferSource.BufferSource buffers, Vec3 camera) {
        if (circle.chest() ? age >= CrucifixBanishmentTiming.VISUAL_END
                : age <= LIGHT_START || age >= CrucifixBanishmentTiming.LIGHT_END) return;
        float intensity = circle.chest() ? chestIntensity(age) : age < DESCENT_START
                ? 0.25F * Mth.clamp((age - LIGHT_START) / (DESCENT_START - LIGHT_START), 0, 1)
                : age < CrucifixBanishmentTiming.CAPTURE_END
                ? Mth.lerp((age - DESCENT_START) / (CrucifixBanishmentTiming.CAPTURE_END - DESCENT_START), 0.25F, 0.85F)
                : 0.85F * CrucifixBanishmentTiming.fade(age, CrucifixBanishmentTiming.CAPTURE_END,
                        CrucifixBanishmentTiming.LIGHT_END - CrucifixBanishmentTiming.CAPTURE_END);
        var minecraft = Minecraft.getInstance();
        long tick = minecraft.level.getGameTime();
        if (refreshed == Long.MIN_VALUE || tick - refreshed >= REFRESH_TICKS) {
            rebuild(minecraft, circle);
            refreshed = tick;
        }
        var type = RansomRenderTypes.crucifixBlockLight();
        var consumer = buffers.getBuffer(type);
        float currentRadius = (circle.chest() ? 8 : RADIUS) * Mth.clamp(intensity / 0.85F, 0, 1);
        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            for (Surface surface : surfaces) {
                if (surface.distance >= currentRadius) continue;
                float radial = 1 - surface.distance / currentRadius;
                int alpha = Math.round(255 * Math.min(1, intensity * 1.2F) * radial * radial * surface.weight);
                if (alpha == 0) continue;
                int[] data = surface.quad.getVertices();
                int stride = data.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    consumer.addVertex(pose.last().pose(),
                                    surface.pos.getX() + Float.intBitsToFloat(data[offset]),
                                    surface.pos.getY() + Float.intBitsToFloat(data[offset + 1]),
                                    surface.pos.getZ() + Float.intBitsToFloat(data[offset + 2]))
                            .setUv(Float.intBitsToFloat(data[offset + 4]), Float.intBitsToFloat(data[offset + 5]))
                            .setColor(85, 210, 255, alpha);
                }
            }
            buffers.endBatch(type);
        } finally {
            pose.popPose();
        }
    }

    private void rebuild(Minecraft minecraft, CrucifixCirclePayload circle) {
        surfaces.clear();
        BlockPos center = BlockPos.containing(circle.x(), circle.y(), circle.z());
        emittingChest = circle.chest() ? center : null;
        emittingPartner = null;
        if (circle.chest()) {
            var chestState = minecraft.level.getBlockState(center);
            if (chestState.hasProperty(ChestBlock.TYPE) && chestState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos partner = center.relative(ChestBlock.getConnectedDirection(chestState));
                if (minecraft.level.getBlockState(partner).is(chestState.getBlock())) emittingPartner = partner;
            }
        }
        Vec3 source = new Vec3(circle.x(), circle.y() + 0.35, circle.z());
        int radius = circle.chest() ? 8 : RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!minecraft.level.hasChunkAt(pos)) continue;
            double distance = Vec3.atCenterOf(pos).distanceTo(source);
            if (distance >= radius) continue;
            var state = minecraft.level.getBlockState(pos);
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            var model = minecraft.getBlockRenderer().getBlockModel(state);
            var modelData = minecraft.level.getModelDataManager().getAt(pos);
            if (modelData == null) modelData = net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
            for (var layer : model.getRenderTypes(state, random, modelData)) {
                for (Direction direction : Direction.values()) {
                    if (!Block.shouldRenderFace(state, minecraft.level, pos, direction, pos.relative(direction))) continue;
                    random.setSeed(state.getSeed(pos));
                    add(minecraft, pos, model.getQuads(state, direction, random, modelData, layer), source, (float) distance);
                }
                random.setSeed(state.getSeed(pos));
                add(minecraft, pos, model.getQuads(state, null, random, modelData, layer), source, (float) distance);
            }
        }
    }

    private void add(Minecraft minecraft, BlockPos pos, List<BakedQuad> quads, Vec3 source, float distance) {
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            double x = 0, y = 0, z = 0;
            for (int i = 0; i < 4; i++) {
                x += Float.intBitsToFloat(vertices[i * stride]);
                y += Float.intBitsToFloat(vertices[i * stride + 1]);
                z += Float.intBitsToFloat(vertices[i * stride + 2]);
            }
            Vec3 faceCenter = new Vec3(pos.getX() + x / 4, pos.getY() + y / 4, pos.getZ() + z / 4);
            Vec3 towardSource = source.subtract(faceCenter).normalize();
            // End just outside this face: its own block must not count as an obstruction.
            Vec3 endpoint = faceCenter.add(towardSource.scale(0.02));
            var hit = minecraft.level.clip(new ClipContext(source, endpoint, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, minecraft.player) {
                @Override
                public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos checkedPos) {
                    // The emitting chest must not shadow the light coming from its own interior.
                    if (checkedPos.equals(emittingChest) || checkedPos.equals(emittingPartner)) return Shapes.empty();
                    // Only a full opaque block blocks the ritual light. Glass and partial
                    // shapes (fences, slabs, stairs, etc.) let it reach surfaces behind them.
                    if (!state.isSolidRender(level, checkedPos)) return Shapes.empty();
                    return super.getBlockShape(state, level, checkedPos);
                }
            });
            if (hit.getType() != HitResult.Type.MISS) continue;
            surfaces.add(new Surface(pos.immutable(), quad, 1, distance));
        }
    }

    private record Surface(BlockPos pos, BakedQuad quad, float weight, float distance) {}

    static float chestIntensity(float age) {
        return 0.85F * Mth.clamp(age / 20, 0, 1) * CrucifixBanishmentTiming.fade(age,
                CrucifixBanishmentTiming.CAPTURE_END,
                CrucifixBanishmentTiming.VISUAL_END - CrucifixBanishmentTiming.CAPTURE_END);
    }
}
