package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record MobBanishmentPayload(UUID id, long start) {
    public static final Class<MobBanishmentPayload> TYPE = MobBanishmentPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(MobBanishmentPayload p, FriendlyByteBuf b) {
        b.writeUUID(p.id());
        b.writeLong(p.start());
    }
    public static MobBanishmentPayload decode(FriendlyByteBuf b) { return new MobBanishmentPayload(b.readUUID(), b.readLong()); }

}
