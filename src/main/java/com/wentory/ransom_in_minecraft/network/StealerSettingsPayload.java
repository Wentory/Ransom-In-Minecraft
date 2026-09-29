package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record StealerSettingsPayload(boolean enabled, int chancePercent) {
    public static final Class<StealerSettingsPayload> TYPE = StealerSettingsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(StealerSettingsPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.enabled());
        buffer.writeVarInt(payload.chancePercent());
    }
    public static StealerSettingsPayload decode(FriendlyByteBuf buffer) {
        return new StealerSettingsPayload(buffer.readBoolean(), buffer.readVarInt());
    }

}
