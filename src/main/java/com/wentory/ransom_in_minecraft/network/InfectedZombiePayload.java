package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record InfectedZombiePayload(UUID zombieId) {
    public static final Class<InfectedZombiePayload> TYPE = InfectedZombiePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(InfectedZombiePayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.zombieId());
    }
    public static InfectedZombiePayload decode(FriendlyByteBuf buffer) {
        return new InfectedZombiePayload(buffer.readUUID());
    }

}
