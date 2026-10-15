package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CaveRansomInput(boolean start) implements CustomPacketPayload {
    public static final Type<CaveRansomInput> TYPE = new Type<>(Identifier.fromNamespaceAndPath("ransom_in_minecraft", "cave_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CaveRansomInput> STREAM_CODEC = StreamCodec.of(
            (b, p) -> b.writeBoolean(p.start), b -> new CaveRansomInput(b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
