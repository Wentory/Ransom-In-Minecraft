package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;
import java.util.function.Consumer;

public record CaveRansomState(UUID owner, boolean active, double x, double y, double z, int attacks, int outcome, int serial, boolean screaming)
        implements CustomPacketPayload {
    private static Consumer<CaveRansomState> receiver = p -> {};
    public static void bind(Consumer<CaveRansomState> consumer) { receiver = consumer; }
    public void accept() { receiver.accept(this); }
    public static final Type<CaveRansomState> TYPE = new Type<>(Identifier.fromNamespaceAndPath("ransom_in_minecraft", "cave_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CaveRansomState> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUUID(p.owner); b.writeBoolean(p.active); b.writeDouble(p.x); b.writeDouble(p.y); b.writeDouble(p.z);
                b.writeVarInt(p.attacks); b.writeVarInt(p.outcome); b.writeVarInt(p.serial); b.writeBoolean(p.screaming); },
            b -> new CaveRansomState(b.readUUID(), b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
