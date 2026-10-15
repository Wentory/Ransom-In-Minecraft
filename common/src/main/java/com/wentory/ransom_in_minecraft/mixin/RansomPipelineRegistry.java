package com.wentory.ransom_in_minecraft.mixin;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(RenderPipelines.class)
public interface RansomPipelineRegistry {
    @Invoker("register") static RenderPipeline ransom$register(RenderPipeline pipeline) { throw new AssertionError(); }
}
