package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WormInfectionPayload(int value) implements CustomPacketPayload {
    public static final Type<WormInfectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "worm_infection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WormInfectionPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> b.writeVarInt(p.value()), b -> new WormInfectionPayload(b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
