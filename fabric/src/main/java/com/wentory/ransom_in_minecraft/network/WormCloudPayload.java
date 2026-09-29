package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WormCloudPayload(String dimension, double x, double y, double z, int ticks) implements CustomPacketPayload {
    public static final Type<WormCloudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "worm_cloud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WormCloudPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.dimension()); b.writeDouble(p.x()); b.writeDouble(p.y()); b.writeDouble(p.z()); b.writeVarInt(p.ticks()); },
            b -> new WormCloudPayload(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
