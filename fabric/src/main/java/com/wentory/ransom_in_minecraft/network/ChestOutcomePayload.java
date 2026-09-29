package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChestOutcomePayload(String dimension, long chestPos, int outcome, int targetId)
        implements CustomPacketPayload {
    public static final Type<ChestOutcomePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "chest_outcome"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestOutcomePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.dimension);
                buffer.writeLong(payload.chestPos);
                buffer.writeVarInt(payload.outcome);
                buffer.writeVarInt(payload.targetId);
            },
            buffer -> new ChestOutcomePayload(buffer.readUtf(), buffer.readLong(),
                    buffer.readVarInt(), buffer.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
