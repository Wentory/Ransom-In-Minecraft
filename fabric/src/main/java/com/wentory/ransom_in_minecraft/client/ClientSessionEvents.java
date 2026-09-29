package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import com.wentory.ransom_in_minecraft.network.ClientPlayerGlitchTracker;
import com.wentory.ransom_in_minecraft.network.ClientPlayerVisualEffectTracker;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import net.minecraft.client.Minecraft;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.ClientPlayerNetworkEvent;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.EntityLeaveLevelEvent;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class ClientSessionEvents {
    private ClientSessionEvents() {}

    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RansomEncounter.handleDisconnect(Minecraft.getInstance());
        ClientRansomResumeTracker.clear();
        ClientInfectionTracker.clear();
        ClientInfectedZombies.clear();
        InfectedCreeperVisuals.clear();
        WormEffects.clear();
        ClientPlayerGlitchTracker.clear();
        ClientPlayerVisualEffectTracker.clear();
        InfectedPlayerGlitchRenderer.clear();
        RansomChestVisuals.clear();
        RansomChestScreen.clear();
    }

    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientInfectionTracker.clear();
        ClientInfectedZombies.clear();
        InfectedCreeperVisuals.clear();
        WormEffects.clear();
        ClientPlayerGlitchTracker.clear();
        ClientPlayerVisualEffectTracker.clear();
        InfectedPlayerGlitchRenderer.clear();
        RansomChestVisuals.clear();
        RansomChestScreen.clear();
    }

    public static void entityLeft(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) InfectedCreeperVisuals.remove(event.getEntity().getUUID());
        if (event.getLevel().isClientSide()) {
            ClientInfectedZombies.remove(event.getEntity().getUUID());
        }
    }
}
