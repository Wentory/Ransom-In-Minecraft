package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.InfectedCreeperVisuals;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperModel.class)
public abstract class InfectedCreeperModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void ransom$shake(Entity entity, float swing, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        CreeperModel<?> model = (CreeperModel<?>) (Object) this;
        ModelPart root = model.root();
        root.getChild("body").zRot = root.getChild("head").zRot = 0;
        boolean victim = ClientInfectedZombies.isWormVictim(entity.getUUID());
        if (InfectedCreeperVisuals.phase(entity.getUUID()) < 0 && !victim) return;
        for (String name : new String[]{"right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg"}) {
            root.getChild(name).xRot = 0;
        }
        float shake = InfectedCreeperVisuals.phase(entity.getUUID()) == 1 || victim
                ? (float) Math.sin(age * 21.0F) * 0.075F : 0;
        root.getChild("body").zRot = shake;
        root.getChild("head").zRot = shake;
    }
}
