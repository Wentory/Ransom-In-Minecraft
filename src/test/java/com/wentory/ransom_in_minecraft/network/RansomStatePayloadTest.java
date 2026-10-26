package com.wentory.ransom_in_minecraft.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public final class RansomStatePayloadTest {
    public static void main(String[] args) {
        var reset = new RansomStatePayload(false, false, 0, 0, false);
        if (reset.success()) throw new AssertionError("An ordinary reset must not award an advancement");
        var cases = new RansomStatePayload[] {
                reset,
                new RansomStatePayload(true, false, 12, 0, false),
                new RansomStatePayload(false, true, 12, 0, true),
                new RansomStatePayload(true, false, 12, 4, false),
                new RansomStatePayload(false, false, 100, 0, false, true)
        };
        for (var original : cases) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                RansomStatePayload.encode(original, buffer);
                var decoded = RansomStatePayload.decode(buffer);
                if (!original.equals(decoded) || buffer.isReadable())
                    throw new AssertionError("Encounter result packet did not round-trip: " + original);
            } finally {
                buffer.release();
            }
        }
        System.out.println("Encounter results: Ransomware victory, active, failure, damage and ordinary reset packets passed");
    }
}
