package com.wentory.ransom_in_minecraft;

/** Server ticks shared by the gameplay timer and the visual outro. */
public final class CrucifixBanishmentTiming {
    public static final int CAPTURE_END = 100;
    public static final int SHINE_FADE_TICKS = 10;
    public static final int OUTER_FADE_START = CAPTURE_END;
    public static final int CIRCLE_FADE_TICKS = 14;
    public static final int INNER_FADE_START = CAPTURE_END + 4;
    public static final int MOON_FADE_START = INNER_FADE_START + CIRCLE_FADE_TICKS;
    public static final int MOON_FADE_TICKS = 30;
    public static final int VISUAL_END = MOON_FADE_START + MOON_FADE_TICKS;
    public static final int LIGHT_END = VISUAL_END + 40;

    private CrucifixBanishmentTiming() {}

    public static float fade(float age, int start, int duration) {
        return 1 - Math.max(0, Math.min(1, (age - start) / duration));
    }
}
