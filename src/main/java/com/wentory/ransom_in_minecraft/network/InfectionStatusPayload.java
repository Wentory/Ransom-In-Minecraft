package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record InfectionStatusPayload(UUID playerId, boolean infected) {
    public static final Class<InfectionStatusPayload> TYPE = InfectionStatusPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(InfectionStatusPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.playerId());
        buffer.writeBoolean(payload.infected());
    }
    public static InfectionStatusPayload decode(FriendlyByteBuf buffer) {
        return new InfectionStatusPayload(buffer.readUUID(), buffer.readBoolean());
    }

}
