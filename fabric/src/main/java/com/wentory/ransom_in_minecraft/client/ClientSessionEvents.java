package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.network.ClientInfectionTracker;
import com.wentory.ransom_in_minecraft.network.ClientRansomResumeTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class ClientSessionEvents {
    private ClientSessionEvents() {}

    public static void initialize() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> {
            RansomEncounter.handleDisconnect(minecraft);
            ClientRansomResumeTracker.clear();
            ClientInfectionTracker.clear();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraft) ->
                ClientInfectionTracker.clear());
    }
}