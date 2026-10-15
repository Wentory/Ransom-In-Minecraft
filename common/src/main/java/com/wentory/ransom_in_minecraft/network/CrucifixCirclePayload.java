package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record CrucifixCirclePayload(UUID owner, String dimension, double x, double y, double z,
                                    long startTick, boolean visible, boolean banishment, boolean chest,
                                    double projectionX, double projectionY, double projectionZ) implements CustomPacketPayload {
    public CrucifixCirclePayload(UUID owner, String dimension, double x, double y, double z,
                                 long startTick, boolean visible, boolean banishment, boolean chest) {
        this(owner, dimension, x, y, z, startTick, visible, banishment, chest, x, y + 1, z);
    }
    public CrucifixCirclePayload(UUID owner, String dimension, double x, double y, double z,
                                 long startTick, boolean visible, boolean banishment) {
        this(owner, dimension, x, y, z, startTick, visible, banishment, false);
    }
    public CrucifixCirclePayload(UUID owner, String dimension, double x, double y, double z,
                                 long startTick, boolean visible) {
        this(owner, dimension, x, y, z, startTick, visible, false);
    }
    public static final Type<CrucifixCirclePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ransom_in_minecraft", "crucifix_circle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrucifixCirclePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.owner());
                buffer.writeUtf(payload.dimension());
                buffer.writeDouble(payload.x()); buffer.writeDouble(payload.y()); buffer.writeDouble(payload.z());
                buffer.writeLong(payload.startTick()); buffer.writeBoolean(payload.visible());
                buffer.writeBoolean(payload.banishment());
                buffer.writeBoolean(payload.chest());
                buffer.writeDouble(payload.projectionX()); buffer.writeDouble(payload.projectionY()); buffer.writeDouble(payload.projectionZ());
            }, buffer -> new CrucifixCirclePayload(buffer.readUUID(), buffer.readUtf(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
