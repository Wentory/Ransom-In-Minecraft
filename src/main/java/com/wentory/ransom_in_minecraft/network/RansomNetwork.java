package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class RansomNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RansomInMinecraft.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    private static int id;

    private RansomNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(RansomStatePayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RansomStatePayload::encode).decoder(RansomStatePayload::decode).consumerMainThread(RansomNetwork::state).add();
        CHANNEL.messageBuilder(InfectionStatusPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(InfectionStatusPayload::encode).decoder(InfectionStatusPayload::decode).consumerMainThread((p, c) -> client(c, () -> ClientInfectionTracker.accept(p))).add();
        CHANNEL.messageBuilder(RansomProgressPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RansomProgressPayload::encode).decoder(RansomProgressPayload::decode).consumerMainThread((p, c) -> { ServerPlayer player=c.get().getSender(); if(player!=null) RansomServerState.saveProgress(player,p); c.get().setPacketHandled(true); }).add();
        CHANNEL.messageBuilder(RansomResumePayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RansomResumePayload::encode).decoder(RansomResumePayload::decode).consumerMainThread((p, c) -> client(c, () -> ClientRansomResumeTracker.accept(p))).add();
        CHANNEL.messageBuilder(RansomFailureArmedPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RansomFailureArmedPayload::encode).decoder(RansomFailureArmedPayload::decode).consumerMainThread(RansomNetwork::armed).add();
        CHANNEL.messageBuilder(RansomFailureScarePayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RansomFailureScarePayload::encode).decoder(RansomFailureScarePayload::decode).consumerMainThread((p, c) -> client(c, () -> ClientRansomResumeTracker.acceptFailureScare(p))).add();
        CHANNEL.messageBuilder(RansomSummonPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RansomSummonPayload::encode).decoder(RansomSummonPayload::decode).consumerMainThread((p, c) -> client(c, () -> ClientRansomResumeTracker.acceptSummon(p))).add();
    }

    private static void state(RansomStatePayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) RansomServerState.acceptState(player, payload);
        context.setPacketHandled(true);
    }

    private static void armed(RansomFailureArmedPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) RansomServerState.armFailure(player, payload);
        context.setPacketHandled(true);
    }

    private static void client(Supplier<NetworkEvent.Context> supplier, Runnable work) {
        supplier.get().enqueueWork(work);
        supplier.get().setPacketHandled(true);
    }

    public static void sendToServer(Object payload) { CHANNEL.sendToServer(payload); }
    public static void sendTo(ServerPlayer player, Object payload) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload); }
    public static void sendToAll(Object payload) { CHANNEL.send(PacketDistributor.ALL.noArg(), payload); }
}
