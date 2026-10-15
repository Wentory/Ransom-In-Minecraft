package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RansomInMinecraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfectedZombieLayers {
    private InfectedZombieLayers() {}

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        ZombieRenderer renderer = event.getRenderer(EntityType.ZOMBIE);
        if (renderer != null) renderer.addLayer(new InfectionLayer<>(renderer));
        DrownedRenderer drowned = event.getRenderer(EntityType.DROWNED);
        if (drowned != null) drowned.addLayer(new InfectionLayer<>(drowned));
    }

    private static final class InfectionLayer<T extends Zombie, M extends AbstractZombieModel<T>> extends RenderLayer<T, M> {
        private InfectionLayer(RenderLayerParent<T, M> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, T zombie,
                           float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
            if (!ClientInfectedZombies.contains(zombie.getUUID()) || zombie.isInvisible() || zombie.isDeadOrDying()) return;
            HijackedMobSurface.render(getParentModel(), getTextureLocation(zombie), pose, buffers,
                    packedLight, zombie.tickCount, 0x76FF1010);
        }
    }
}
