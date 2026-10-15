package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.CaveRansomClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class CaveCameraShakeMixin {
    @Shadow private float yRot,xRot;
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void ransom$shake(float partial,CallbackInfo ci){
        float[] shake=CaveRansomClient.shake();
        setRotation(yRot+shake[0],xRot+shake[1]);
        ((Camera)(Object)this).rotation().rotateZ((float)Math.toRadians(shake[2]));
    }
}
