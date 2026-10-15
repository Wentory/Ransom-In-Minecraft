package com.wentory.ransom_in_minecraft.mixin;
import com.wentory.ransom_in_minecraft.client.CrucifixSpatialSound;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(SoundEngine.class)
public abstract class CrucifixSpatialSoundMixin {
    @ModifyVariable(method="play",at=@At("HEAD"),argsOnly=true)
    private SoundInstance ransom$spatial(SoundInstance instance){return CrucifixSpatialSound.wrap(instance);}
}
