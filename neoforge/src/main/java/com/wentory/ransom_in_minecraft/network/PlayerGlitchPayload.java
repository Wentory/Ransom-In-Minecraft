package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record PlayerGlitchPayload(UUID playerId) implements CustomPacketPayload {
    public static final Type<PlayerGlitchPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "player_glitch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerGlitchPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> buffer.writeUUID(payload.playerId),
                    buffer -> new PlayerGlitchPayload(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
