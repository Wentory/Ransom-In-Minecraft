package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;

public record CrucifixUsePayload() {
    public static final Class<CrucifixUsePayload> TYPE = CrucifixUsePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(CrucifixUsePayload p, FriendlyByteBuf b) {
    }
    public static CrucifixUsePayload decode(FriendlyByteBuf b) { return new CrucifixUsePayload(); }

}
