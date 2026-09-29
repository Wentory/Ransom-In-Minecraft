package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record ZombieStopPayload(UUID zombieId, int ticks) implements CustomPacketPayload {
    public static final Type<ZombieStopPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "zombie_stop"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZombieStopPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeUUID(p.zombieId()); b.writeVarInt(p.ticks()); },
            b -> new ZombieStopPayload(b.readUUID(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
