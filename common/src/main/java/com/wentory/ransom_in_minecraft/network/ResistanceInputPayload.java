package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ResistanceInputPayload(int sequence, float dx, float dy, boolean click, boolean playing, boolean inventoryOpen)
        implements CustomPacketPayload {
    public static final Type<ResistanceInputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            "ransom_in_minecraft", "resistance_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResistanceInputPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeVarInt(p.sequence); b.writeFloat(p.dx); b.writeFloat(p.dy); b.writeBoolean(p.click); b.writeBoolean(p.playing); b.writeBoolean(p.inventoryOpen); },
            b -> new ResistanceInputPayload(b.readVarInt(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
