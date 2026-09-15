package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
public record RansomProgressPayload(int coins,int targetCoins,int phaseTicks){
 static void encode(RansomProgressPayload p,FriendlyByteBuf b){b.writeVarInt(p.coins);b.writeVarInt(p.targetCoins);b.writeVarInt(p.phaseTicks);}
 static RansomProgressPayload decode(FriendlyByteBuf b){return new RansomProgressPayload(b.readVarInt(),b.readVarInt(),b.readVarInt());}
}
