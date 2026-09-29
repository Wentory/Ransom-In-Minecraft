package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ChestQteProgressPayload(UUID sessionId, int round, int keyIndex, String sequence) {
    public static final Class<ChestQteProgressPayload> TYPE = ChestQteProgressPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestQteProgressPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.sessionId());
        buffer.writeVarInt(payload.round());
        buffer.writeVarInt(payload.keyIndex());
        buffer.writeUtf(payload.sequence());
    }
    public static ChestQteProgressPayload decode(FriendlyByteBuf buffer) {
        return new ChestQteProgressPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
    }

}
