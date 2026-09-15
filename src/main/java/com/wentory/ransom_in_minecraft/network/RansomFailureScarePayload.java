package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
public record RansomFailureScarePayload(int coins,boolean deleteHotbar){
 static void encode(RansomFailureScarePayload p,FriendlyByteBuf b){b.writeVarInt(p.coins);b.writeBoolean(p.deleteHotbar);}
 static RansomFailureScarePayload decode(FriendlyByteBuf b){return new RansomFailureScarePayload(b.readVarInt(),b.readBoolean());}
}
