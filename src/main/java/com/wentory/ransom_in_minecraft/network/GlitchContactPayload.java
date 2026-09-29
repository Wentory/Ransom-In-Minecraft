package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;

public record GlitchContactPayload() {
    public static final Class<GlitchContactPayload> TYPE = GlitchContactPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(GlitchContactPayload payload, FriendlyByteBuf buffer) {
        // No fields.
    }
    public static GlitchContactPayload decode(FriendlyByteBuf buffer) {
        return new GlitchContactPayload();
    }

}
