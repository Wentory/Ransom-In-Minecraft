package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChestInfectionPayload(String dimension, long blockPos, boolean infected)
        implements CustomPacketPayload {
    public static final Type<ChestInfectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "chest_infection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestInfectionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.dimension);
                buffer.writeLong(payload.blockPos);
                buffer.writeBoolean(payload.infected);
            },
            buffer -> new ChestInfectionPayload(buffer.readUtf(), buffer.readLong(), buffer.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
