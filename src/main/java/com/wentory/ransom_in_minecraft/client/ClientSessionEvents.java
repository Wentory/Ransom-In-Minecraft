package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientNaturalSpawnState;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class ClientSessionEvents {
    private ClientSessionEvents() {}

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RansomEncounter.handleDisconnect(Minecraft.getInstance());
        ClientRansomResumeTracker.clear();
        ClientInfectionTracker.clear();
        ClientNaturalSpawnState.reset();
    }

    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientInfectionTracker.clear();
        ClientNaturalSpawnState.reset();
    }
}
