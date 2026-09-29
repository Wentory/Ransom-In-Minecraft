package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record NaturalSpawnStatePayload(boolean enabled) {
    public static void encode(NaturalSpawnStatePayload payload, FriendlyByteBuf buffer) { buffer.writeBoolean(payload.enabled()); }
    public static NaturalSpawnStatePayload decode(FriendlyByteBuf buffer) { return new NaturalSpawnStatePayload(buffer.readBoolean()); }
}