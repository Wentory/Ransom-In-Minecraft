package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HoneyPotInventoryPayload(int slot) implements CustomPacketPayload {
    public static final Type<HoneyPotInventoryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "honey_pot_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoneyPotInventoryPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> b.writeVarInt(p.slot), b -> new HoneyPotInventoryPayload(b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
