package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomResumePayload(int coins, int targetCoins, int phaseTicks) implements CustomPacketPayload {
    public static final Type<RansomResumePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_resume"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomResumePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RansomResumePayload::coins,
            ByteBufCodecs.VAR_INT, RansomResumePayload::targetCoins,
            ByteBufCodecs.VAR_INT, RansomResumePayload::phaseTicks,
            RansomResumePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
