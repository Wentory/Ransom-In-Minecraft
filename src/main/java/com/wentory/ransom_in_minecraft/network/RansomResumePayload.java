package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomResumePayload(int coins, int targetCoins, int phaseTicks) {
    public static final Class<RansomResumePayload> TYPE = RansomResumePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomResumePayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.coins());
        buffer.writeVarInt(payload.targetCoins());
        buffer.writeVarInt(payload.phaseTicks());
    }
    public static RansomResumePayload decode(FriendlyByteBuf buffer) {
        return new RansomResumePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

}
