package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record WormCreeperPayload(UUID entityId, int phase) {
    public static final Class<WormCreeperPayload> TYPE = WormCreeperPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(WormCreeperPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.entityId());
        buffer.writeVarInt(payload.phase());
    }
    public static WormCreeperPayload decode(FriendlyByteBuf buffer) {
        return new WormCreeperPayload(buffer.readUUID(), buffer.readVarInt());
    }

}
