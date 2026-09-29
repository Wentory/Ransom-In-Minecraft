package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomForcedAttackPayload(boolean attack) implements CustomPacketPayload {
    public static final Type<RansomForcedAttackPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "ransom_forced_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomForcedAttackPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, RansomForcedAttackPayload::attack,
                    RansomForcedAttackPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
