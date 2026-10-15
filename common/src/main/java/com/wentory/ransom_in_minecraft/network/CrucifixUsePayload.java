package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CrucifixUsePayload() implements CustomPacketPayload {
    public static final Type<CrucifixUsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ransom_in_minecraft", "crucifix_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrucifixUsePayload> STREAM_CODEC =
            StreamCodec.unit(new CrucifixUsePayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
