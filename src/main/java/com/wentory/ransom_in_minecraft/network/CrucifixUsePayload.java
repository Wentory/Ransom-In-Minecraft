package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CrucifixUsePayload() implements CustomPacketPayload {
    public static final Type<CrucifixUsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "crucifix_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrucifixUsePayload> STREAM_CODEC =
            StreamCodec.unit(new CrucifixUsePayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
