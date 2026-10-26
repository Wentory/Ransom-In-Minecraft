package com.wentory.ransom_in_minecraft.client;

import java.util.function.IntPredicate;

/** Reads physical controls, never the latched state of a game action. */
public final class StopHeldInput {
    public enum Device { KEYBOARD, SCANCODE, MOUSE }

    public static boolean held(boolean focused, Device device, int code,
                               IntPredicate keyboard, IntPredicate scancode, IntPredicate mouse) {
        if (!focused || code < 0) return false;
        return switch (device) {
            case KEYBOARD -> keyboard.test(code);
            case SCANCODE -> scancode.test(code);
            case MOUSE -> mouse.test(code);
        };
    }

    private StopHeldInput() {}
}
