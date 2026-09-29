package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ChestQteFinishPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<ChestQteFinishPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "chest_qte_finish"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestQteFinishPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUUID(payload.sessionId),
            buffer -> new ChestQteFinishPayload(buffer.readUUID()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
