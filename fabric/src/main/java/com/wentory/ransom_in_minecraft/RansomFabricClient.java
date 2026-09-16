package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.client.ClientSessionEvents;
import com.wentory.ransom_in_minecraft.client.InfectedPlayerVisuals;
import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import com.wentory.ransom_in_minecraft.client.RansomConfigScreen;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import com.wentory.ransom_in_minecraft.network.InfectionStatusPayload;
import com.wentory.ransom_in_minecraft.network.RansomFailureScarePayload;
import com.wentory.ransom_in_minecraft.network.RansomResumePayload;
import com.wentory.ransom_in_minecraft.network.RansomSummonPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

public final class RansomFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("ransomconfig").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> client.setScreenAndShow(new RansomConfigScreen(client.gui.screen())));
                    return 1;
                })));
        ClientSessionEvents.initialize();
        InfectedPlayerVisuals.initialize();
        ClientPlayNetworking.registerGlobalReceiver(InfectionStatusPayload.TYPE,
                (payload, context) -> ClientInfectionTracker.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(RansomResumePayload.TYPE,
                (payload, context) -> ClientRansomResumeTracker.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(RansomFailureScarePayload.TYPE,
                (payload, context) -> ClientRansomResumeTracker.acceptFailureScare(payload));
        ClientPlayNetworking.registerGlobalReceiver(RansomSummonPayload.TYPE,
                (payload, context) -> ClientRansomResumeTracker.acceptSummon(payload));
        ClientTickEvents.END_CLIENT_TICK.register(client -> RansomEncounter.clientTick());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(RansomFabric.MODID, "ransom_overlay"),
                (graphics, delta) -> RansomEncounter.renderHud(graphics));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, delta) ->
                        RansomEncounter.renderInventoryHotbarLock(current, graphics)));
        LevelRenderEvents.COLLECT_SUBMITS.register(RansomEncounter::renderInfectedBlocks);
    }
}
