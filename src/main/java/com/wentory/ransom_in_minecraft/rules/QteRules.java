package com.wentory.ransom_in_minecraft.rules;

public final class QteRules {
    public enum Difficulty { EASY, NORMAL, HARD }
    private QteRules() {}
    public static int keyTicks(int index) {
        return keyTicks(index, Difficulty.NORMAL);
    }
    public static int keyTicks(int index, Difficulty difficulty) {
        if (index < 0 || index >= 10) throw new IllegalArgumentException("Key index must be 0..9");
        int ticks = index < 5 ? 10 : 12 + (index - 5) * 2;
        if (index < 3) {
            if (difficulty == Difficulty.EASY) ticks += index == 0 ? 8 : 2;
            else if (difficulty == Difficulty.HARD) ticks -= 2;
        }
        return ticks;
    }
    public static int roundTicks(int length) {
        return roundTicks(length, Difficulty.NORMAL);
    }
    public static int roundTicks(int length, Difficulty difficulty) {
        if (length < 0 || length > 10) throw new IllegalArgumentException("Round length must be 0..10");
        int ticks = 0;
        for (int index = 0; index < length; index++) ticks += keyTicks(index, difficulty);
        return ticks;
    }
}
