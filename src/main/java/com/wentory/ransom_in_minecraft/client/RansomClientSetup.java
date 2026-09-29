package com.wentory.ransom_in_minecraft.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only registration; never resolve Minecraft screens on a dedicated server. */
public final class RansomClientSetup {
    private RansomClientSetup() {}

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new RansomConfigScreen(parent));
    }
}
