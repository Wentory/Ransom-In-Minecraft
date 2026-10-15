package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CaveRansomInput(boolean start) implements CustomPacketPayload {
    public static final Type<CaveRansomInput> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "cave_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CaveRansomInput> STREAM_CODEC = StreamCodec.of(
            (b, p) -> b.writeBoolean(p.start), b -> new CaveRansomInput(b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
