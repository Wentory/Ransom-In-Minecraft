package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerVisualEffectRequestPayload(int effect) implements CustomPacketPayload {
    public static final Type<PlayerVisualEffectRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "player_visual_effect_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVisualEffectRequestPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PlayerVisualEffectRequestPayload::effect,
                    PlayerVisualEffectRequestPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
