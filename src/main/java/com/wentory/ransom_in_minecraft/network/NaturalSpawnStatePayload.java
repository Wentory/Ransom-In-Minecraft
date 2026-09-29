package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record NaturalSpawnStatePayload(boolean enabled) {
    public static final Class<NaturalSpawnStatePayload> TYPE = NaturalSpawnStatePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(NaturalSpawnStatePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.enabled());
    }
    public static NaturalSpawnStatePayload decode(FriendlyByteBuf buffer) {
        return new NaturalSpawnStatePayload(buffer.readBoolean());
    }

}
