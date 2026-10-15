package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.StealerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Map;
import com.wentory.ransom_in_minecraft.rules.QteRules.Difficulty;

import net.minecraft.network.FriendlyByteBuf;
@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public record CommonSettingsPayload(boolean stealer, int chestChance, boolean hijack, int recovery, int zombie, int drowned, int creeper, Difficulty difficulty) {
    public static final Class<CommonSettingsPayload> TYPE = CommonSettingsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(CommonSettingsPayload p, FriendlyByteBuf b) {
        b.writeBoolean(p.stealer());
        b.writeVarInt(p.chestChance());
        b.writeBoolean(p.hijack());
        b.writeVarInt(p.recovery());
        b.writeVarInt(p.zombie());
        b.writeVarInt(p.drowned());
        b.writeVarInt(p.creeper());
        b.writeEnum(p.difficulty());
    }
    public static CommonSettingsPayload decode(FriendlyByteBuf b) { return new CommonSettingsPayload(b.readBoolean(), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readEnum(Difficulty.class)); }
    private static Map<ForgeConfigSpec.ConfigValue<?>, Object> remoteValues = Map.of();
    public void accept() {
        remoteValues = Map.of(StealerConfig.ENABLED, stealer, StealerConfig.INFECTION_CHANCE_PERCENT, chestChance,
                StealerConfig.WORM_ENABLED, hijack, StealerConfig.WORM_HEAL_TICKS, recovery,
                StealerConfig.HIJACK_ZOMBIE_CHANCE, zombie, StealerConfig.HIJACK_DROWNED_CHANCE, drowned,
                StealerConfig.HIJACK_CREEPER_CHANCE, creeper, StealerConfig.DIFFICULTY, difficulty);
    }
    @SuppressWarnings("unchecked")
    public static <T> T remoteValue(ForgeConfigSpec.ConfigValue<T> value) {
        return (T) remoteValues.getOrDefault(value, value.get());
    }
    public static CommonSettingsPayload current() {
        return new CommonSettingsPayload(StealerConfig.ENABLED.get(), StealerConfig.INFECTION_CHANCE_PERCENT.get(),
                StealerConfig.WORM_ENABLED.get(), StealerConfig.WORM_HEAL_TICKS.get(),
                StealerConfig.HIJACK_ZOMBIE_CHANCE.get(), StealerConfig.HIJACK_DROWNED_CHANCE.get(),
                StealerConfig.HIJACK_CREEPER_CHANCE.get(), StealerConfig.DIFFICULTY.get());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PacketDistributor.sendToPlayer(player, current());
    }
}
