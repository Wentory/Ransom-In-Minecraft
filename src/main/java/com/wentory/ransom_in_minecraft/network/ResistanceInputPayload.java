package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;

public record ResistanceInputPayload(int sequence, float dx, float dy, boolean click, boolean playing, boolean inventoryOpen) {
    public static final Class<ResistanceInputPayload> TYPE = ResistanceInputPayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ResistanceInputPayload p, FriendlyByteBuf b) {
        b.writeVarInt(p.sequence());
        b.writeFloat(p.dx());
        b.writeFloat(p.dy());
        b.writeBoolean(p.click());
        b.writeBoolean(p.playing());
        b.writeBoolean(p.inventoryOpen());
    }
    public static ResistanceInputPayload decode(FriendlyByteBuf b) { return new ResistanceInputPayload(b.readVarInt(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readBoolean(), b.readBoolean()); }

}
