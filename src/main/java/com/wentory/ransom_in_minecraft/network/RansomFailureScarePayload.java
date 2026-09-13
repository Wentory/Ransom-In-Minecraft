package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RansomFailureScarePayload(int coins, boolean deleteHotbar) implements CustomPacketPayload {
    public static final Type<RansomFailureScarePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_failure_scare"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomFailureScarePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RansomFailureScarePayload::coins,
                    ByteBufCodecs.BOOL, RansomFailureScarePayload::deleteHotbar,
                    RansomFailureScarePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
