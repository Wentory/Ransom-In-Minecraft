package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import java.util.List;
final class CrucifixModelParts {
    static void collect(BlockStateModel model,ClientLevel level,BlockPos pos,BlockState state,RandomSource random,List<BlockStateModelPart> parts){
        model.collectParts(level,pos,state,random,parts);
    }
}
