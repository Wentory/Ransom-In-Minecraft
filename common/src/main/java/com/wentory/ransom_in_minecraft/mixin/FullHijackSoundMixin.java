package com.wentory.ransom_in_minecraft.mixin;

import com.wentory.ransom_in_minecraft.client.FullHijackAudio;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class FullHijackSoundMixin {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow protected abstract float calculateVolume(SoundInstance sound);
    @Shadow protected abstract float calculateVolume(float volume, SoundSource source);
    @Unique private boolean ransom$ducking;

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void ransom$mixExistingSound(SoundInstance sound, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(callback.getReturnValueF() * FullHijackAudio.gain(sound));
    }

    @Redirect(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float ransom$mixNewSound(SoundEngine engine, float volume, SoundSource source, SoundInstance sound) {
        return calculateVolume(volume, source) * FullHijackAudio.gain(sound);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void ransom$refreshMix(boolean paused, CallbackInfo callback) {
        boolean ducking = FullHijackAudio.ducking();
        if (ducking == ransom$ducking) return;
        ransom$ducking = ducking;
        // Static sounds also need their already-playing channels updated on entry and exit.
        instanceToChannel.forEach((sound, handle) -> {
            float volume = calculateVolume(sound);
            handle.execute(channel -> channel.setVolume(volume));
        });
    }
}
