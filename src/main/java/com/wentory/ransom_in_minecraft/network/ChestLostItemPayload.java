package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record ChestLostItemPayload(String dimension, long chestPos, String itemId) {
    public static final Class<ChestLostItemPayload> TYPE = ChestLostItemPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestLostItemPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUtf(payload.dimension());
        buffer.writeLong(payload.chestPos());
        buffer.writeUtf(payload.itemId());
    }
    public static ChestLostItemPayload decode(FriendlyByteBuf buffer) {
        return new ChestLostItemPayload(buffer.readUtf(), buffer.readLong(), buffer.readUtf());
    }

}
