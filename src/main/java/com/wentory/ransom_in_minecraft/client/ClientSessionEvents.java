package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import com.wentory.ransom_in_minecraft.network.ClientPlayerGlitchTracker;
import com.wentory.ransom_in_minecraft.network.ClientPlayerVisualEffectTracker;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import com.wentory.ransom_in_minecraft.network.RansomwareSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.minecraft.world.entity.monster.Zombie;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class ClientSessionEvents {
    private ClientSessionEvents() {}

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RansomwareSyncPayload.clear();
        RansomEncounter.handleDisconnect(Minecraft.getInstance());
        CaveRansomClient.clear();
        ClientRansomResumeTracker.clear();
        ClientInfectionTracker.clear();
        ClientInfectedZombies.clear();
        InfectedCreeperVisuals.clear();
        WormEffects.clear();
        FullHijackClient.clear();
        com.wentory.ransom_in_minecraft.network.ClientFullHijackTracker.clear();
        ClientPlayerGlitchTracker.clear();
        ClientPlayerVisualEffectTracker.clear();
        InfectedPlayerGlitchRenderer.clear();
        CrucifixCircleRenderer.clear();
        RansomChestVisuals.clear();
        RansomChestScreen.clear();
    }

    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        CaveRansomClient.clear();
        ClientInfectionTracker.clear();
        ClientInfectedZombies.clear();
        InfectedCreeperVisuals.clear();
        WormEffects.clear();
        FullHijackClient.clear();
        com.wentory.ransom_in_minecraft.network.ClientFullHijackTracker.clear();
        ClientPlayerGlitchTracker.clear();
        ClientPlayerVisualEffectTracker.clear();
        InfectedPlayerGlitchRenderer.clear();
        CrucifixCircleRenderer.clear();
        RansomChestVisuals.clear();
        RansomChestScreen.clear();
    }

    @SubscribeEvent
    public static void entityLeft(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) com.wentory.ransom_in_minecraft.network.ClientFullHijackTracker.remove(event.getEntity().getUUID());
        if (event.getLevel().isClientSide()) InfectedCreeperVisuals.remove(event.getEntity().getUUID());
        if (event.getLevel().isClientSide()) {
            ClientInfectedZombies.remove(event.getEntity().getUUID());
        }
    }
}
