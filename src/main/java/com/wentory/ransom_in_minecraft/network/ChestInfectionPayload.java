package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record ChestInfectionPayload(String dimension, long blockPos, boolean infected) {
    public static final Class<ChestInfectionPayload> TYPE = ChestInfectionPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestInfectionPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.dimension());
        buffer.writeLong(payload.blockPos());
        buffer.writeBoolean(payload.infected());
    }
    public static ChestInfectionPayload decode(FriendlyByteBuf buffer) {
        return new ChestInfectionPayload(buffer.readUtf(), buffer.readLong(), buffer.readBoolean());
    }

}
