package com.wentory.ransom_in_minecraft.client;
import com.wentory.ransom_in_minecraft.network.HoneyPotInventoryPayload;
public final class HoneyPotInventorySender {
    private HoneyPotInventorySender() {}
    public static void send(HoneyPotInventoryPayload payload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }
}
