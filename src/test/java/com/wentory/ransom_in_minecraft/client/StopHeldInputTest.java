package com.wentory.ransom_in_minecraft.client;

import java.util.HashSet;
import java.util.Set;

public final class StopHeldInputTest {
    public static void main(String[] args) {
        Set<Integer> keys = new HashSet<>(), scans = new HashSet<>(), buttons = new HashSet<>();
        var keyboard = StopHeldInput.Device.KEYBOARD;
        var mouse = StopHeldInput.Device.MOUSE;
        // Toggle actions can remain enabled throughout this sequence; only hardware changes.
        keys.add(341);
        check(StopHeldInput.held(true, keyboard, 341, keys::contains, scans::contains, buttons::contains), "Held sprint key must count");
        keys.clear();
        check(!StopHeldInput.held(true, keyboard, 341, keys::contains, scans::contains, buttons::contains), "Released toggle sprint must not count");
        check(!StopHeldInput.held(true, keyboard, 340, keys::contains, scans::contains, buttons::contains), "Latched sneak must not count");
        buttons.add(4);
        check(StopHeldInput.held(true, mouse, 4, keys::contains, scans::contains, buttons::contains), "Rebound sprint on a side mouse button must count");
        check(!StopHeldInput.held(true, keyboard, 4, keys::contains, scans::contains, buttons::contains), "Mouse and keyboard codes must not be confused");
        check(!StopHeldInput.held(false, mouse, 4, keys::contains, scans::contains, buttons::contains), "Inactive window must not count");
        scans.add(17);
        check(StopHeldInput.held(true, StopHeldInput.Device.SCANCODE, 17, keys::contains, scans::contains, buttons::contains), "Scancode binding must use physical scan input");
        buttons.clear();
        check(!StopHeldInput.held(true, mouse, 4, keys::contains, scans::contains, buttons::contains), "Released mouse button must not count");
        check(!StopHeldInput.held(true, keyboard, -1, i -> { throw new AssertionError("Unbound key polled"); }, scans::contains, buttons::contains), "Unbound input must be ignored");
        System.out.println("STOP held input: toggle release, physical hold, mouse rebinding, scancodes, focus and unbound controls passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
