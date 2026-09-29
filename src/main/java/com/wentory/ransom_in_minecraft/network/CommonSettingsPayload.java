package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.FriendlyByteBuf;
import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import com.wentory.ransom_in_minecraft.StealerConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;

@EventBusSubscriber(modid = RansomInMinecraft.MODID)
public record CommonSettingsPayload(boolean stealer, int chestChance, boolean hijack, int recovery,
                                    int zombie, int drowned, int creeper) {
    public static final Class<CommonSettingsPayload> TYPE = CommonSettingsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(CommonSettingsPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBoolean(payload.stealer());
        buffer.writeVarInt(payload.chestChance());
        buffer.writeBoolean(payload.hijack());
        buffer.writeVarInt(payload.recovery());
        buffer.writeVarInt(payload.zombie());
        buffer.writeVarInt(payload.drowned());
        buffer.writeVarInt(payload.creeper());
    }
    public static CommonSettingsPayload decode(FriendlyByteBuf buffer) {
        return new CommonSettingsPayload(buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }
    private static Map<ForgeConfigSpec.ConfigValue<?>, Object> remoteValues = Map.of();
    public void accept() {
        remoteValues = Map.of(StealerConfig.ENABLED, stealer, StealerConfig.INFECTION_CHANCE_PERCENT, chestChance,
                StealerConfig.WORM_ENABLED, hijack, StealerConfig.WORM_HEAL_TICKS, recovery,
                StealerConfig.HIJACK_ZOMBIE_CHANCE, zombie, StealerConfig.HIJACK_DROWNED_CHANCE, drowned,
                StealerConfig.HIJACK_CREEPER_CHANCE, creeper);
    }
    @SuppressWarnings("unchecked")
    public static <T> T remoteValue(ForgeConfigSpec.ConfigValue<T> value) {
        return (T) remoteValues.getOrDefault(value, value.get());
    }
    public static CommonSettingsPayload current() {
        return new CommonSettingsPayload(StealerConfig.ENABLED.get(), StealerConfig.INFECTION_CHANCE_PERCENT.get(),
                StealerConfig.WORM_ENABLED.get(), StealerConfig.WORM_HEAL_TICKS.get(),
                StealerConfig.HIJACK_ZOMBIE_CHANCE.get(), StealerConfig.HIJACK_DROWNED_CHANCE.get(),
                StealerConfig.HIJACK_CREEPER_CHANCE.get());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RansomNetwork.sendTo(player, current());
    }
}
