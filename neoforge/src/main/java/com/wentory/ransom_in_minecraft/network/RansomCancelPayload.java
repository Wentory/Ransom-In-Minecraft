package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomCancelPayload(boolean cancel) implements CustomPacketPayload {
    public static final Type<RansomCancelPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_cancel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomCancelPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, RansomCancelPayload::cancel, RansomCancelPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
