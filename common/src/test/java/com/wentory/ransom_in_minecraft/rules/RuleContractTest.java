package com.wentory.ransom_in_minecraft.rules;

public final class RuleContractTest {
    public static void main(String[] args) {
        equal(0, QteRules.roundTicks(0)); // Empty screen state must remain safe.
        int[] durations = {30, 40, 50, 62, 76, 92, 110, 130};
        for (int i = 0; i < durations.length; i++) equal(durations[i], QteRules.roundTicks(i + 3));
        for (int length = 3; length <= 10; length++) {
            equal(QteRules.roundTicks(length) + 12, QteRules.roundTicks(length, QteRules.Difficulty.EASY));
            equal(QteRules.roundTicks(length) - 6, QteRules.roundTicks(length, QteRules.Difficulty.HARD));
        }
        equal(18, QteRules.keyTicks(0, QteRules.Difficulty.EASY));
        equal(12, QteRules.keyTicks(1, QteRules.Difficulty.EASY));
        equal(12, QteRules.keyTicks(2, QteRules.Difficulty.EASY));
        for (int index = 0; index < 3; index++) equal(8, QteRules.keyTicks(index, QteRules.Difficulty.HARD));
        for (int index = 3; index < 10; index++) {
            equal(QteRules.keyTicks(index), QteRules.keyTicks(index, QteRules.Difficulty.EASY));
            equal(QteRules.keyTicks(index), QteRules.keyTicks(index, QteRules.Difficulty.HARD));
        }
        equal(10, QteRules.keyTicks(4));
        equal(12, QteRules.keyTicks(5));
        equal(20, QteRules.keyTicks(9));
        int[] values = {Integer.MIN_VALUE, -1, 0, 1, 24, 25, 49, 50, 74, 75, 99, 100, Integer.MAX_VALUE};
        int[] levels = {0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
        for (int i = 0; i < values.length; i++) equal(levels[i], HijackRules.level(values[i]));
        equal(0, HijackRules.clamp(Integer.MIN_VALUE));
        equal(100, HijackRules.clamp(Integer.MAX_VALUE));
        resistance();
        audioTimeline();
        System.out.println("QTE and Hijack contracts passed");
    }

    private static void audioTimeline() {
        int rate = 44100;
        HijackAudioTimeline loop = new HijackAudioTimeline(rate, rate * 74, false);
        equal(0, loop.position());
        int introAndLoop = (int) Math.round(36.700 * rate);
        equal(introAndLoop, loop.available(Integer.MAX_VALUE));
        loop.advance(introAndLoop);
        int loopLength = (int) Math.round(36.700 * rate) - (int) Math.round(2.820 * rate);
        equal(loopLength, loop.available(Integer.MAX_VALUE));
        equal((int) Math.round(2.820 * rate), loop.position());
        loop.advance(loopLength);
        equal(loopLength, loop.available(Integer.MAX_VALUE));
        equal((int) Math.round(2.820 * rate), loop.position());
        HijackAudioTimeline ending = new HijackAudioTimeline(rate, rate * 74, true);
        equal((int) Math.round(69.160 * rate), ending.position());
        int tail = ending.available(Integer.MAX_VALUE);
        ending.advance(tail);
        equal(0, ending.available(100));
        System.out.println("Hijack audio timeline contracts passed");
    }

    private static void resistance() {
        ResistanceRules ignored = new ResistanceRules(1);
        ignored.progress = .2F;
        ignored.ticks = ResistanceRules.PULSE_MATCH - 1;
        ignored.tick(0, 0, false);
        if (ignored.progress <= .2F) throw new AssertionError("Ignoring a timing ring must not penalize holding");
        ResistanceRules perfect = new ResistanceRules(1);
        perfect.ticks = ResistanceRules.PULSE_MATCH - 1;
        perfect.tick(0, 0, true);
        equal(2, perfect.feedback);
        ResistanceRules good = new ResistanceRules(1);
        good.ticks = ResistanceRules.PULSE_MATCH + ResistanceRules.GOOD_WINDOW - 1;
        good.tick(0, 0, true);
        equal(1, good.feedback);
        if (perfect.progress <= good.progress) throw new AssertionError("Perfect timing must reward more");
        float old = perfect.progress;
        perfect.tick(0, 0, true);
        equal(-1, perfect.feedback);
        if (perfect.progress >= old) throw new AssertionError("Click spam must not farm bonuses");
        ResistanceRules outside = new ResistanceRules(1);
        outside.cursorX = .95F;
        outside.progress = .2F;
        outside.ticks = ResistanceRules.PULSE_MATCH - 1;
        outside.tick(0, 0, true);
        equal(-1, outside.feedback);
        if (outside.progress >= .2F - ResistanceRules.MISS_PENALTY) throw new AssertionError("Outside click must lose progress");
        ResistanceRules unlock = new ResistanceRules(2);
        unlock.progress = ResistanceRules.INVENTORY_THRESHOLD;
        unlock.tick(0, 0, false);
        if (!unlock.inventoryUnlocked) throw new AssertionError("Inventory threshold must unlock");
        unlock.cursorX = .95F;
        unlock.progress = 0;
        unlock.tick(0, 0, true);
        if (!unlock.inventoryUnlocked || unlock.progress < 0) throw new AssertionError("Unlocked inventory must stay available");
        ResistanceRules hold = new ResistanceRules(3);
        for (int i = 0; i < 660; i++) {
            hold.cursorX = hold.targetX;
            hold.cursorY = hold.targetY;
            hold.tick(0, 0, false);
        }
        if (hold.progress < 1) throw new AssertionError("Holding alone must allow escape, without optional clicks");
        if (hold.cursorX < 0 || hold.cursorX > 1 || hold.cursorY < 0 || hold.cursorY > 1)
            throw new AssertionError("Marker must remain within arena");
        ResistanceRules frozen = new ResistanceRules(4);
        frozen.progress = .5F;
        frozen.inventoryUnlocked = true;
        for (int i = 0; i < 300; i++) {
            frozen.tick(.3F, -.3F, true, true);
            if (frozen.cursorX != .5F || frozen.cursorY != .5F || frozen.feedback != 0 || (frozen.soundEvents & 2) != 0)
                throw new AssertionError("Inventory must freeze input, jitter and kicks, including clicks");
        }
        if (frozen.ticks != 300 || frozen.progress == .5F || frozen.targetX == .5F && frozen.targetY == .5F)
            throw new AssertionError("Inventory must not pause the resistance simulation");
        frozen.tick(.1F, 0, false);
        if (frozen.cursorX == .5F) throw new AssertionError("Marker must resume after inventory closes");
        System.out.println("Resistance contracts passed");
    }
    private static void equal(int expected, int actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
}
