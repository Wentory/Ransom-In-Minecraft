package com.wentory.ransom_in_minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class RansomItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RansomInMinecraft.MODID);
    public static final DeferredHolder<Item, Item> HONEY_POT = ITEMS.register("honey_pot", () -> new HoneyPotItem(properties("honey_pot")));
    public static final DeferredHolder<Item, Item> CRUCIFIX = ITEMS.register("crucifix", () -> new Item(properties("crucifix").durability(100)));
    private RansomItems() {}
    private static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID, name)));
    }
    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(HONEY_POT.get());
            event.accept(CRUCIFIX.get());
        }
    }
}
