package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedModel.class)
public abstract class HijackedDrownedModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;)V", at = @At("TAIL"))
    private void ransom$freezeSwimming(net.minecraft.client.renderer.entity.state.ZombieRenderState state, CallbackInfo ci) {
        if (!(com.wentory.ransom_in_minecraft.client.RenderStateEntities.get(state) instanceof Zombie zombie) || !ClientInfectedZombies.contains(zombie.getUUID())) return;
        DrownedModel model = (DrownedModel) (Object) this;
        model.leftArm.xRot = model.rightArm.xRot = -(float) Math.PI / 2.0F;
        model.leftArm.yRot = model.rightArm.yRot = 0.0F;
        model.leftArm.zRot = model.rightArm.zRot = 0.0F;
        model.leftLeg.xRot = model.rightLeg.xRot = 0.0F;
        model.leftLeg.yRot = model.rightLeg.yRot = 0.0F;
        model.leftLeg.zRot = model.rightLeg.zRot = 0.0F;
        model.head.xRot = state.xRot * ((float) Math.PI / 180.0F);
    }
}
