package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GlitchContactPayload() implements CustomPacketPayload {
    public static final Type<GlitchContactPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "glitch_contact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlitchContactPayload> STREAM_CODEC =
            StreamCodec.unit(new GlitchContactPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
