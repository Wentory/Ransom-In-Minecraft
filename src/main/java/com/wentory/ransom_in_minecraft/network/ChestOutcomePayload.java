package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record ChestOutcomePayload(String dimension, long chestPos, int outcome, int targetId) {
    public static final Class<ChestOutcomePayload> TYPE = ChestOutcomePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestOutcomePayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.dimension());
        buffer.writeLong(payload.chestPos());
        buffer.writeVarInt(payload.outcome());
        buffer.writeVarInt(payload.targetId());
    }
    public static ChestOutcomePayload decode(FriendlyByteBuf buffer) {
        return new ChestOutcomePayload(buffer.readUtf(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt());
    }

}
