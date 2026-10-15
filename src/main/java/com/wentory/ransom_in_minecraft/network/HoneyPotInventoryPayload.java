package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record HoneyPotInventoryPayload(int slot) {
    public static final Class<HoneyPotInventoryPayload> TYPE = HoneyPotInventoryPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(HoneyPotInventoryPayload payload, FriendlyByteBuf buffer) { buffer.writeVarInt(payload.slot()); }
    public static HoneyPotInventoryPayload decode(FriendlyByteBuf buffer) { return new HoneyPotInventoryPayload(buffer.readVarInt()); }
}
