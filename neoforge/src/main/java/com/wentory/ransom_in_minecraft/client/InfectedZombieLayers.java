package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.monster.zombie.AbstractZombieModel;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.world.entity.monster.zombie.Drowned;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;


public final class InfectedZombieLayers {
    private static final Identifier GLITCH = Identifier.fromNamespaceAndPath(
            RansomInMinecraft.MODID, "textures/glitch.png");

    private InfectedZombieLayers() {}


    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        ZombieRenderer renderer = event.getRenderer(net.minecraft.world.entity.EntityTypes.ZOMBIE);
        if (renderer != null) renderer.addLayer(new InfectionLayer<>(renderer));
        DrownedRenderer drowned = event.getRenderer(net.minecraft.world.entity.EntityTypes.DROWNED);
        if (drowned != null) drowned.addLayer(new InfectionLayer<>(drowned));
    }

    private static final class InfectionLayer<T extends Zombie, M extends AbstractZombieModel<net.minecraft.client.renderer.entity.state.ZombieRenderState>> extends RenderLayer<net.minecraft.client.renderer.entity.state.ZombieRenderState, M> {
        private InfectionLayer(RenderLayerParent<net.minecraft.client.renderer.entity.state.ZombieRenderState, M> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight,
 net.minecraft.client.renderer.entity.state.ZombieRenderState state, float netHeadYaw, float headPitch) {
 if (!(RenderStateEntities.get(state) instanceof Zombie zombie)) return;
 DeferredBuffers buffers = new DeferredBuffers(collector);
 getParentModel().setupAnim(state);
            if (!ClientInfectedZombies.contains(zombie.getUUID()) || zombie.isInvisible() || zombie.isDeadOrDying()) return;
            HijackedMobSurface.render(getParentModel(),Identifier.withDefaultNamespace(zombie instanceof Drowned ? "textures/entity/zombie/drowned.png" : "textures/entity/zombie/zombie.png"),pose,buffers,packedLight,zombie.tickCount,0x76FF1010);
            buffers.endBatch();
        }
    }
}
