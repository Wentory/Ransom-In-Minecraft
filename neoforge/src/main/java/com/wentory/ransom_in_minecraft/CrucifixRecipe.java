package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.ReplayCompatibility;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

public final class CrucifixRecipe {
    private static final List<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> RECIPE = List.of(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "crucifix")));
    private CrucifixRecipe() {}

    public static void discover(ServerPlayer player) {
        if (!ReplayCompatibility.isReplayServer(player.level().getServer())) player.awardRecipesByKey(RECIPE);
    }
}
