package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record PlayerVisualEffectPayload(UUID playerId, int effect) {
    public static final Class<PlayerVisualEffectPayload> TYPE = PlayerVisualEffectPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static final int CLEAR = 0;
    public static final int STOP = 1;
    public static final int CHAOS = 2;
    public static final int SUCCESS = 3;
    public static void encode(PlayerVisualEffectPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.playerId());
        buffer.writeVarInt(payload.effect());
    }
    public static PlayerVisualEffectPayload decode(FriendlyByteBuf buffer) {
        return new PlayerVisualEffectPayload(buffer.readUUID(), buffer.readVarInt());
    }

}
