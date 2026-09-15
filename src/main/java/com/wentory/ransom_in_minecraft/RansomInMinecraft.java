package com.wentory.ransom_in_minecraft;

import com.wentory.ransom_in_minecraft.network.RansomServerState;
import com.wentory.ransom_in_minecraft.network.RansomNetwork;
import com.wentory.ransom_in_minecraft.client.RansomConfigScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(RansomInMinecraft.MODID)
public class RansomInMinecraft {
    public static final String MODID = "ransom_in_minecraft";
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final RegistryObject<SoundEvent> SPAWN = sound("spawn");
    public static final RegistryObject<SoundEvent> JUMPSCARE = sound("jumpscare");
    public static final RegistryObject<SoundEvent> JUMPSCARE2 = sound("jumpscare2");
    public static final RegistryObject<SoundEvent> BOWOMP = sound("bowomp");
    public static final RegistryObject<SoundEvent> SUCCESS = sound("ransom_success");
    public static final RegistryObject<SoundEvent> SOUNDTRACK = sound("soundtrack");

    public RansomInMinecraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(modBus);
        RansomNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new RansomConfigScreen(parent)));
        }
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        ResourceLocation id = new ResourceLocation(MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
