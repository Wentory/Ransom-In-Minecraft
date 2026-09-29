package com.wentory.ransom_in_minecraft.network;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** A real chest menu with an empty, inaccessible view of the loot during the QTE. */
final class SealedChestMenu extends ChestMenu {
    SealedChestMenu(int id, Inventory inventory, int rows) {
        super(rows == 6 ? MenuType.GENERIC_9x6 : MenuType.GENERIC_9x3,
                id, inventory, new SimpleContainer(rows * 9), rows);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // The real chest inventory is never attached to this menu.
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
