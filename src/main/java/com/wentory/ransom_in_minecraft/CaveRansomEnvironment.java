package com.wentory.ransom_in_minecraft;

import net.minecraft.world.entity.player.Player;

/** Shared height-based selection for the underground Ransom encounter. */
public final class CaveRansomEnvironment {
    private CaveRansomEnvironment() {}

    public static boolean isCave(Player player) {
        return player.getY() < player.level().getSeaLevel();
    }
}
