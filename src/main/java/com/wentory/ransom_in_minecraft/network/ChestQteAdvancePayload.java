package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

public record ChestQteAdvancePayload(UUID sessionId, boolean roundSuccess, String lostItem,
                                     int lostCount, int round, int totalRounds,
                                     String nextSequence, int finalOutcome, long remainingMillis) {
    public static final Class<ChestQteAdvancePayload> TYPE = ChestQteAdvancePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static final int NOT_FINISHED = 0;
    public static final int SAVED_ALL = 1;
    public static final int SAVED_SOME = 2;
    public static final int SAVED_NONE = 3;
    public static final int RESISTED = 4;
    public static final int INFECTED = 5;
    public static void encode(ChestQteAdvancePayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.sessionId());
        buffer.writeBoolean(payload.roundSuccess());
        buffer.writeUtf(payload.lostItem());
        buffer.writeVarInt(payload.lostCount());
        buffer.writeVarInt(payload.round());
        buffer.writeVarInt(payload.totalRounds());
        buffer.writeUtf(payload.nextSequence());
        buffer.writeVarInt(payload.finalOutcome());
        buffer.writeLong(payload.remainingMillis());
    }
    public static ChestQteAdvancePayload decode(FriendlyByteBuf buffer) {
        return new ChestQteAdvancePayload(buffer.readUUID(), buffer.readBoolean(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readLong());
    }

}
