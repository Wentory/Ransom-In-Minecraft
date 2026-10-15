package com.wentory.ransom_in_minecraft.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
record WorldEffectFrame(PoseStack getPoseStack,DeferredBuffers buffers,LevelRenderState state) {
    WorldEffectFrame(PoseStack pose,SubmitNodeCollector collector,LevelRenderState state){this(pose,new DeferredBuffers(collector),state);}
    Camera getCamera(){return new Camera(state.cameraRenderState);}
    float partial(){return net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);}
    record Camera(net.minecraft.client.renderer.state.level.CameraRenderState state){
        Vec3 getPosition(){return state.pos;}
        float getYRot(){return state.yRot;}
        org.joml.Quaternionf rotation(){return state.orientation;}
        org.joml.Vector3f getLookVector(){return new org.joml.Vector3f(0,0,-1).rotate(state.orientation);}
    }
}
