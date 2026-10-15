package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record MobBanishmentPayload(UUID id, long start) implements CustomPacketPayload {
    public static final Type<MobBanishmentPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ransom_in_minecraft", "mob_banishment"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobBanishmentPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeUUID(payload.id()); buffer.writeLong(payload.start()); },
            buffer -> new MobBanishmentPayload(buffer.readUUID(), buffer.readLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
