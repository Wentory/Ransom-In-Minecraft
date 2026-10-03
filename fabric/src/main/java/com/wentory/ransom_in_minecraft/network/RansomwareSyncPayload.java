package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.RansomwareConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.wentory.ransom_in_minecraft.platform.ConfigSpec;

import java.util.Map;

public record RansomwareSyncPayload(double jumpscareEnd, double downloadStart, double downloadEnd,
                                    boolean deleteItems, int below, int above, int radius,
                                    int spawnMin, int spawnMax, boolean biomeWhitelistEnabled,
                                    String biomeWhitelist) implements CustomPacketPayload {
    public static final Type<RansomwareSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "ransomware_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RansomwareSyncPayload> STREAM_CODEC = StreamCodec.of(
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
            buffer -> new RansomwareSyncPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf()));

    private static Map<ConfigSpec.ConfigValue<?>, Object> remoteValues = Map.of();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void accept() {
        remoteValues = Map.ofEntries(
                Map.entry(RansomwareConfig.JUMPSCARE_ENDS_AT, jumpscareEnd),
                Map.entry(RansomwareConfig.DOWNLOAD_STARTS_AT, downloadStart),
                Map.entry(RansomwareConfig.DOWNLOAD_ENDS_AT, downloadEnd),
                Map.entry(RansomwareConfig.DELETE_HOTBAR_ON_FAILURE, deleteItems),
                Map.entry(RansomwareConfig.INFECTION_VERTICAL_BELOW, below),
                Map.entry(RansomwareConfig.INFECTION_VERTICAL_ABOVE, above),
                Map.entry(RansomwareConfig.INFECTION_RADIUS, radius),
                Map.entry(RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS, spawnMin),
                Map.entry(RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS, spawnMax),
                Map.entry(RansomwareConfig.BIOME_WHITELIST_ENABLED, biomeWhitelistEnabled),
                Map.entry(RansomwareConfig.BIOME_WHITELIST, biomeWhitelist));
    }

    public static void clear() {
        remoteValues = Map.of();
    }

    @SuppressWarnings("unchecked")
    public static <T> T value(ConfigSpec.ConfigValue<T> setting) {
        return (T) remoteValues.getOrDefault(setting, setting.get());
    }

    public static RansomwareSyncPayload current() {
        return new RansomwareSyncPayload(RansomwareConfig.JUMPSCARE_ENDS_AT.get(),
                RansomwareConfig.DOWNLOAD_STARTS_AT.get(), RansomwareConfig.DOWNLOAD_ENDS_AT.get(),
                RansomwareConfig.DELETE_HOTBAR_ON_FAILURE.get(), RansomwareConfig.INFECTION_VERTICAL_BELOW.get(),
                RansomwareConfig.INFECTION_VERTICAL_ABOVE.get(), RansomwareConfig.INFECTION_RADIUS.get(),
                RansomwareConfig.NATURAL_SPAWN_MIN_SECONDS.get(), RansomwareConfig.NATURAL_SPAWN_MAX_SECONDS.get(),
                RansomwareConfig.BIOME_WHITELIST_ENABLED.get(), RansomwareConfig.BIOME_WHITELIST.get());
    }
}
