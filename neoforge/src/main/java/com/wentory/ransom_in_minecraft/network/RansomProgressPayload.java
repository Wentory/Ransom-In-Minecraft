package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomProgressPayload(int coins, int targetCoins, int phaseTicks) implements CustomPacketPayload {
    public static final Type<RansomProgressPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomProgressPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RansomProgressPayload::coins,
            ByteBufCodecs.VAR_INT, RansomProgressPayload::targetCoins,
            ByteBufCodecs.VAR_INT, RansomProgressPayload::phaseTicks,
            RansomProgressPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
