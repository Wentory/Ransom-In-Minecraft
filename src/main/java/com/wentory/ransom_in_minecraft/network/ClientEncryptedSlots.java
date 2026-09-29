package com.wentory.ransom_in_minecraft.network;

public final class ClientEncryptedSlots {
    private static int mask;
    private static boolean glitchHitPending;

    private ClientEncryptedSlots() {}

    public static synchronized void accept(EncryptedSlotsPayload payload) {
        mask = payload.mask();
        if (payload.glitchHit()) glitchHitPending = true;
    }

    public static synchronized boolean isEncrypted(int inventorySlot) {
        int bit = inventorySlot - 9;
        return bit >= 0 && bit < 27 && (mask & 1 << bit) != 0;
    }

    public static synchronized boolean consumeGlitchHit() {
        boolean result = glitchHitPending;
        glitchHitPending = false;
        return result;
    }

    public static synchronized void clear() {
        mask = 0;
        glitchHitPending = false;
    }
}
