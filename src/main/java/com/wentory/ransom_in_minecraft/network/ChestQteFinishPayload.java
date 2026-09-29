package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ChestQteFinishPayload(UUID sessionId) {
    public static final Class<ChestQteFinishPayload> TYPE = ChestQteFinishPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestQteFinishPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.sessionId());
    }
    public static ChestQteFinishPayload decode(FriendlyByteBuf buffer) {
        return new ChestQteFinishPayload(buffer.readUUID());
    }

}
