package com.wentory.ransom_in_minecraft;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public final class RansomItems {
    public static final Supplier<Item> HONEY_POT = register("honey_pot", true);
    public static final Supplier<Item> CRUCIFIX = register("crucifix", false);
    private RansomItems() {}
    private static Supplier<Item> register(String name, boolean honey) {
        var id = Identifier.fromNamespaceAndPath(RansomFabric.MODID, name);
        var properties = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
        Item item = honey ? new HoneyPotItem(properties) : new Item(properties.durability(100));
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return () -> item;
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(HONEY_POT.get());
            entries.accept(CRUCIFIX.get());
        });
    }
}
