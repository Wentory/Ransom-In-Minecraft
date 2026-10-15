package com.wentory.ransom_in_minecraft;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class RansomEffects {
    public static final Holder<MobEffect> FIREWALL = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(RansomFabric.MODID, "firewall"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xD99A31) {});
    private RansomEffects() {}
    public static void initialize() {}
}
