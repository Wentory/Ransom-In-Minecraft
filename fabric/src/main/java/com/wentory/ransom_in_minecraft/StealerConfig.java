package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.platform.ConfigSpec;
public final class StealerConfig {
    private static final ConfigSpec.Builder BUILDER = new ConfigSpec.Builder();

    public static final ConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the RANSOM: STEALER minigame for unopened loot-table chests.")
            .define("stealer.enabled", true);
    public static final ConfigSpec.IntValue INFECTION_CHANCE_PERCENT = BUILDER
            .comment("Chance in percent that an unopened loot-table chest becomes infected on its first opening attempt. 0 disables random infection; 100 infects every eligible chest.")
            .defineInRange("stealer.infectionChancePercent", 30, 0, 100);

    public static final ConfigSpec.BooleanValue WORM_ENABLED = BUILDER
            .comment("Enable player infection by Ransom: Worm. Disabling clears accumulated infection.")
            .define("worm.enabled", true);
    public static final ConfigSpec.IntValue WORM_HEAL_TICKS = BUILDER
            .comment("Ticks between passive recovery of 1 infection point. 1 second = 20 ticks; default 60 ticks = 3 seconds.")
            .defineInRange("worm.passiveHealingTicks", 60, 1, 72000);

    public static final ConfigSpec.IntValue HIJACK_ZOMBIE_CHANCE = BUILDER
            .comment("Percent chance for a naturally spawned zombie to be Hijacked. 0 disables; 100 always.")
            .defineInRange("hijack.zombieSpawnChancePercent", 5, 0, 100);
    public static final ConfigSpec.IntValue HIJACK_DROWNED_CHANCE = BUILDER
            .comment("Percent chance for a naturally spawned drowned to be Hijacked. Does not affect conversion or infection.")
            .defineInRange("hijack.drownedSpawnChancePercent", 5, 0, 100);
    public static final ConfigSpec.IntValue HIJACK_CREEPER_CHANCE = BUILDER
            .comment("Percent chance for a naturally spawned creeper to be Hijacked.")
            .defineInRange("hijack.creeperSpawnChancePercent", 5, 0, 100);

    public static final ConfigSpec SPEC = BUILDER.build();

    private StealerConfig() {
    }
}
