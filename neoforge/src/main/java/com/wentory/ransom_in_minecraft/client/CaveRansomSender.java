package com.wentory.ransom_in_minecraft.client;
import com.wentory.ransom_in_minecraft.network.CaveRansomInput;
final class CaveRansomSender {
    static void send(CaveRansomInput packet){net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(packet);}
}
