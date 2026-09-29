package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChestQteStartPayload(UUID sessionId, long chestPos, int round, int totalRounds,
                                   String sequence, int keyIndex, long remainingMillis, boolean resumed)
        implements CustomPacketPayload {
    public static final Type<ChestQteStartPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "chest_qte_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestQteStartPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.sessionId);
                buffer.writeLong(payload.chestPos);
                buffer.writeVarInt(payload.round);
                buffer.writeVarInt(payload.totalRounds);
                buffer.writeUtf(payload.sequence);
                buffer.writeVarInt(payload.keyIndex);
                buffer.writeLong(payload.remainingMillis);
                buffer.writeBoolean(payload.resumed);
            },
            buffer -> new ChestQteStartPayload(buffer.readUUID(), buffer.readLong(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readLong(), buffer.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
