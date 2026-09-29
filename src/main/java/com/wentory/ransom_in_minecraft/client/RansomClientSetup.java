package com.wentory.ransom_in_minecraft.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only registration; never resolve Minecraft screens on a dedicated server. */
public final class RansomClientSetup {
    private RansomClientSetup() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new RansomConfigScreen(parent)));
    }
}
