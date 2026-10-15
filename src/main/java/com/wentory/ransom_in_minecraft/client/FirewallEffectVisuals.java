package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wentory.ransom_in_minecraft.RansomEffects;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FirewallEffectVisuals {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "textures/mob_effect/firewall.png");

    private FirewallEffectVisuals() {}

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                return false;
            }

            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
                                               GuiGraphics graphics, int x, int y, int blitOffset) {
                graphics.flush();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                graphics.blit(ICON, x, y + 7, 18, 18, 0, 0, 16, 16, 16, 16);
                graphics.flush();
                return true;
            }
        }, RansomEffects.FIREWALL);
    }
}
