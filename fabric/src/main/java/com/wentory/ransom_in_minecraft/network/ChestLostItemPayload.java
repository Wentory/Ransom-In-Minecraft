package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChestLostItemPayload(String dimension, long chestPos, String itemId)
        implements CustomPacketPayload {
    public static final Type<ChestLostItemPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "chest_lost_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestLostItemPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.dimension);
                buffer.writeLong(payload.chestPos);
                buffer.writeUtf(payload.itemId);
            },
            buffer -> new ChestLostItemPayload(buffer.readUtf(), buffer.readLong(), buffer.readUtf()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
