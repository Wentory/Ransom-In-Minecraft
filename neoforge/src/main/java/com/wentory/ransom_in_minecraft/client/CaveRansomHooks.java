package com.wentory.ransom_in_minecraft.client;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=RansomInMinecraft.MODID,value=Dist.CLIENT)
public final class CaveRansomHooks {
    @SubscribeEvent public static void reload(AddClientReloadListenersEvent event){
        event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath(RansomInMinecraft.MODID,"crucifix_models"),
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener)CrucifixCircleRenderer::reload);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){CaveRansomClient.initialize();CrucifixCircleRenderer.initialize();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){CaveRansomClient.tick();MoonlightEffects.tick();}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){CaveRansomClient.hud(event.getGuiGraphics());CrucifixActivationFlash.render(event.getGuiGraphics());}
    @SubscribeEvent public static void render(SubmitCustomGeometryEvent event){CaveRansomClient.render(event.getPoseStack(),event.getSubmitNodeCollector(),event.getLevelRenderState());
        CrucifixCircleRenderer.render(new WorldEffectFrame(event.getPoseStack(),event.getSubmitNodeCollector(),event.getLevelRenderState()));}
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event){
        if(event.isAttack()&&CaveRansomClient.attack()){event.setCanceled(true);event.setSwingHand(true);}
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void shake(ViewportEvent.ComputeCameraAngles event){
        float[] shake=CaveRansomClient.shake();
        event.setYaw(event.getYaw()+shake[0]);event.setPitch(event.getPitch()+shake[1]);event.setRoll(event.getRoll()+shake[2]);
    }
}
