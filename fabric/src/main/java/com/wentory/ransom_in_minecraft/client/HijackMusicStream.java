package com.wentory.ransom_in_minecraft.client;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import java.util.concurrent.CompletableFuture;
interface HijackMusicStream extends FullHijackMusic.PcmLayer,FabricSoundInstance {
    @Override default CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library,Identifier path,boolean loop){return stream();}
}
