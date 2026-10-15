package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.rules.QteRules.Difficulty;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StealerSettingsPayload(boolean enabled, int chancePercent, Difficulty difficulty) implements CustomPacketPayload {
    public static final Type<StealerSettingsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "stealer_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StealerSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, StealerSettingsPayload::enabled,
                    ByteBufCodecs.VAR_INT, StealerSettingsPayload::chancePercent,
                    ByteBufCodecs.STRING_UTF8.map(Difficulty::valueOf, Difficulty::name), StealerSettingsPayload::difficulty,
                    StealerSettingsPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
