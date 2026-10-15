package com.wentory.ransom_in_minecraft;

import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

/** Searches the hotbar left to right, then the main inventory. */
final class HijackedInventory {
    private HijackedInventory() {}

    static int find(ServerPlayer player, Predicate<ItemStack> matches) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && matches.test(stack)) return slot;
        }
        return -1;
    }

    static void equip(ServerPlayer player, int slot) {
        if (slot < 0) return;
        var inventory = player.getInventory();
        if (slot < 9) {
            if (inventory.selected == slot) return;
            inventory.selected = slot;
            player.connection.send(new ClientboundSetCarriedItemPacket(slot));
        } else {
            ItemStack previous = inventory.getItem(inventory.selected);
            inventory.setItem(inventory.selected, inventory.getItem(slot));
            inventory.setItem(slot, previous);
        }
        player.resetAttackStrengthTicker();
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    static void weapon(ServerPlayer player) {
        equip(player, find(player, stack -> stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem || stack.getItem() instanceof TridentItem
                || (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)
                    && (!player.getProjectile(stack).isEmpty() || CrossbowItem.isCharged(stack))));
    }

    static void tool(ServerPlayer player, BlockState state) {
        // Choose the first suitable tool in the same inventory order, not the strongest one.
        equip(player, find(player, stack -> stack.getItem() instanceof DiggerItem && stack.getDestroySpeed(state) > 1));
    }

    static int food(ServerPlayer player) {
        return find(player, stack -> stack.has(DataComponents.FOOD));
    }

    static int buildingBlock(ServerPlayer player) {
        return find(player, stack -> stack.getItem() instanceof BlockItem item
                && item.getBlock().defaultBlockState().isCollisionShapeFullBlock(player.level(), player.blockPosition()));
    }
}
