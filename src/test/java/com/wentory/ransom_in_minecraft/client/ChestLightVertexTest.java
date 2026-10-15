package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import java.util.EnumSet;

/** Exercises the real vanilla model/buffer path without a window or GL context. */
public final class ChestLightVertexTest {
    public static void main(String[] args) {
        var cube = new ModelPart.Cube(0, 0, 0, 0, 0, 14, 10, 14, 0, 0, 0, false,
                64, 64, EnumSet.allOf(Direction.class));
        var pose = new PoseStack();
        var broken = new BufferBuilder(4096);
        broken.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean reproduced = false;
        try { cube.compile(pose.last(), broken, 0xF000F0, 0, .58F, .94F, 1, .75F); }
        catch (IllegalStateException expected) {
            reproduced = expected.getMessage().contains("Not filled all elements");
        }
        if (!reproduced) throw new AssertionError("Original crash did not reproduce");
        broken.discard();
        for (boolean mirrored : new boolean[]{false, true}) {
            var model = new ModelPart.Cube(0, 0, 0, 0, 0, 14, 10, 14, 0, 0, 0, mirrored,
                    64, 64, EnumSet.allOf(Direction.class));
            var buffer = new BufferBuilder(4096);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            model.compile(pose.last(), new ChestLightVertexConsumer(buffer), 0xF000F0, 0,
                    .58F, .94F, 1, .75F);
            var rendered = buffer.end();
            if (rendered.drawState().vertexCount() != 24) throw new AssertionError("Missing faces");
            rendered.release();
        }
        System.out.println("Chest light: original crash reproduced; both model variants emit 24 vertices without GL");
    }
}
