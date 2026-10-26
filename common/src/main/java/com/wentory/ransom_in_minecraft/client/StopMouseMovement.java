package com.wentory.ransom_in_minecraft.client;

/** Accumulates real cursor callbacks; never compares coordinates across screen transitions. */
public final class StopMouseMovement {
    private boolean pending;
    public void moved(boolean activeWindow, boolean ignoreFirstMove,
                      double previousX, double previousY, double x, double y) {
        if (activeWindow && !ignoreFirstMove
                && (Math.abs(x-previousX)>.01D || Math.abs(y-previousY)>.01D)) pending=true;
    }
    public boolean consume() {
        boolean result=pending;
        pending=false;
        return result;
    }
    public void clear() { pending=false; }
}
