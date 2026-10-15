package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.network.CrucifixCirclePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import java.time.LocalDate;

/** Additive textured light on the chest's own geometry, without a duplicate silhouette. */
final class CrucifixChestGlow {
    private static ModelPart single, left, right;

    private CrucifixChestGlow() {}

    static void clear() {
        single = left = right = null;
    }

    static void render(CrucifixCirclePayload circle, float age, PoseStack pose,
                       DeferredBuffers buffers, Vec3 camera, float partial) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = BlockPos.containing(circle.x(), circle.y(), circle.z());
        if (!(minecraft.level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        var state = chest.getBlockState();
        if (!state.hasProperty(ChestBlock.TYPE)) return;
        if (single == null) {
            single = minecraft.getEntityModels().bakeLayer(ModelLayers.CHEST);
            left = minecraft.getEntityModels().bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
            right = minecraft.getEntityModels().bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        }
        ModelPart model = switch (state.getValue(ChestBlock.TYPE)) {
            case LEFT -> left;
            case RIGHT -> right;
            default -> single;
        };
        float openness = chest.getOpenNess(partial);
        float angle = -(1 - (float) Math.pow(1 - openness, 3)) * (float) Math.PI / 2;
        model.getChild("lid").xRot = angle;
        model.getChild("lock").xRot = angle;
        int alpha = Math.round(235 * CrucifixBlockIllumination.chestIntensity(age) / 0.85F);
        if (alpha <= 0) return;
        pose.pushPose();
        try {
            pose.translate(pos.getX() - camera.x + 0.5, pos.getY() - camera.y + 0.5,
                    pos.getZ() - camera.z + 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(ChestBlock.FACING).toYRot()));
            pose.translate(-0.5, -0.5, -0.5);
            LocalDate date = LocalDate.now();
            boolean christmas = date.getMonthValue() == 12 && date.getDayOfMonth() >= 24 && date.getDayOfMonth() <= 26;
            var kind = christmas ? net.minecraft.client.renderer.blockentity.state.ChestRenderState.ChestMaterialType.CHRISTMAS
                    : chest instanceof net.minecraft.world.level.block.entity.TrappedChestBlockEntity
                    ? net.minecraft.client.renderer.blockentity.state.ChestRenderState.ChestMaterialType.TRAPPED
                    : net.minecraft.client.renderer.blockentity.state.ChestRenderState.ChestMaterialType.REGULAR;
            var material = Sheets.chooseSprite(kind,state.getValue(ChestBlock.TYPE));
            var type = RansomRenderTypes.crucifixChestLight(material.atlasLocation());
            model.render(pose, minecraft.getAtlasManager().get(material).wrap(buffers.getBuffer(type)), 0xF000F0,
                    OverlayTexture.NO_OVERLAY, alpha << 24 | 0x93EFFF);
            buffers.endBatch(type);
        } finally {
            pose.popPose();
        }
    }
}
