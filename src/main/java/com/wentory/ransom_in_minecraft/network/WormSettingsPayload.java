package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record WormSettingsPayload(boolean enabled, int healingTicks, int zombieChance, int drownedChance, int creeperChance) {
    public static final Class<WormSettingsPayload> TYPE = WormSettingsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(WormSettingsPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.enabled());
        buffer.writeVarInt(payload.healingTicks());
        buffer.writeVarInt(payload.zombieChance());
        buffer.writeVarInt(payload.drownedChance());
        buffer.writeVarInt(payload.creeperChance());
    }
    public static WormSettingsPayload decode(FriendlyByteBuf buffer) {
        return new WormSettingsPayload(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

}
