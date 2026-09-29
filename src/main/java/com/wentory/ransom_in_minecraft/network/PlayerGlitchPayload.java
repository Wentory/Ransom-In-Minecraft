package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record PlayerGlitchPayload(UUID playerId) {
    public static final Class<PlayerGlitchPayload> TYPE = PlayerGlitchPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(PlayerGlitchPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.playerId());
    }
    public static PlayerGlitchPayload decode(FriendlyByteBuf buffer) {
        return new PlayerGlitchPayload(buffer.readUUID());
    }

}
