package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import com.wentory.ransom_in_minecraft.client.RansomConfigScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(RansomInMinecraft.MODID)
public class RansomInMinecraft {
    public static final String MODID = "ransom_in_minecraft";
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN = sound("spawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUMPSCARE = sound("jumpscare");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUMPSCARE2 = sound("jumpscare2");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWOMP = sound("bowomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUCCESS = sound("ransom_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUNDTRACK = sound("soundtrack");

    public RansomInMinecraft(IEventBus modBus, ModContainer container) {
        SOUNDS.register(modBus);
        modBus.addListener(RansomServerState::registerPayloads);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (mod, parent) -> new RansomConfigScreen(parent));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
