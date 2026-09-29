package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ChestQteResultPayload(UUID sessionId, int round, boolean success) implements CustomPacketPayload {
    public static final Type<ChestQteResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "chest_qte_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestQteResultPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.sessionId);
                buffer.writeVarInt(payload.round);
                buffer.writeBoolean(payload.success);
            },
            buffer -> new ChestQteResultPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
