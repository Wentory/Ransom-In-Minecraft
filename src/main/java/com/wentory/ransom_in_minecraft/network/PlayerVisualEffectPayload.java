package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PlayerVisualEffectPayload(UUID playerId, int effect) implements CustomPacketPayload {
    public static final int CLEAR = 0;
    public static final int STOP = 1;
    public static final int CHAOS = 2;
    public static final int SUCCESS = 3;

    public static final Type<PlayerVisualEffectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "player_visual_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVisualEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.playerId);
                buffer.writeVarInt(payload.effect);
            },
            buffer -> new PlayerVisualEffectPayload(buffer.readUUID(), buffer.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
