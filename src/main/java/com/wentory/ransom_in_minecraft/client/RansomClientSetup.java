package com.wentory.ransom_in_minecraft.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only registration; never resolve Minecraft screens on a dedicated server. */
public final class RansomClientSetup {
    private RansomClientSetup() {}

    public static void registerConfigScreen() {
        // Bind before joining any world. Unbound common payload dispatchers are
        // deliberately no-ops on a server, but must never remain no-ops on a client.
        FullHijackClient.bindReceivers();
        CaveRansomClient.bindReceiver();
        com.wentory.ransom_in_minecraft.network.CrucifixClientPayloads.bind(
                CrucifixCircleRenderer::accept, MobBanishmentVisuals::accept);
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(MobBanishmentVisuals.Layers::add);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new RansomConfigScreen(parent)));
    }
}
