package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomFailureArmedPayload(int coins, boolean deleteHotbar) implements CustomPacketPayload {
    public static final Type<RansomFailureArmedPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_failure_armed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomFailureArmedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RansomFailureArmedPayload::coins,
            ByteBufCodecs.BOOL, RansomFailureArmedPayload::deleteHotbar,
            RansomFailureArmedPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
