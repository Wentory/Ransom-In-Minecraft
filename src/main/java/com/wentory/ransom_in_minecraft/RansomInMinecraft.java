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
    public static final RegistryObject<SoundEvent> SOUNDTRACK2 = sound("soundtrack2");
    public static final RegistryObject<SoundEvent> SPAM_ENCOUNTER = sound("spam_encounter");
    public static final RegistryObject<SoundEvent> GLITCH_HIT = sound("glitch_hit");
    public static final RegistryObject<SoundEvent> PICK_COINS = sound("pick_coins");
    public static final RegistryObject<SoundEvent> GOT_COINS = sound("got_coins");
    public static final RegistryObject<SoundEvent> GLITCH = sound("glitch");
    public static final RegistryObject<SoundEvent> GLITCH2 = sound("glitch2");
    public static final RegistryObject<SoundEvent> FAILTURE = sound("failture");
    public static final RegistryObject<SoundEvent> STEALER_SUCCESS = sound("stealer_success");
    public static final RegistryObject<SoundEvent> WINDOW_OPEN = sound("window_open");
    public static final RegistryObject<SoundEvent> WINDOW_CLOSE = sound("window_close");
    public static final RegistryObject<SoundEvent> SCANNING = sound("scanning");
    public static final RegistryObject<SoundEvent> TERMINAL_CLICK = sound("terminal_click");
    public static final RegistryObject<SoundEvent> LAUGH = sound("laugh");
    public static final RegistryObject<SoundEvent> ZOMBIE_SAY = sound("zombie_say");
    public static final RegistryObject<SoundEvent> ZOMBIE_HURT = sound("zombie_hurt");
    public static final RegistryObject<SoundEvent> ZOMBIE_DEATH = sound("zombie_death");
    public static final RegistryObject<SoundEvent> ZOMBIE_STEP = sound("zombie_step");
    public static final RegistryObject<SoundEvent> ZOMBIE_STOP = sound("zombie_stop");
    public static final RegistryObject<SoundEvent> CREEPER_EXPLODE = sound("creeper_explode");
    public static final RegistryObject<SoundEvent> CREEPER_TRAIL = sound("creeper_trail");
    public static final RegistryObject<SoundEvent> DROWNED_IDLE = sound("drowned_idle");
    public static final RegistryObject<SoundEvent> DROWNED_HURT = sound("drowned_hurt");
    public static final RegistryObject<SoundEvent> DROWNED_DEATH = sound("drowned_death");
    public static final RegistryObject<SoundEvent> DROWNED_IDLE_WATER = sound("drowned_idle_water");
    public static final RegistryObject<SoundEvent> DROWNED_HURT_WATER = sound("drowned_hurt_water");
    public static final RegistryObject<SoundEvent> DROWNED_DEATH_WATER = sound("drowned_death_water");
    public static final RegistryObject<SoundEvent> CREEPER_CHARGE = SOUNDS.register("creeper_charge",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(MODID, "creeper_charge"), 50.0F));

    public RansomInMinecraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(modBus);
        RansomNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StealerConfig.SPEC);
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
