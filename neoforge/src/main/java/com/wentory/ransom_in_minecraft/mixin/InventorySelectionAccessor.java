package com.wentory.ransom_in_minecraft.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface InventorySelectionAccessor {
    @Accessor("selected")
    void ransom$setSelectedSlot(int slot);

    static void selectHidden(Inventory inventory) {
        ((InventorySelectionAccessor) inventory).ransom$setSelectedSlot(Inventory.getSelectionSize());
    }
}
