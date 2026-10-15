package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;

public record CaveRansomInput(boolean start) {
    public static final Class<CaveRansomInput> TYPE = CaveRansomInput.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(CaveRansomInput p, FriendlyByteBuf b) {
        b.writeBoolean(p.start());
    }
    public static CaveRansomInput decode(FriendlyByteBuf b) { return new CaveRansomInput(b.readBoolean()); }

}
