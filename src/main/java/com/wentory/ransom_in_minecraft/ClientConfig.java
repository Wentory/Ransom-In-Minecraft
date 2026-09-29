package com.wentory.ransom_in_minecraft;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue JUMPSCARE_ENDS_AT = BUILDER
            .comment("Seconds after movement when the jumpscare image and sound stop.")
            .defineInRange("timing.jumpscareEndsAtSeconds", 1.0, 0.05, 30.0);
    public static final ModConfigSpec.DoubleValue DOWNLOAD_STARTS_AT = BUILDER
            .comment("Seconds after movement when the virus download bar appears.")
            .defineInRange("timing.downloadStartsAtSeconds", 1.0, 0.0, 30.0);
    public static final ModConfigSpec.DoubleValue DOWNLOAD_ENDS_AT = BUILDER
            .comment("Seconds after movement when the virus download bar disappears.")
            .defineInRange("timing.downloadEndsAtSeconds", 2.0, 0.05, 30.0);
    public static final ModConfigSpec.BooleanValue DELETE_HOTBAR_ON_FAILURE = BUILDER
            .comment("Delete the hotbar and every additionally encrypted inventory stack after the final failure jumpscare.")
            .define("punishment.deleteHotbarOnFailure", true);
    public static final ModConfigSpec.IntValue INFECTION_VERTICAL_BELOW = BUILDER
            .comment("How many blocks below the player infected blocks may spawn.")
            .defineInRange("game.infectionVerticalBelow", 5, 0, 128);
    public static final ModConfigSpec.IntValue INFECTION_VERTICAL_ABOVE = BUILDER
            .comment("How many blocks above the player infected blocks may spawn.")
            .defineInRange("game.infectionVerticalAbove", 5, 0, 128);
    public static final ModConfigSpec.IntValue INFECTION_RADIUS = BUILDER
            .comment("Horizontal game and infection radius in blocks.")
            .defineInRange("game.infectionRadius", 64, 8, 256);
    public static final ModConfigSpec.IntValue NATURAL_SPAWN_MIN_SECONDS = BUILDER
            .comment("Minimum delay between natural Ransom encounters, in seconds.")
            .defineInRange("spawn.minimumSeconds", 180, 1, 86400);
    public static final ModConfigSpec.IntValue NATURAL_SPAWN_MAX_SECONDS = BUILDER
            .comment("Maximum delay between natural Ransom encounters, in seconds. Values below the minimum are treated as the minimum.")
            .defineInRange("spawn.maximumSeconds", 360, 1, 86400);
    public static final ModConfigSpec.BooleanValue BIOME_WHITELIST_ENABLED = BUILDER
            .comment("Only allow natural Ransom encounters in the biome IDs listed in biomeWhitelist.")
            .define("spawn.biomeWhitelistEnabled", false);
    public static final ModConfigSpec.ConfigValue<String> BIOME_WHITELIST = BUILDER
            .comment("Comma-separated biome IDs where Ransom may naturally appear, for example: minecraft:plains, minecraft:forest")
            .define("spawn.biomeWhitelist", "");
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
