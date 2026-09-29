package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ChestQteStartPayload(UUID sessionId, long chestPos, int round, int totalRounds,
                                   String sequence, int keyIndex, long remainingMillis, boolean resumed) {
    public static final Class<ChestQteStartPayload> TYPE = ChestQteStartPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ChestQteStartPayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.sessionId());
        buffer.writeLong(payload.chestPos());
        buffer.writeVarInt(payload.round());
        buffer.writeVarInt(payload.totalRounds());
        buffer.writeUtf(payload.sequence());
        buffer.writeVarInt(payload.keyIndex());
        buffer.writeLong(payload.remainingMillis());
        buffer.writeBoolean(payload.resumed());
    }
    public static ChestQteStartPayload decode(FriendlyByteBuf buffer) {
        return new ChestQteStartPayload(buffer.readUUID(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readLong(), buffer.readBoolean());
    }

}
