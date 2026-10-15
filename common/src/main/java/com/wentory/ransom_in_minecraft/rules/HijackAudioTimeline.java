package com.wentory.ransom_in_minecraft.rules;

/** Frame-exact intro, loop and ending boundaries, independent of game ticks and loader. */
public final class HijackAudioTimeline {
    public static final double LOOP_START_SECONDS = 2.820;
    public static final double LOOP_END_SECONDS = 36.700;
    public static final double ENDING_START_SECONDS = 69.160;
    private final int loopStart, end;
    private final boolean ending;
    private int position;

    public HijackAudioTimeline(int sampleRate, int frames, boolean ending) {
        loopStart = (int) Math.round(LOOP_START_SECONDS * sampleRate);
        int loopEnd = (int) Math.round(LOOP_END_SECONDS * sampleRate);
        int endingStart = (int) Math.round(ENDING_START_SECONDS * sampleRate);
        if (sampleRate <= 0 || frames <= endingStart || loopStart >= loopEnd) throw new IllegalArgumentException("Audio is shorter than its timeline");
        this.ending = ending;
        position = ending ? endingStart : 0;
        end = ending ? frames : loopEnd;
    }
    public int position() { return position; }
    public int available(int requestedFrames) {
        if (!ending && position == end) position = loopStart;
        return Math.min(Math.max(0, requestedFrames), end - position);
    }
    public void advance(int frames) {
        if (frames < 0 || frames > end - position) throw new IllegalArgumentException("Read crosses a timeline boundary");
        position += frames;
    }
}
