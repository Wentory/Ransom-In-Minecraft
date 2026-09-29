package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ZombieStopPayload(UUID zombieId, int ticks) {
    public static final Class<ZombieStopPayload> TYPE = ZombieStopPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ZombieStopPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.zombieId());
        buffer.writeVarInt(payload.ticks());
    }
    public static ZombieStopPayload decode(FriendlyByteBuf buffer) {
        return new ZombieStopPayload(buffer.readUUID(), buffer.readVarInt());
    }

}
