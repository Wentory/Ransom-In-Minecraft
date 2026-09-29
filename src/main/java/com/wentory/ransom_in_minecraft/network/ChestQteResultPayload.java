package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ChestQteResultPayload(UUID sessionId, int round, boolean success) {
    public static final Class<ChestQteResultPayload> TYPE = ChestQteResultPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestQteResultPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.sessionId());
        buffer.writeVarInt(payload.round());
        buffer.writeBoolean(payload.success());
    }
    public static ChestQteResultPayload decode(FriendlyByteBuf buffer) {
        return new ChestQteResultPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean());
    }

}
