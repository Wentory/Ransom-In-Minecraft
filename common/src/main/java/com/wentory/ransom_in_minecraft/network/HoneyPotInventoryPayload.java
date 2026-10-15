package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.wentory.ransom_in_minecraft.HoneyPotItem;

public record HoneyPotInventoryPayload(int slot) implements CustomPacketPayload {
    public static final Type<HoneyPotInventoryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            "ransom_in_minecraft", "honey_pot_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoneyPotInventoryPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.slot), buffer -> new HoneyPotInventoryPayload(buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public void consume(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || ReplayCompatibility.isReplayServer(player.level().getServer())
                || !player.containerMenu.getCarried().isEmpty() || slot < 0 || slot >= player.getInventory().getContainerSize()) return;
        boolean visible = player.containerMenu.slots.stream().anyMatch(candidate ->
                candidate.container == player.getInventory() && candidate.getContainerSlot() == slot);
        if (!visible) return;
        HoneyPotItem.consume(player, player.getInventory().getItem(slot));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
}
