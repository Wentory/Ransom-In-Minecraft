package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
public record RansomResumePayload(int coins,int targetCoins,int phaseTicks){
 static void encode(RansomResumePayload p,FriendlyByteBuf b){b.writeVarInt(p.coins);b.writeVarInt(p.targetCoins);b.writeVarInt(p.phaseTicks);}
 static RansomResumePayload decode(FriendlyByteBuf b){return new RansomResumePayload(b.readVarInt(),b.readVarInt(),b.readVarInt());}
}
