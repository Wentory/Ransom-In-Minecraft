package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RansomwareSettingsPayload(double jumpscareEnd, double downloadStart, double downloadEnd,
                                        boolean deleteItems, int below, int above, int radius,
                                        int spawnMin, int spawnMax, boolean biomeWhitelistEnabled,
                                        String biomeWhitelist) implements CustomPacketPayload {
    public static final Type<RansomwareSettingsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "ransomware_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomwareSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeDouble(payload.jumpscareEnd());
                buffer.writeDouble(payload.downloadStart());
                buffer.writeDouble(payload.downloadEnd());
                buffer.writeBoolean(payload.deleteItems());
                buffer.writeVarInt(payload.below());
                buffer.writeVarInt(payload.above());
                buffer.writeVarInt(payload.radius());
                buffer.writeVarInt(payload.spawnMin());
                buffer.writeVarInt(payload.spawnMax());
                buffer.writeBoolean(payload.biomeWhitelistEnabled());
                buffer.writeUtf(payload.biomeWhitelist());
            },
            buffer -> new RansomwareSettingsPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
