package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record WormCreeperPayload(UUID entityId, int phase) implements CustomPacketPayload {
    public static final Type<WormCreeperPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "worm_creeper"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WormCreeperPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeUUID(payload.entityId()); buffer.writeVarInt(payload.phase()); },
            buffer -> new WormCreeperPayload(buffer.readUUID(), buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
