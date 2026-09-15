package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
public record RansomFailureArmedPayload(int coins,boolean deleteHotbar){
 static void encode(RansomFailureArmedPayload p,FriendlyByteBuf b){b.writeVarInt(p.coins);b.writeBoolean(p.deleteHotbar);}
 static RansomFailureArmedPayload decode(FriendlyByteBuf b){return new RansomFailureArmedPayload(b.readVarInt(),b.readBoolean());}
}
