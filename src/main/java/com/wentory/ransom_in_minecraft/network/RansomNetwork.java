package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RansomNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RansomInMinecraft.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    private RansomNetwork() {}
    public static void register() { RansomServerState.registerPayloads(new PayloadRegistrar()); }
    public static void sendToServer(Object payload) { CHANNEL.sendToServer(payload); }
    public static void sendTo(ServerPlayer player, Object payload) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), payload);
    }
    public static void sendToAll(Object payload) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), payload);
    }
}