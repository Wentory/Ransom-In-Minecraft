package com.wentory.ransom_in_minecraft.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class RansomClientSetup {
    private RansomClientSetup() {}

    public static void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(InfectedPlayerVisuals::addPlayerLayers);
        modBus.addListener(InfectedZombieLayers::addLayers);
        modBus.addListener(InfectedCreeperVisuals.Layers::add);
        modBus.addListener(WormIllusionLayers::add);
        modBus.addListener(ChestPreviewRenderer::register);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new RansomConfigScreen(parent));
    }
}
