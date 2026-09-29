package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ChestQteAbortPayload(UUID sessionId) {
    public static final Class<ChestQteAbortPayload> TYPE = ChestQteAbortPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestQteAbortPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.sessionId());
    }
    public static ChestQteAbortPayload decode(FriendlyByteBuf buffer) {
        return new ChestQteAbortPayload(buffer.readUUID());
    }

}
