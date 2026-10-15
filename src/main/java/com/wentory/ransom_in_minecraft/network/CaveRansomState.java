package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
import java.util.function.Consumer;

public record CaveRansomState(UUID owner, boolean active, double x, double y, double z, int attacks, int outcome, int serial, boolean screaming) {
    private static Consumer<CaveRansomState> receiver = p -> {};
    public static void bind(Consumer<CaveRansomState> consumer) { receiver = consumer; }
    public void accept() { receiver.accept(this); }
    public static final Class<CaveRansomState> TYPE = CaveRansomState.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(CaveRansomState p, FriendlyByteBuf b) {
        b.writeUUID(p.owner());
        b.writeBoolean(p.active());
        b.writeDouble(p.x());
        b.writeDouble(p.y());
        b.writeDouble(p.z());
        b.writeVarInt(p.attacks());
        b.writeVarInt(p.outcome());
        b.writeVarInt(p.serial());
        b.writeBoolean(p.screaming());
    }
    public static CaveRansomState decode(FriendlyByteBuf b) { return new CaveRansomState(b.readUUID(), b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean()); }

}
