package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record FullHijackVisualPayload(UUID playerId, boolean hijacked) implements CustomPacketPayload {
    public static final Type<FullHijackVisualPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "full_hijack_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FullHijackVisualPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeUUID(payload.playerId); buffer.writeBoolean(payload.hijacked); },
            buffer -> new FullHijackVisualPayload(buffer.readUUID(), buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
