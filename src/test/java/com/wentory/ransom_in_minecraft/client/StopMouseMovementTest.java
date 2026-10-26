package com.wentory.ransom_in_minecraft.client;

public final class StopMouseMovementTest {
    public static void main(String[] args) {
        var input=new StopMouseMovement();
        // Sleep screen closes and grabMouse directly recenters xpos/ypos: no physical callback.
        check(!input.consume(),"Automatic cursor recenter must not trigger STOP");
        input.moved(true,true,960,540,300,200);
        check(!input.consume(),"First callback after mouse capture must be ignored like vanilla");
        input.moved(true,false,300,200,303,200);
        input.moved(true,false,303,200,300,200);
        check(input.consume(),"Real motion and return within one tick must still trigger STOP");
        check(!input.consume(),"Movement must be consumed once");
        input.moved(false,false,300,200,500,400);
        check(!input.consume(),"Inactive window must not supply gameplay input");
        input.moved(true,false,300,200,300,200);
        check(!input.consume(),"Same-position callback must not trigger STOP");
        input.moved(true,false,300,200,300,201);
        check(input.consume(),"Real motion immediately after capture must still be detected");
        input.moved(true,false,300,200,305,200);input.clear();
        check(!input.consume(),"New encounter must not inherit earlier input");
        System.out.println("STOP mouse: sleep recenter, capture callback, real movement, focus and reset passed");
    }
    private static void check(boolean condition,String message) {
        if(!condition) throw new AssertionError(message);
    }
}
