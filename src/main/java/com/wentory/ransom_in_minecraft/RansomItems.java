package com.wentory.ransom_in_minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RansomItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RansomInMinecraft.MODID);
    public static final DeferredHolder<Item, Item> HONEY_POT = ITEMS.register("honey_pot",
            () -> new HoneyPotItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRUCIFIX = ITEMS.register("crucifix",
            () -> new Item(new Item.Properties().durability(100)));
    // Advancement-only icon; not included in creative tabs or recipes.
    public static final DeferredHolder<Item, Item> STOP_ICON = ITEMS.register("stop_icon",
            () -> new Item(new Item.Properties()));
    private RansomItems() {
    }

    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(HONEY_POT.get());
            event.accept(CRUCIFIX.get());
        }
    }
}
