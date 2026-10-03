package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT)
public final class StealerBreathing {
    private StealerBreathing() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preserveDisplayedAir(LivingBreatheEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getEntity().level().isClientSide() || event.getEntity() != minecraft.player
                || !event.getEntity().isAlive() || !event.getEntity().isUnderWater()
                || !(minecraft.screen instanceof RansomChestScreen) || !RansomChestScreen.isActive()) return;
        event.setCanBreathe(true);
        event.setConsumeAirAmount(0);
        event.setRefillAirAmount(0);
    }
}
