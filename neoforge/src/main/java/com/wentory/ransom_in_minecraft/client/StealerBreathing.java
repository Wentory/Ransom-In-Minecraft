package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class StealerBreathing {
    private StealerBreathing() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preserveDisplayedAir(LivingBreatheEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getEntity().level().isClientSide() || event.getEntity() != minecraft.player
                || !event.getEntity().isAlive() || !event.getEntity().isUnderWater()
                || !(minecraft.gui.screen() instanceof RansomChestScreen) || !RansomChestScreen.isActive()) return;
        event.setCanBreathe(true);
        event.setConsumeAirAmount(0);
        event.setRefillAirAmount(0);
    }
}
