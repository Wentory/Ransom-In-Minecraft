package com.wentory.ransom_in_minecraft;

import com.mojang.logging.LogUtils;
import com.wentory.ransom_in_minecraft.network.RansomServerState;
import com.wentory.ransom_in_minecraft.client.RansomClientSetup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(RansomInMinecraft.MODID)
public class RansomInMinecraft {
    public static final String MODID = "ransom_in_minecraft";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RANSOMWARE_CONFIG_FILE = "ransom_in_minecraft-ransomware.toml";
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN = sound("spawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUMPSCARE = sound("jumpscare");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUMPSCARE2 = sound("jumpscare2");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWOMP = sound("bowomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUCCESS = sound("ransom_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUNDTRACK = sound("soundtrack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUNDTRACK2 = sound("soundtrack2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAM_ENCOUNTER = sound("spam_encounter");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLITCH_HIT = sound("glitch_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> PICK_COINS = sound("pick_coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOT_COINS = sound("got_coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLITCH = sound("glitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLITCH2 = sound("glitch2");
    public static final DeferredHolder<SoundEvent, SoundEvent> FAILTURE = sound("failture");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEALER_SUCCESS = sound("stealer_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDOW_OPEN = sound("window_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDOW_CLOSE = sound("window_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCANNING = sound("scanning");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERMINAL_CLICK = sound("terminal_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAUGH = sound("laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_SAY = sound("zombie_say");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_HURT = sound("zombie_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_DEATH = sound("zombie_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_STEP = sound("zombie_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOMBIE_STOP = sound("zombie_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CREEPER_CHARGE = SOUNDS.register("creeper_charge",
            () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "creeper_charge"), 50.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> CREEPER_EXPLODE = sound("creeper_explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> CREEPER_TRAIL = sound("creeper_trail");
    public static final DeferredHolder<SoundEvent, SoundEvent> DROWNED_IDLE = sound("drowned_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DROWNED_HURT = sound("drowned_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DROWNED_DEATH = sound("drowned_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DROWNED_IDLE_WATER = sound("drowned_idle_water");
    public static final DeferredHolder<SoundEvent, SoundEvent> DROWNED_HURT_WATER = sound("drowned_hurt_water");
    public static final DeferredHolder<SoundEvent, SoundEvent> DROWNED_DEATH_WATER = sound("drowned_death_water");

    public RansomInMinecraft(IEventBus modBus, ModContainer container) {
        SOUNDS.register(modBus);
        modBus.addListener(RansomServerState::registerPayloads);
        migrateRansomwareConfig();
        container.registerConfig(ModConfig.Type.COMMON, RansomwareConfig.SPEC,
                RANSOMWARE_CONFIG_FILE);
        container.registerConfig(ModConfig.Type.COMMON, StealerConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RansomClientSetup.registerConfigScreen(container);
        }
    }

    private static void migrateRansomwareConfig() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path oldConfig = configDir.resolve("ransom_in_minecraft-client.toml");
        Path newConfig = configDir.resolve(RANSOMWARE_CONFIG_FILE);
        if (!Files.isRegularFile(oldConfig) || Files.exists(newConfig)) return;
        try {
            Files.copy(oldConfig, newConfig);
            LOGGER.info("Migrated Ransomware settings from {} to {}", oldConfig, newConfig);
        } catch (IOException error) {
            LOGGER.warn("Could not migrate old Ransomware settings from {}", oldConfig, error);
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
