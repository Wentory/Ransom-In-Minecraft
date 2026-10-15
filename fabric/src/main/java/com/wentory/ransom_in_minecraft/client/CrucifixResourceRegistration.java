package com.wentory.ransom_in_minecraft.client;
final class CrucifixResourceRegistration {
    static void install(){
        var loader=net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
        var id=net.minecraft.resources.Identifier.fromNamespaceAndPath("ransom_in_minecraft","crucifix_models");
        loader.registerReloadListener(id,(net.minecraft.server.packs.resources.ResourceManagerReloadListener)CrucifixCircleRenderer::reload);
        loader.addListenerOrdering(net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys.Client.SHADERS,id);
    }
}
