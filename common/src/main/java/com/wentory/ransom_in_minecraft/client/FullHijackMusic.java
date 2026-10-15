package com.wentory.ransom_in_minecraft.client;

import com.wentory.ransom_in_minecraft.rules.HijackAudioTimeline;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.sound.sampled.AudioFormat;

/** Both layers use source PCM frames, so looping never waits for a game tick or a new Ogg decoder. */
public final class FullHijackMusic {
    private static CompletableFuture<Decoded[]> decoded;
    private static Object resources;
    private static Layer music, noise;
    private static int generation;
    private FullHijackMusic() {}

    private static CompletableFuture<Decoded[]> preload() {
        var manager = Minecraft.getInstance().getResourceManager();
        if (decoded == null || resources != manager || decoded.isCompletedExceptionally()) {
            resources = manager;
            decoded = CompletableFuture.supplyAsync(() -> {
                try {
                    return new Decoded[]{decode(manager, "soundtrack"), decode(manager, "white_noise")};
                } catch (IOException e) { throw new CompletionException(e); }
            }, Util.nonCriticalIoPool());
        }
        return decoded;
    }
    private static Decoded decode(net.minecraft.server.packs.resources.ResourceProvider resources, String name) throws IOException {
        var path = Identifier.fromNamespaceAndPath("ransom_in_minecraft", "sounds/hijackgame/" + name + ".ogg");
        try (var input = resources.open(path); var audio = new JOrbisAudioStream(input)) {
            ByteBuffer buffer = audio.readAll();
            byte[] pcm = new byte[buffer.remaining()];
            buffer.get(pcm);
            var result = new Decoded(audio.getFormat(), pcm);
            new HijackAudioTimeline(Math.round(result.format.getSampleRate()), pcm.length / result.format.getFrameSize(), true);
            return result;
        }
    }
    static void start() { play(false); }
    static void finish() { play(true); }
    private static void play(boolean ending) {
        stop();
        int request = generation;
        preload().whenComplete((tracks, error) -> Minecraft.getInstance().execute(() -> {
            if (request != generation) return;
            if (error != null) {
                com.mojang.logging.LogUtils.getLogger().error("Unable to load full Hijack audio", error);
                return;
            }
            music = new Layer("hijack_soundtrack", tracks[0], ending);
            noise = new Layer("hijack_white_noise", tracks[1], ending);
            var sounds = Minecraft.getInstance().getSoundManager();
            sounds.play(music);
            sounds.play(noise);
            if (!ending) FullHijackClient.musicStarted();
        }));
    }
    public static void stop() {
        generation++;
        var sounds = Minecraft.getInstance().getSoundManager();
        if (music != null) sounds.stop(music);
        if (noise != null) sounds.stop(noise);
        music = noise = null;
    }
    public static void login() { preload(); }
    public static void logout() { stop(); decoded = null; resources = null; }
    public interface PcmLayer { CompletableFuture<AudioStream> stream(); }

    private record Decoded(AudioFormat format, byte[] pcm) {}
    private static final class Layer extends AbstractSoundInstance implements HijackMusicStream {
        private final Decoded decoded;
        private final boolean ending;
        Layer(String name, Decoded decoded, boolean ending) {
            super(Identifier.fromNamespaceAndPath("ransom_in_minecraft", name), SoundSource.MASTER, RandomSource.create());
            this.decoded = decoded;
            this.ending = ending;
            // -4 dB amplitude gain, shared by soundtrack and white noise, including the ending.
            volume = 0.6309573F;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            looping = !ending;
        }
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean loop) {
            return stream();
        }
        @Override public CompletableFuture<AudioStream> stream() {
            return CompletableFuture.completedFuture(new PcmStream(decoded, ending));
        }
    }
    private static final class PcmStream implements AudioStream {
        private final Decoded decoded;
        private final HijackAudioTimeline timeline;
        private boolean closed;
        PcmStream(Decoded decoded, boolean ending) {
            this.decoded = decoded;
            timeline = new HijackAudioTimeline(Math.round(decoded.format.getSampleRate()),
                    decoded.pcm.length / decoded.format.getFrameSize(), ending);
        }
        @Override public AudioFormat getFormat() { return decoded.format; }
        @Override public ByteBuffer read(int bytes) {
            int frameSize = decoded.format.getFrameSize();
            ByteBuffer result = ByteBuffer.allocateDirect(bytes - bytes % frameSize);
            while (!closed && result.remaining() >= frameSize) {
                int frames = timeline.available(result.remaining() / frameSize);
                if (frames == 0) break;
                result.put(decoded.pcm, timeline.position() * frameSize, frames * frameSize);
                timeline.advance(frames);
            }
            return result.flip();
        }
        @Override public void close() { closed = true; }
    }
}
