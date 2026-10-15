package com.wentory.ransom_in_minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class RansomEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, RansomInMinecraft.MODID);
    public static final RegistryObject<MobEffect> FIREWALL = EFFECTS.register("firewall",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD99A31) {
                @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
                    com.wentory.ransom_in_minecraft.client.FirewallEffectVisuals.initialize(consumer);
                }
            });

    private RansomEffects() {
    }
}
