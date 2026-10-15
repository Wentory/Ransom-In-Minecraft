package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record InfectedZombiePayload(UUID zombieId, boolean infected) implements CustomPacketPayload {
    public InfectedZombiePayload(UUID zombieId) { this(zombieId, true); }
    public static final Type<InfectedZombiePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "infected_zombie"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfectedZombiePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeUUID(payload.zombieId()); buffer.writeBoolean(payload.infected()); },
            buffer -> new InfectedZombiePayload(buffer.readUUID(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
