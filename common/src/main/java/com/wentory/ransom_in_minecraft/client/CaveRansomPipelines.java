package com.wentory.ransom_in_minecraft.client;
import com.mojang.blaze3d.pipeline.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import com.wentory.ransom_in_minecraft.mixin.RansomPipelineRegistry;
import java.util.*;
final class CaveRansomPipelines {
    private static final RenderPipeline CLOUD=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("pipeline/cave_cloud_xray")).withDepthStencilState(Optional.empty()).build());
    private static final RenderType CLOUD_XRAY=RenderType.create("ransom_cave_cloud_xray",RenderSetup.builder(CLOUD).sortOnUpload().createRenderSetup());
    private static final RenderPipeline SPRITE= sprite(false),SPRITE_XRAY=sprite(true);
    private static final Map<Identifier,RenderType> NORMAL=new HashMap<>(),XRAY=new HashMap<>();
    private static RenderPipeline sprite(boolean xray) {
        var builder=RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
                .withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
                .withLocation(id("pipeline/cave_sprite"+(xray?"_xray":"")))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false);
        if(xray) builder.withDepthStencilState(Optional.empty());
        else builder.withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,false));
        return RansomPipelineRegistry.ransom$register(builder.build());
    }
    static RenderType cloud(boolean owner){return owner?CLOUD_XRAY:RenderTypes.debugQuads();}
    static RenderType sprite(Identifier texture,boolean owner) {
        return (owner?XRAY:NORMAL).computeIfAbsent(texture,t->RenderType.create("ransom_cave_sprite",
                RenderSetup.builder(owner?SPRITE_XRAY:SPRITE).withTexture("Sampler0",t).sortOnUpload().createRenderSetup()));
    }
    private static Identifier id(String name){return Identifier.fromNamespaceAndPath("ransom_in_minecraft",name);}
    private CaveRansomPipelines(){}
    static void preload(){}
}
