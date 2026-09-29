package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.monster.zombie.AbstractZombieModel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public abstract class InfectedZombieModelMixin {
    @Unique private boolean ransom$shakingLastRender;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;)V", at = @At("HEAD"))
    private void ransom$resetShake(net.minecraft.client.renderer.entity.state.ZombieRenderState state, CallbackInfo callback) {
        if (!ransom$shakingLastRender) return;
        AbstractZombieModel<net.minecraft.client.renderer.entity.state.ZombieRenderState> model = (AbstractZombieModel<net.minecraft.client.renderer.entity.state.ZombieRenderState>) (Object) this;
        model.body.zRot = model.head.zRot = 0.0F;
        model.leftArm.zRot = model.rightArm.zRot = 0.0F;
        ransom$shakingLastRender = false;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;)V", at = @At("TAIL"))
    private void ransom$freezeInfectedZombie(net.minecraft.client.renderer.entity.state.ZombieRenderState state, CallbackInfo callback) {
        if (!(com.wentory.ransom_in_minecraft.client.RenderStateEntities.get(state) instanceof Zombie zombie)) return;
        AbstractZombieModel<net.minecraft.client.renderer.entity.state.ZombieRenderState> model = (AbstractZombieModel<net.minecraft.client.renderer.entity.state.ZombieRenderState>) (Object) this;
        if (ClientInfectedZombies.isWormVictim(zombie.getUUID())) {
            float shake = (float) Math.sin(state.ageInTicks * 23.0F) * 0.075F;
            model.body.zRot = shake;
            model.head.zRot = shake * 0.7F;
            model.leftArm.zRot -= shake;
            model.rightArm.zRot += shake;
            ransom$shakingLastRender = true;
            return;
        }
        if (!ClientInfectedZombies.contains(zombie.getUUID())) return;
        model.body.zRot = model.head.zRot = 0.0F;
        model.leftArm.xRot = -(float) Math.PI / 2.0F;
        model.rightArm.xRot = -(float) Math.PI / 2.0F;
        model.leftArm.yRot = model.rightArm.yRot = 0.0F;
        model.leftArm.zRot = model.rightArm.zRot = 0.0F;
        model.leftLeg.xRot = model.rightLeg.xRot = 0.0F;
        model.leftLeg.yRot = model.rightLeg.yRot = 0.0F;
        model.leftLeg.zRot = model.rightLeg.zRot = 0.0F;
    }
}
