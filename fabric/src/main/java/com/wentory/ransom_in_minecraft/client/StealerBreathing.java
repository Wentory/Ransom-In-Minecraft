package com.wentory.ransom_in_minecraft.client;
public final class StealerBreathing {
    private StealerBreathing() {}
    public static boolean preservesAir(net.minecraft.world.entity.LivingEntity entity) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return entity == mc.player && entity.isAlive() && entity.isUnderWater()
                && mc.gui.screen() instanceof RansomChestScreen && RansomChestScreen.isActive();
    }
}
