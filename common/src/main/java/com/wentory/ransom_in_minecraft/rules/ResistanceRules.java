package com.wentory.ransom_in_minecraft.rules;

import java.util.Random;

/** Loader-independent, server-authoritative concentration simulation. Coordinates are arena units. */
public final class ResistanceRules {
    public static final float TARGET_RADIUS = 0.105F;
    public static final float MARKER_RADIUS = 0.045F;
    public static final float HOLD_GAIN = 1F / 600;
    public static final float OUTSIDE_LOSS = 1F / 1600;
    public static final float INVENTORY_THRESHOLD = 0.30F;
    public static final float PERFECT_BONUS = 0.055F;
    public static final float GOOD_BONUS = 0.025F;
    public static final float MISS_PENALTY = 0.03F;
    public static final int PULSE_PERIOD = 90;
    public static final int PULSE_START = 25;
    public static final int PULSE_MATCH = 65;
    public static final int PULSE_END = 82;
    public static final int PERFECT_WINDOW = 2;
    public static final int GOOD_WINDOW = 5;
    public static final int TELEPORT_WARNING = 8;
    public static final int RECOVERED_HIJACK = 50;
    public static final int HONEY_RECOVERY = 15;
    private static final float TARGET_MOVE_EASING = 0.14F;
    private static final float TARGET_SHAKE = 0.006F;

    private final Random random;
    public float cursorX = 0.5F, cursorY = 0.5F;
    public float targetX = 0.5F, targetY = 0.5F;
    public float progress;
    public boolean inventoryUnlocked;
    public int ticks;
    public int glitchTicks;
    public int feedback;
    public int soundEvents;
    private float destinationX = 0.5F, destinationY = 0.5F;
    private float centerX = 0.5F, centerY = 0.5F;
    private float nextX = 0.5F, nextY = 0.5F;
    private int moveAt = 50;
    private int kickAt = 80;
    private boolean teleport;
    private boolean pulseUsed;

    public ResistanceRules(long seed) { random = new Random(seed); }

    public static float timingRingScale(int pulseTick) {
        if (pulseTick < PULSE_START || pulseTick >= PULSE_END) return 0;
        float elapsed = (pulseTick - PULSE_START) / (float) (PULSE_END - PULSE_START);
        float match = (PULSE_MATCH - PULSE_START) / (float) (PULSE_END - PULSE_START);
        return (1 - elapsed * elapsed) / (1 - match * match);
    }

    public void tick(float dx, float dy, boolean click) {
        tick(dx, dy, click, false);
    }

    public void tick(float dx, float dy, boolean click, boolean cursorFrozen) {
        ticks++;
        feedback = 0;
        soundEvents = 0;
        if (glitchTicks > 0) glitchTicks--;
        if (ticks == moveAt - TELEPORT_WARNING) {
            nextX = 0.18F + random.nextFloat() * 0.64F;
            nextY = 0.18F + random.nextFloat() * 0.64F;
            teleport = random.nextInt(6) == 0;
            if (teleport) glitchTicks = TELEPORT_WARNING + 3;
        }
        if (ticks >= moveAt) {
            destinationX = nextX;
            destinationY = nextY;
            if (teleport) { centerX = destinationX; centerY = destinationY; soundEvents |= 1; }
            moveAt = ticks + 35 + random.nextInt(31);
            teleport = false;
        }
        centerX += (destinationX - centerX) * TARGET_MOVE_EASING;
        centerY += (destinationY - centerY) * TARGET_MOVE_EASING;
        // Shake the actual hit region, without accumulating drift in its destination.
        targetX = centerX + (random.nextFloat() - 0.5F) * TARGET_SHAKE;
        targetY = centerY + (random.nextFloat() - 0.5F) * TARGET_SHAKE;
        if (!cursorFrozen) {
            cursorX += clamp(dx, -0.35F, 0.35F);
            cursorY += clamp(dy, -0.35F, 0.35F);
            float angle = (float) Math.atan2(cursorY - targetY, cursorX - targetX);
            if (distanceSquared() < 0.00001F) angle = ticks * 0.17F;
            float push = 0.0022F + progress * 0.0014F;
            cursorX += (float) Math.cos(angle) * push;
            cursorY += (float) Math.sin(angle) * push;
            cursorX += (random.nextFloat() - 0.5F) * 0.009F;
            cursorY += (random.nextFloat() - 0.5F) * 0.009F;
            if (ticks >= kickAt) {
                soundEvents |= 2;
                float kickAngle = random.nextFloat() * (float) (Math.PI * 2);
                cursorX += (float) Math.cos(kickAngle) * 0.16F;
                cursorY += (float) Math.sin(kickAngle) * 0.16F;
                kickAt = ticks + 90 + random.nextInt(71);
                glitchTicks = Math.max(glitchTicks, 3);
            }
            cursorX = clamp(cursorX, 0.02F, 0.98F);
            cursorY = clamp(cursorY, 0.02F, 0.98F);
        } else if (ticks >= kickAt) {
            kickAt = ticks + 90 + random.nextInt(71);
        }
        // Judge holding by the marker's center, allowing roughly half of it outside.
        float clearance = TARGET_RADIUS;
        boolean inside = distanceSquared() <= clearance * clearance;
        progress = clamp(progress + (inside ? HOLD_GAIN : -OUTSIDE_LOSS), 0, 1);
        int pulse = ticks % PULSE_PERIOD;
        if (pulse == 0) pulseUsed = false;
        if (click && !cursorFrozen) {
            int error = Math.abs(pulse - PULSE_MATCH);
            if (!pulseUsed && inside && pulse >= PULSE_START && error <= GOOD_WINDOW) {
                progress = clamp(progress + (error <= PERFECT_WINDOW ? PERFECT_BONUS : GOOD_BONUS), 0, 1);
                feedback = error <= PERFECT_WINDOW ? 2 : 1;
                pulseUsed = true;
            } else {
                progress = clamp(progress - MISS_PENALTY, 0, 1);
                feedback = -1;
            }
        }
        if (progress >= INVENTORY_THRESHOLD) inventoryUnlocked = true;
    }

    private float distanceSquared() {
        float dx = cursorX - targetX, dy = cursorY - targetY;
        return dx * dx + dy * dy;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
