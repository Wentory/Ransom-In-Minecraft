package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

public final class CrucifixRecipe {
    private static final List<ResourceLocation> RECIPE = List.of(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "crucifix"));
    private CrucifixRecipe() {}

    public static void discover(ServerPlayer player) {
        if (!ReplayCompatibility.isReplayServer(player.getServer())) player.awardRecipesByKey(RECIPE);
    }
}
