package com.wentory.ransom_in_minecraft.client;
import com.wentory.ransom_in_minecraft.network.HoneyPotInventoryPayload;
public final class HoneyPotInventorySender {
    private HoneyPotInventorySender() {}
    public static void send(HoneyPotInventoryPayload payload) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
    }
}
