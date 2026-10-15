package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record FullHijackVisualPayload(UUID playerId, boolean hijacked) {
    public static final Class<FullHijackVisualPayload> TYPE = FullHijackVisualPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(FullHijackVisualPayload p, FriendlyByteBuf b) {
        b.writeUUID(p.playerId());
        b.writeBoolean(p.hijacked());
    }
    public static FullHijackVisualPayload decode(FriendlyByteBuf b) { return new FullHijackVisualPayload(b.readUUID(), b.readBoolean()); }

}
