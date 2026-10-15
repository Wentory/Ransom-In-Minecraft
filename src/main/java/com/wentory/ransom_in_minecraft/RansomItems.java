package com.wentory.ransom_in_minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class RansomItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RansomInMinecraft.MODID);
    public static final RegistryObject<Item> HONEY_POT = ITEMS.register("honey_pot",
            () -> new HoneyPotItem(new Item.Properties()));
    public static final RegistryObject<Item> CRUCIFIX = ITEMS.register("crucifix",
            () -> new Item(new Item.Properties().durability(100)));
    private RansomItems() {
    }

    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(HONEY_POT.get());
            event.accept(CRUCIFIX.get());
        }
    }
}
