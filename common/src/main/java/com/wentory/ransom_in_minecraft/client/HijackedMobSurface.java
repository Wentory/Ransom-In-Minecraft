package com.wentory.ransom_in_minecraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import com.wentory.ransom_in_minecraft.mixin.RansomPipelineRegistry;
import java.util.*;
/** Render on the animated parent's exact surface, without growing a second silhouette. */
final class HijackedMobSurface {
    private static final Identifier GLITCH=Identifier.fromNamespaceAndPath("ransom_in_minecraft","textures/glitch.png");
    private static final RenderPipeline PIPELINE=RansomPipelineRegistry.ransom$register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("ransom_in_minecraft","pipeline/hijacked_surface"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL,false,1,1))
            .withCull(false).build());
    private static final Map<Identifier,RenderType> TYPES=new HashMap<>();
    private static RenderType type(Identifier texture){
        if(CrucifixIrisCompatibility.useCompatibleRenderer())return RenderTypes.entityTranslucent(texture);
        return TYPES.computeIfAbsent(texture,t->RenderType.create("hijacked_surface",RenderSetup.builder(PIPELINE).withTexture("Sampler0",t).sortOnUpload().createRenderSetup()));
    }
    static <S extends EntityRenderState> void render(EntityModel<S> model,Identifier skin,PoseStack pose,DeferredBuffers buffers,int light,int tick,int tint){render(model,skin,pose,buffers,light,tick,tint,0xCCFFFFFF);}
    static <S extends EntityRenderState> void render(EntityModel<S> model,Identifier skin,PoseStack pose,DeferredBuffers buffers,int light,int tick,int tint,int noiseColor){
        model.renderToBuffer(pose,buffers.getBuffer(type(skin)),light,0,tint);
        var noise=new AnimatedSheetVertexConsumer(buffers.getBuffer(type(GLITCH)),tick/2,6);
        model.renderToBuffer(pose,noise,0xF000F0,0,noiseColor);
    }
    private HijackedMobSurface(){}
    static void preload(){}
}
