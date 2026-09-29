package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomForcedAttackPayload(boolean attack) {
    public static final Class<RansomForcedAttackPayload> TYPE = RansomForcedAttackPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomForcedAttackPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.attack());
    }
    public static RansomForcedAttackPayload decode(FriendlyByteBuf buffer) {
        return new RansomForcedAttackPayload(buffer.readBoolean());
    }

}
