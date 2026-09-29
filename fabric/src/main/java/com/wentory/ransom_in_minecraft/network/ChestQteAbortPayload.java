package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ChestQteAbortPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<ChestQteAbortPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "chest_qte_abort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestQteAbortPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUUID(payload.sessionId),
            buffer -> new ChestQteAbortPayload(buffer.readUUID()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
