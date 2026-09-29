package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WormSettingsPayload(boolean enabled, int healingTicks, int zombieChance, int drownedChance, int creeperChance) implements CustomPacketPayload {
    public static final Type<WormSettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, "worm_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WormSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeBoolean(p.enabled()); b.writeVarInt(p.healingTicks());
                b.writeVarInt(p.zombieChance()); b.writeVarInt(p.drownedChance()); b.writeVarInt(p.creeperChance()); },
            b -> new WormSettingsPayload(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
