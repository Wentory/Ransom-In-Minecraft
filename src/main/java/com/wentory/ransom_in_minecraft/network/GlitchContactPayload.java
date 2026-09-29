package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GlitchContactPayload() implements CustomPacketPayload {
    public static final Type<GlitchContactPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "glitch_contact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlitchContactPayload> STREAM_CODEC =
            StreamCodec.unit(new GlitchContactPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
