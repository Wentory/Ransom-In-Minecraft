package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record WormVictimPayload(UUID zombieId, boolean active) {
    public static final Class<WormVictimPayload> TYPE = WormVictimPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(WormVictimPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.zombieId());
        buffer.writeBoolean(payload.active());
    }
    public static WormVictimPayload decode(FriendlyByteBuf buffer) {
        return new WormVictimPayload(buffer.readUUID(), buffer.readBoolean());
    }

}
