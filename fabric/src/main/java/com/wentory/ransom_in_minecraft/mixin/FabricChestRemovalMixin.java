package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.network.RansomChestGame;
import com.wentory.ransom_in_minecraft.platform.FabricEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class FabricChestRemovalMixin {
    @Inject(method="setBlockState",at=@At("HEAD"))
    private void ransom$destroyed(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> ci){
        LevelChunk chunk=(LevelChunk)(Object)this;
        // Check before replacement: afterward the position already contains air/new terrain.
        if(chunk.getLevel() instanceof ServerLevel level
                && chunk.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                && chunk.getBlockState(pos).getBlock()!=state.getBlock()
                && chunk.getBlockEntity(pos) instanceof ChestBlockEntity)
            RansomChestGame.breakInfectedChest(new FabricEvents.BreakBlockEvent(level,pos));
    }
}
