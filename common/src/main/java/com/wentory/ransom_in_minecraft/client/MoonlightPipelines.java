package com.wentory.ransom_in_minecraft.client;
import com.mojang.blaze3d.pipeline.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import com.wentory.ransom_in_minecraft.mixin.RansomPipelineRegistry;
import java.util.Optional;
final class MoonlightPipelines {
    static final RenderPipeline PARTICLE=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ransom_in_minecraft","pipeline/moonlight_particle"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("ransom_in_minecraft","core/moonlight"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,false))
            .withCull(false).build());
    private MoonlightPipelines(){}
    static void preload(){}
}
