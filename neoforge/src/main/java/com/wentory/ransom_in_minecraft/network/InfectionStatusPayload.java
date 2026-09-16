package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record InfectionStatusPayload(UUID playerId, boolean infected) implements CustomPacketPayload {
    public static final Type<InfectionStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "infection_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfectionStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.playerId());
                buffer.writeBoolean(payload.infected());
            },
            buffer -> new InfectionStatusPayload(buffer.readUUID(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
