package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;
public record InfectionStatusPayload(UUID playerId, boolean infected) {
 static void encode(InfectionStatusPayload p, FriendlyByteBuf b){b.writeUUID(p.playerId);b.writeBoolean(p.infected);}
 static InfectionStatusPayload decode(FriendlyByteBuf b){return new InfectionStatusPayload(b.readUUID(),b.readBoolean());}
}
