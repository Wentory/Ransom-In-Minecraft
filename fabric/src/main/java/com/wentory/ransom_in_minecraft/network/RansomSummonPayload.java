package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomSummonPayload(boolean summon) implements CustomPacketPayload {
    public static final Type<RansomSummonPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "ransom_summon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomSummonPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, RansomSummonPayload::summon, RansomSummonPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
