package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import com.wentory.ransom_in_minecraft.RansomwareConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Map;

public record RansomwareSyncPayload(double jumpscareEnd, double downloadStart, double downloadEnd, boolean deleteItems, int below, int above, int radius, int spawnMin, int spawnMax, boolean biomeWhitelistEnabled, String biomeWhitelist) {
    public static final Class<RansomwareSyncPayload> TYPE = RansomwareSyncPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(RansomwareSyncPayload payload, FriendlyByteBuf buffer) {
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
    }
    public static RansomwareSyncPayload decode(FriendlyByteBuf buffer) {
        return new RansomwareSyncPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf());
    }
    private static Map<ForgeConfigSpec.ConfigValue<?>, Object> remoteValues = Map.of();



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
    public static <T> T value(ForgeConfigSpec.ConfigValue<T> setting) {
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
