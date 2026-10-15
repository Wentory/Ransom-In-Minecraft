package com.wentory.ransom_in_minecraft.client;
/** Compile native effect pipelines during resource loading, rather than on the first attack. */
final class RansomShaderPreloader {
    static void load(){
        MoonlightPipelines.preload();CrucifixPipelines.preload();CaveRansomPipelines.preload();
        HijackedMobSurface.preload();ResistanceProgressBar.preload();
        var mc=net.minecraft.client.Minecraft.getInstance();
        var shaders=mc.getShaderManager();
        int count=0;
        for(var pipeline:net.minecraft.client.renderer.RenderPipelines.getStaticPipelines()){
            if(!pipeline.getLocation().getNamespace().equals("ransom_in_minecraft"))continue;
            if(!com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline(pipeline,shaders::getShader).isValid())
                throw new IllegalStateException("Unable to compile Ransom pipeline "+pipeline.getLocation());
            count++;
        }
        if(shaders.getPostChain(net.minecraft.resources.Identifier.fromNamespaceAndPath("ransom_in_minecraft","full_hijack"),
                net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS)==null)
            throw new IllegalStateException("Unable to load Ransom full Hijack post chain");
        com.mojang.logging.LogUtils.getLogger().info("Preloaded {} Ransom effect pipelines and full Hijack post chain",count);
    }
    private RansomShaderPreloader(){}
}
