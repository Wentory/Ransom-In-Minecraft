package com.wentory.ransom_in_minecraft;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import java.util.function.Supplier;
public final class RansomFabric implements ModInitializer {
    public static final String MODID="ransom_in_minecraft";
    public static final Supplier<SoundEvent> SPAWN=sound("spawn");
    public static final Supplier<SoundEvent> JUMPSCARE=sound("jumpscare");
    public static final Supplier<SoundEvent> JUMPSCARE2=sound("jumpscare2");
    public static final Supplier<SoundEvent> BOWOMP=sound("bowomp");
    public static final Supplier<SoundEvent> SUCCESS=sound("ransom_success");
    public static final Supplier<SoundEvent> SOUNDTRACK=sound("soundtrack");
    public static final Supplier<SoundEvent> SOUNDTRACK2=sound("soundtrack2");
    public static final Supplier<SoundEvent> SPAM_ENCOUNTER=sound("spam_encounter");
    public static final Supplier<SoundEvent> GLITCH_HIT=sound("glitch_hit");
    public static final Supplier<SoundEvent> PICK_COINS=sound("pick_coins");
    public static final Supplier<SoundEvent> GOT_COINS=sound("got_coins");
    public static final Supplier<SoundEvent> GLITCH=sound("glitch");
    public static final Supplier<SoundEvent> GLITCH2=sound("glitch2");
    public static final Supplier<SoundEvent> FAILTURE=sound("failture");
    public static final Supplier<SoundEvent> STEALER_SUCCESS=sound("stealer_success");
    public static final Supplier<SoundEvent> WINDOW_OPEN=sound("window_open");
    public static final Supplier<SoundEvent> WINDOW_CLOSE=sound("window_close");
    public static final Supplier<SoundEvent> SCANNING=sound("scanning");
    public static final Supplier<SoundEvent> TERMINAL_CLICK=sound("terminal_click");
    public static final Supplier<SoundEvent> LAUGH=sound("laugh");
    public static final Supplier<SoundEvent> ZOMBIE_SAY=sound("zombie_say");
    public static final Supplier<SoundEvent> ZOMBIE_HURT=sound("zombie_hurt");
    public static final Supplier<SoundEvent> ZOMBIE_DEATH=sound("zombie_death");
    public static final Supplier<SoundEvent> ZOMBIE_STEP=sound("zombie_step");
    public static final Supplier<SoundEvent> ZOMBIE_STOP=sound("zombie_stop");
    public static final Supplier<SoundEvent> CREEPER_EXPLODE=sound("creeper_explode");
    public static final Supplier<SoundEvent> CREEPER_TRAIL=sound("creeper_trail");
    public static final Supplier<SoundEvent> DROWNED_IDLE=sound("drowned_idle");
    public static final Supplier<SoundEvent> DROWNED_HURT=sound("drowned_hurt");
    public static final Supplier<SoundEvent> DROWNED_DEATH=sound("drowned_death");
    public static final Supplier<SoundEvent> DROWNED_IDLE_WATER=sound("drowned_idle_water");
    public static final Supplier<SoundEvent> DROWNED_HURT_WATER=sound("drowned_hurt_water");
    public static final Supplier<SoundEvent> DROWNED_DEATH_WATER=sound("drowned_death_water");
    public static final Supplier<SoundEvent> CREEPER_CHARGE=register("creeper_charge",50);
    private static Supplier<SoundEvent> sound(String name) {return register(name,0);}
    private static Supplier<SoundEvent> register(String name,float range) {
        Identifier id=Identifier.fromNamespaceAndPath(MODID,name);
        SoundEvent event=Registry.register(BuiltInRegistries.SOUND_EVENT,id,
                range>0 ? SoundEvent.createFixedRangeEvent(id,range) : SoundEvent.createVariableRangeEvent(id));
        return () -> event;
    }
    public void onInitialize() {
        com.wentory.ransom_in_minecraft.platform.PersistentData.initialize();
        ClientConfig.SPEC.load("ransom_in_minecraft.properties");
        StealerConfig.SPEC.load("ransom_in_minecraft-common.properties");
        com.wentory.ransom_in_minecraft.platform.FabricHooks.initialize();
    }
}
