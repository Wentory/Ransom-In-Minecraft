package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

public final class FirewallEffectVisuals {
    private static final ResourceLocation ICON = new ResourceLocation("ransom_in_minecraft", "textures/mob_effect/firewall.png");
    private FirewallEffectVisuals() {}
    public static void initialize(java.util.function.Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override public boolean isVisibleInGui(MobEffectInstance instance) { return false; }
            @Override public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
                    GuiGraphics graphics, int x, int y, int blitOffset) {
                graphics.flush();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, 1);
                graphics.blit(ICON, x, y + 7, 18, 18, 0, 0, 16, 16, 16, 16);
                graphics.flush();
                return true;
            }
        });
    }
}
