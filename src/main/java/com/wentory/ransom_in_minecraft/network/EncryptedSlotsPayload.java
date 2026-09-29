package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record EncryptedSlotsPayload(int mask, boolean glitchHit) {
    public static final Class<EncryptedSlotsPayload> TYPE = EncryptedSlotsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(EncryptedSlotsPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.mask());
        buffer.writeBoolean(payload.glitchHit());
    }
    public static EncryptedSlotsPayload decode(FriendlyByteBuf buffer) {
        return new EncryptedSlotsPayload(buffer.readVarInt(), buffer.readBoolean());
    }

}
