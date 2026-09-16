package com.wentory.ransom_in_minecraft.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persisted in the player's save data, which is scoped to this world. */
public record RansomPlayerData(boolean active, int coins, int target, int ticks,
                               boolean failureArmed, boolean deleteHotbar) {
    public static final RansomPlayerData EMPTY = new RansomPlayerData(false, 0, 0, 0, false, false);
    public static final Codec<RansomPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("active").forGetter(RansomPlayerData::active),
            Codec.INT.fieldOf("coins").forGetter(RansomPlayerData::coins),
            Codec.INT.fieldOf("target").forGetter(RansomPlayerData::target),
            Codec.INT.fieldOf("ticks").forGetter(RansomPlayerData::ticks),
            Codec.BOOL.fieldOf("failureArmed").forGetter(RansomPlayerData::failureArmed),
            Codec.BOOL.fieldOf("deleteHotbar").forGetter(RansomPlayerData::deleteHotbar)
    ).apply(instance, RansomPlayerData::new));
}