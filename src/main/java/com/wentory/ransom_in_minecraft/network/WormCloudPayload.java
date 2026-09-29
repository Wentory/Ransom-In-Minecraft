package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record WormCloudPayload(String dimension, double x, double y, double z, int ticks) {
    public static final Class<WormCloudPayload> TYPE = WormCloudPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(WormCloudPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.dimension());
        buffer.writeDouble(payload.x());
        buffer.writeDouble(payload.y());
        buffer.writeDouble(payload.z());
        buffer.writeVarInt(payload.ticks());
    }
    public static WormCloudPayload decode(FriendlyByteBuf buffer) {
        return new WormCloudPayload(buffer.readUtf(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt());
    }

}
