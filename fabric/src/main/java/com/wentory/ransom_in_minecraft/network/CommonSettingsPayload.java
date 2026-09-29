package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomFabric;
import com.wentory.ransom_in_minecraft.StealerConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.wentory.ransom_in_minecraft.platform.FabricEvents.PlayerEvent;
import com.wentory.ransom_in_minecraft.platform.FabricNetwork;
import com.wentory.ransom_in_minecraft.platform.ConfigSpec;
import java.util.Map;

public record CommonSettingsPayload(boolean stealer, int chestChance, boolean hijack, int recovery,
                                    int zombie, int drowned, int creeper) implements CustomPacketPayload {
    public static final Type<CommonSettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RansomFabric.MODID, "common_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CommonSettingsPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeBoolean(p.stealer); b.writeVarInt(p.chestChance); b.writeBoolean(p.hijack);
                b.writeVarInt(p.recovery); b.writeVarInt(p.zombie); b.writeVarInt(p.drowned); b.writeVarInt(p.creeper); },
            b -> new CommonSettingsPayload(b.readBoolean(), b.readVarInt(), b.readBoolean(), b.readVarInt(),
                    b.readVarInt(), b.readVarInt(), b.readVarInt()));
    private static Map<ConfigSpec.ConfigValue<?>, Object> remoteValues = Map.of();
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public void accept() {
        remoteValues = Map.of(StealerConfig.ENABLED, stealer, StealerConfig.INFECTION_CHANCE_PERCENT, chestChance,
                StealerConfig.WORM_ENABLED, hijack, StealerConfig.WORM_HEAL_TICKS, recovery,
                StealerConfig.HIJACK_ZOMBIE_CHANCE, zombie, StealerConfig.HIJACK_DROWNED_CHANCE, drowned,
                StealerConfig.HIJACK_CREEPER_CHANCE, creeper);
    }
    @SuppressWarnings("unchecked")
    public static <T> T remoteValue(ConfigSpec.ConfigValue<T> value) {
        return (T) remoteValues.getOrDefault(value, value.get());
    }
    public static CommonSettingsPayload current() {
        return new CommonSettingsPayload(StealerConfig.ENABLED.get(), StealerConfig.INFECTION_CHANCE_PERCENT.get(),
                StealerConfig.WORM_ENABLED.get(), StealerConfig.WORM_HEAL_TICKS.get(),
                StealerConfig.HIJACK_ZOMBIE_CHANCE.get(), StealerConfig.HIJACK_DROWNED_CHANCE.get(),
                StealerConfig.HIJACK_CREEPER_CHANCE.get());
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FabricNetwork.sendToPlayer(player, current());
    }
}
