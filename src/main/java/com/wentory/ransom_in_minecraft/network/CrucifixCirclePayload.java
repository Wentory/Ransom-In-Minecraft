package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record CrucifixCirclePayload(UUID owner, String dimension, double x, double y, double z,
                                    long startTick, boolean visible, boolean banishment, boolean chest,
                                    double projectionX, double projectionY, double projectionZ) {
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
    public static final Class<CrucifixCirclePayload> TYPE = CrucifixCirclePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(CrucifixCirclePayload p, FriendlyByteBuf b) {
        b.writeUUID(p.owner());
        b.writeUtf(p.dimension());
        b.writeDouble(p.x());
        b.writeDouble(p.y());
        b.writeDouble(p.z());
        b.writeLong(p.startTick());
        b.writeBoolean(p.visible());
        b.writeBoolean(p.banishment());
        b.writeBoolean(p.chest());
        b.writeDouble(p.projectionX());
        b.writeDouble(p.projectionY());
        b.writeDouble(p.projectionZ());
    }
    public static CrucifixCirclePayload decode(FriendlyByteBuf b) { return new CrucifixCirclePayload(b.readUUID(), b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readLong(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble()); }

}
