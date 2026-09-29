package com.wentory.ransom_in_minecraft.network;

public final class ClientRansomResumeTracker {
    private static RansomResumePayload pending;
    private static RansomFailureScarePayload failureScarePending;
    private static boolean summonPending;
    private static boolean forcedAttackPending;
    private ClientRansomResumeTracker() {}
    public static synchronized void accept(RansomResumePayload payload) { pending = payload; }
    public static synchronized RansomResumePayload consume() {
        RansomResumePayload result = pending;
        pending = null;
        return result;
    }
    public static synchronized void acceptFailureScare(RansomFailureScarePayload payload) {
        failureScarePending = payload;
    }
    public static synchronized RansomFailureScarePayload consumeFailureScare() {
        RansomFailureScarePayload result = failureScarePending;
        failureScarePending = null;
        return result;
    }
    public static synchronized void acceptSummon(RansomSummonPayload payload) {
        if (payload.summon()) summonPending = true;
    }
    public static synchronized boolean consumeSummon() {
        boolean result = summonPending;
        summonPending = false;
        return result;
    }

    public static synchronized void acceptForcedAttack(RansomForcedAttackPayload payload) {
        if (payload.attack()) forcedAttackPending = true;
    }
    public static synchronized boolean consumeForcedAttack() {
        boolean result = forcedAttackPending;
        forcedAttackPending = false;
        return result;
    }

    public static synchronized void clear() {
        pending = null;
        failureScarePending = null;
        summonPending = false;
        forcedAttackPending = false;
    }
}
