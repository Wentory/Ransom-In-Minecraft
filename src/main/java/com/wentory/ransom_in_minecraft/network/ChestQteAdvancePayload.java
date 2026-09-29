package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ChestQteAdvancePayload(UUID sessionId, boolean roundSuccess, String lostItem,
                                     int lostCount, int round, int totalRounds,
                                     String nextSequence, int finalOutcome, long remainingMillis) implements CustomPacketPayload {
    public static final int NOT_FINISHED = 0;
    public static final int SAVED_ALL = 1;
    public static final int SAVED_SOME = 2;
    public static final int SAVED_NONE = 3;
    public static final int RESISTED = 4;
    public static final int INFECTED = 5;
    public static final Type<ChestQteAdvancePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "chest_qte_advance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestQteAdvancePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.sessionId);
                buffer.writeBoolean(payload.roundSuccess);
                buffer.writeUtf(payload.lostItem);
                buffer.writeVarInt(payload.lostCount);
                buffer.writeVarInt(payload.round);
                buffer.writeVarInt(payload.totalRounds);
                buffer.writeUtf(payload.nextSequence);
                buffer.writeVarInt(payload.finalOutcome);
                buffer.writeLong(payload.remainingMillis);
            },
            buffer -> new ChestQteAdvancePayload(buffer.readUUID(), buffer.readBoolean(), buffer.readUtf(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(),
                    buffer.readVarInt(), buffer.readLong()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
