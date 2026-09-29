package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomStatePayload(boolean active, boolean failure, int coins, int penaltyDamage,
                                 boolean deleteHotbar) {
    public static final Class<RansomStatePayload> TYPE = RansomStatePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomStatePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.active());
        buffer.writeBoolean(payload.failure());
        buffer.writeVarInt(payload.coins());
        buffer.writeVarInt(payload.penaltyDamage());
        buffer.writeBoolean(payload.deleteHotbar());
    }
    public static RansomStatePayload decode(FriendlyByteBuf buffer) {
        return new RansomStatePayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
    }

}
