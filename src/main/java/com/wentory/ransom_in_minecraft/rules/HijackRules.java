package com.wentory.ransom_in_minecraft.rules;

public final class HijackRules {
    private HijackRules() {}
    public static int clamp(int value) {
        //? if >=1.20.5 {
        /*return Math.clamp(value, 0, 100);
        *///? } else {
        return Math.max(0, Math.min(100, value));
        //? }
    }
    public static int level(int value) {
        value = clamp(value);
        if (value == 0) return 0;
        if (value >= 100) return 5;
        if (value >= 75) return 4;
        if (value >= 50) return 3;
        if (value >= 25) return 2;
        return 1;
    }
}
