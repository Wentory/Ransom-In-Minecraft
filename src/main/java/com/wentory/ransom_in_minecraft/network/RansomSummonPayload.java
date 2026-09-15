package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
public record RansomSummonPayload(boolean summon){
 static void encode(RansomSummonPayload p,FriendlyByteBuf b){b.writeBoolean(p.summon);}
 static RansomSummonPayload decode(FriendlyByteBuf b){return new RansomSummonPayload(b.readBoolean());}
}
