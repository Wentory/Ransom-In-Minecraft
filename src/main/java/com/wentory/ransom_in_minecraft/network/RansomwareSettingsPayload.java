package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record RansomwareSettingsPayload(double jumpscareEnd, double downloadStart, double downloadEnd, boolean deleteItems, int below, int above, int radius, int spawnMin, int spawnMax, boolean biomeWhitelistEnabled, String biomeWhitelist) {
    public static final Class<RansomwareSettingsPayload> TYPE = RansomwareSettingsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomwareSettingsPayload payload, FriendlyByteBuf buffer) {
        buffer.writeDouble(payload.jumpscareEnd());
        buffer.writeDouble(payload.downloadStart());
        buffer.writeDouble(payload.downloadEnd());
        buffer.writeBoolean(payload.deleteItems());
        buffer.writeVarInt(payload.below());
        buffer.writeVarInt(payload.above());
        buffer.writeVarInt(payload.radius());
        buffer.writeVarInt(payload.spawnMin());
        buffer.writeVarInt(payload.spawnMax());
        buffer.writeBoolean(payload.biomeWhitelistEnabled());
        buffer.writeUtf(payload.biomeWhitelist());
    }
    public static RansomwareSettingsPayload decode(FriendlyByteBuf buffer) {
        return new RansomwareSettingsPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf());
    }
}
