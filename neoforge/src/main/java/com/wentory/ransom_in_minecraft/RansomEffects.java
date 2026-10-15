package com.wentory.ransom_in_minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class RansomEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RansomInMinecraft.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> FIREWALL = EFFECTS.register("firewall",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD99A31) {});
    private RansomEffects() {}
}
