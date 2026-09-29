package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.network.RansomChestGame;
import com.wentory.ransom_in_minecraft.platform.FabricEvents;
import net.minecraft.server.network.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerChunkSender.class)
public abstract class FabricChunkTrackingMixin {
    @Inject(method="sendChunk",at=@At("TAIL"))
    private static void ransom$sync(ServerGamePacketListenerImpl connection,ServerLevel level,LevelChunk chunk,CallbackInfo ci){
        RansomChestGame.syncChunkInfections(new FabricEvents.ChunkWatchEvent.Sent(connection.player,level,chunk));
    }
}
