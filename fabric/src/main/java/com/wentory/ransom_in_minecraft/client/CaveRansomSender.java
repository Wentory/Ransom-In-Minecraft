package com.wentory.ransom_in_minecraft.client;
import com.wentory.ransom_in_minecraft.network.CaveRansomInput;
final class CaveRansomSender {
    static void send(CaveRansomInput packet){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet);}
}
