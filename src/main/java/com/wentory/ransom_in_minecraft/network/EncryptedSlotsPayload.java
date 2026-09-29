package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EncryptedSlotsPayload(int mask, boolean glitchHit) implements CustomPacketPayload {
    public static final Type<EncryptedSlotsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "encrypted_slots"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EncryptedSlotsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EncryptedSlotsPayload::mask,
            ByteBufCodecs.BOOL, EncryptedSlotsPayload::glitchHit,
            EncryptedSlotsPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
