package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;
import com.wentory.ransom_in_minecraft.rules.QteRules.Difficulty;
public record StealerSettingsPayload(boolean enabled, int chancePercent, Difficulty difficulty) {
    public static final Class<StealerSettingsPayload> TYPE = StealerSettingsPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(StealerSettingsPayload p, FriendlyByteBuf b) {
        b.writeBoolean(p.enabled());
        b.writeVarInt(p.chancePercent());
        b.writeEnum(p.difficulty());
    }
    public static StealerSettingsPayload decode(FriendlyByteBuf b) { return new StealerSettingsPayload(b.readBoolean(), b.readVarInt(), b.readEnum(Difficulty.class)); }
}
