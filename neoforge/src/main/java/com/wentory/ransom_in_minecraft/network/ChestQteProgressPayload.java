package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ChestQteProgressPayload(UUID sessionId, int round, int keyIndex, String sequence)
        implements CustomPacketPayload {
    public static final Type<ChestQteProgressPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "chest_qte_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestQteProgressPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.sessionId);
                buffer.writeVarInt(payload.round);
                buffer.writeVarInt(payload.keyIndex);
                buffer.writeUtf(payload.sequence);
            },
            buffer -> new ChestQteProgressPayload(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readUtf()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
