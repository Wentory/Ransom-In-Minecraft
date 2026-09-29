package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.client.ClientSessionEvents;
import com.wentory.ransom_in_minecraft.client.InfectedPlayerVisuals;
import com.wentory.ransom_in_minecraft.client.RansomEncounter;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import com.wentory.ransom_in_minecraft.network.InfectionStatusPayload;
import com.wentory.ransom_in_minecraft.network.RansomFailureScarePayload;
import com.wentory.ransom_in_minecraft.network.RansomResumePayload;
import com.wentory.ransom_in_minecraft.network.RansomSummonPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.resources.Identifier;

public final class RansomFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.wentory.ransom_in_minecraft.platform.PayloadRegistrar.registerClientReceivers();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler,sender,client) ->
                ClientSessionEvents.loggingIn(new com.wentory.ransom_in_minecraft.platform.FabricEvents.ClientPlayerNetworkEvent.LoggingIn()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client) ->
                ClientSessionEvents.loggingOut(new com.wentory.ransom_in_minecraft.platform.FabricEvents.ClientPlayerNetworkEvent.LoggingOut()));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((entity,level) ->
                ClientSessionEvents.entityLeft(new com.wentory.ransom_in_minecraft.platform.FabricEvents.EntityLeaveLevelEvent(entity,level)));
        InfectedPlayerVisuals.initialize();
        com.wentory.ransom_in_minecraft.client.InfectedZombieLayers.initialize();
        com.wentory.ransom_in_minecraft.client.InfectedCreeperVisuals.Layers.initialize();
        com.wentory.ransom_in_minecraft.client.WormIllusionLayers.initialize();
        com.wentory.ransom_in_minecraft.client.ChestPreviewRenderer.initialize();
        ClientTickEvents.START_CLIENT_TICK.register(client -> com.wentory.ransom_in_minecraft.client.WormEffects.tick(
                new com.wentory.ransom_in_minecraft.platform.FabricEvents.ClientTickEvent.Pre()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var event=new com.wentory.ransom_in_minecraft.platform.FabricEvents.ClientTickEvent.Post();
            RansomEncounter.clientTick(event);
            com.wentory.ransom_in_minecraft.client.RansomChestVisuals.tick(event);
            com.wentory.ransom_in_minecraft.client.InfectedCreeperVisuals.trailTick(event);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(RansomFabric.MODID, "ransom_overlay"),
                (graphics, delta) -> {
                    var event=new com.wentory.ransom_in_minecraft.platform.FabricEvents.RenderGuiEvent.Post(graphics);
                    com.wentory.ransom_in_minecraft.client.WormEffects.overlay(event);
                    RansomEncounter.render(event);
                });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, delta) ->
                        RansomEncounter.renderInventoryHotbarLock(new com.wentory.ransom_in_minecraft.platform.FabricEvents.ScreenEvent.Render.Post(current, graphics))));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            var event=new com.wentory.ransom_in_minecraft.platform.FabricEvents.SubmitCustomGeometryEvent(
                    context.poseStack(), context.submitNodeCollector(), context.levelState());
            RansomEncounter.renderInfectedBlocks(event);
            com.wentory.ransom_in_minecraft.client.RansomChestVisuals.render(event);
            com.wentory.ransom_in_minecraft.client.InfectedCreeperVisuals.clouds(event);
        });
    }
}
