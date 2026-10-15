package com.wentory.ransom_in_minecraft.network;

import java.util.function.Consumer;

/** Common-side dispatch without references to Minecraft client classes. */
public final class CrucifixClientPayloads {
    private static Consumer<CrucifixCirclePayload> circles = payload -> {};
    private static Consumer<MobBanishmentPayload> mobs = payload -> {};

    private CrucifixClientPayloads() {}

    public static void bind(Consumer<CrucifixCirclePayload> circleReceiver,
                            Consumer<MobBanishmentPayload> mobReceiver) {
        circles = circleReceiver;
        mobs = mobReceiver;
    }

    public static void accept(CrucifixCirclePayload payload) {
        circles.accept(payload);
    }

    public static void accept(MobBanishmentPayload payload) {
        mobs.accept(payload);
    }
}
