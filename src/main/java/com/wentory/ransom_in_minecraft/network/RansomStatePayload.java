package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
public record RansomStatePayload(boolean active, boolean failure, int coins, int penaltyDamage, boolean deleteHotbar) {
 static void encode(RansomStatePayload p,FriendlyByteBuf b){b.writeBoolean(p.active);b.writeBoolean(p.failure);b.writeVarInt(p.coins);b.writeVarInt(p.penaltyDamage);b.writeBoolean(p.deleteHotbar);}
 static RansomStatePayload decode(FriendlyByteBuf b){return new RansomStatePayload(b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readBoolean());}
}
