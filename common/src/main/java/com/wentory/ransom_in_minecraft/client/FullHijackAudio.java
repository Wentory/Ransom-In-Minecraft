package com.wentory.ransom_in_minecraft.client;

import java.util.Set;
import net.minecraft.client.resources.sounds.SoundInstance;

/** Per-instance mix: do not alter the user's category or master volume settings. */
public final class FullHijackAudio {
    private static final float EXTERNAL_GAIN = .2F;
    private static final Set<String> GAME_SOUNDS = Set.of("hijack_soundtrack", "hijack_white_noise",
            "hijack_ring", "hijack_ring_success", "hijack_ring_fail",
            "hijack_target_teleport", "hijack_cursor_teleport");

    private FullHijackAudio() {}

    public static boolean ducking() { return FullHijackClient.active() || FullHijackClient.exiting(); }

    public static float gain(SoundInstance sound) {
        if (!ducking()) return 1;
        var id = sound.getIdentifier();
        return id.getNamespace().equals("ransom_in_minecraft") && GAME_SOUNDS.contains(id.getPath())
                ? 1 : EXTERNAL_GAIN;
    }
}
