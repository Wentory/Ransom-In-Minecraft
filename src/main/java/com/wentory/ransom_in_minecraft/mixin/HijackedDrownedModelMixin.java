package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedModel.class)
public abstract class HijackedDrownedModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V", at = @At("TAIL"))
    private void ransom$freezeSwimming(Zombie zombie, float limbSwing, float limbSwingAmount,
                                       float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (!ClientInfectedZombies.contains(zombie.getUUID())) return;
        DrownedModel<?> model = (DrownedModel<?>) (Object) this;
        model.leftArm.xRot = model.rightArm.xRot = -(float) Math.PI / 2.0F;
        model.leftArm.yRot = model.rightArm.yRot = 0.0F;
        model.leftArm.zRot = model.rightArm.zRot = 0.0F;
        model.leftLeg.xRot = model.rightLeg.xRot = 0.0F;
        model.leftLeg.yRot = model.rightLeg.yRot = 0.0F;
        model.leftLeg.zRot = model.rightLeg.zRot = 0.0F;
        model.head.xRot = headPitch * ((float) Math.PI / 180.0F);
    }
}
