package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public final class RansomFabric implements ModInitializer {
    public static final String MODID = "ransom_in_minecraft";
    public static final Supplier<SoundEvent> SPAWN = sound("spawn");
    public static final Supplier<SoundEvent> JUMPSCARE = sound("jumpscare");
    public static final Supplier<SoundEvent> JUMPSCARE2 = sound("jumpscare2");
    public static final Supplier<SoundEvent> BOWOMP = sound("bowomp");
    public static final Supplier<SoundEvent> SUCCESS = sound("ransom_success");
    public static final Supplier<SoundEvent> SOUNDTRACK = sound("soundtrack");

    private static Supplier<SoundEvent> sound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
        SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, id,
                SoundEvent.createVariableRangeEvent(id));
        return () -> event;
    }

    @Override
    public void onInitialize() {
        RansomServerState.initialize();
    }
}
