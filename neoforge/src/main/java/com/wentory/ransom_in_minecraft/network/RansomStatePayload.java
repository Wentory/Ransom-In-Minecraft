package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RansomStatePayload(boolean active, boolean failure, int coins, int penaltyDamage,
                                 boolean deleteHotbar) implements CustomPacketPayload {
    public static final Type<RansomStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransom_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RansomStatePayload::active,
            ByteBufCodecs.BOOL, RansomStatePayload::failure,
            ByteBufCodecs.VAR_INT, RansomStatePayload::coins,
            ByteBufCodecs.VAR_INT, RansomStatePayload::penaltyDamage,
            ByteBufCodecs.BOOL, RansomStatePayload::deleteHotbar,
            RansomStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
