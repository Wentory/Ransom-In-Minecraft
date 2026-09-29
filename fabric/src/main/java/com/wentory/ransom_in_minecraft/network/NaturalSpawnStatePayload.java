package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NaturalSpawnStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<NaturalSpawnStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "natural_spawn_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NaturalSpawnStatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, NaturalSpawnStatePayload::enabled,
                    NaturalSpawnStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
