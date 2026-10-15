package com.wentory.ransom_in_minecraft.client;
import com.mojang.blaze3d.pipeline.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import com.wentory.ransom_in_minecraft.mixin.RansomPipelineRegistry;
import java.util.*;
final class CrucifixPipelines {
    private static final RenderPipeline MOONLIGHT=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(id("pipeline/crucifix_moonlight"))
            .withVertexShader(id("core/moonlight")).withFragmentShader(id("core/moonlight"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,false)).withCull(false).build());
    private static final RenderPipeline UNLIT=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("pipeline/crucifix_unlit")).withVertexShader(id("core/moonlight"))
            .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,true)).withCull(false).build());
    private static final Map<Identifier,RenderType> TYPES=new HashMap<>(),UNLIT_TYPES=new HashMap<>();
    private static final RenderPipeline BLOCK_LIGHT=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withVertexShader(id("core/moonlight"))
            .withLocation(id("pipeline/crucifix_block_light")).withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,false,1,1)).build());
    private static final RenderType BLOCK_LIGHT_TYPE=RenderType.create("crucifix_block_light",RenderSetup.builder(BLOCK_LIGHT).withTexture("Sampler0",net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).createRenderSetup());
    private static final RenderPipeline CHEST_LIGHT=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
            .withLocation(id("pipeline/crucifix_chest_light")).withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,false,1,1)).build());
    private static final Map<Identifier,RenderType> CHESTS=new HashMap<>();
    static RenderType blockLight(){return BLOCK_LIGHT_TYPE;}
    static RenderType chestLight(Identifier texture){return CHESTS.computeIfAbsent(texture,t->RenderType.create("crucifix_chest_light",RenderSetup.builder(CHEST_LIGHT).withTexture("Sampler0",t).createRenderSetup()));}
    static RenderType moonlight(Identifier texture){
        if(CrucifixIrisCompatibility.useCompatibleRenderer())return RenderTypes.entityTranslucent(texture);
        return TYPES.computeIfAbsent(texture,t->RenderType.create("crucifix_moonlight",RenderSetup.builder(MOONLIGHT).withTexture("Sampler0",t).sortOnUpload().createRenderSetup()));
    }
    static RenderType unlit(Identifier texture){
        if(CrucifixIrisCompatibility.useCompatibleRenderer())return RenderTypes.entityTranslucent(texture);
        return UNLIT_TYPES.computeIfAbsent(texture,t->RenderType.create("crucifix_unlit",RenderSetup.builder(UNLIT).withTexture("Sampler0",t).sortOnUpload().createRenderSetup()));
    }
    private static Identifier id(String name){return Identifier.fromNamespaceAndPath("ransom_in_minecraft",name);}
    private CrucifixPipelines(){}
    static void preload(){}
}
