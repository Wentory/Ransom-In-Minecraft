package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;

public record ResistanceStatePayload(boolean active, int acknowledged, int ticks, float cursorX, float cursorY,
        float targetX, float targetY, float progress, boolean inventoryUnlocked, int glitchTicks, int feedback, int soundEvents) {
    private static Consumer<ResistanceStatePayload> receiver = payload -> {};
    public static void bind(Consumer<ResistanceStatePayload> consumer) { receiver = consumer; }
    public void accept() { receiver.accept(this); }
    public static final Class<ResistanceStatePayload> TYPE = ResistanceStatePayload.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(ResistanceStatePayload p, FriendlyByteBuf b) {
        b.writeBoolean(p.active());
        b.writeVarInt(p.acknowledged());
        b.writeVarInt(p.ticks());
        b.writeFloat(p.cursorX());
        b.writeFloat(p.cursorY());
        b.writeFloat(p.targetX());
        b.writeFloat(p.targetY());
        b.writeFloat(p.progress());
        b.writeBoolean(p.inventoryUnlocked());
        b.writeVarInt(p.glitchTicks());
        b.writeVarInt(p.feedback());
        b.writeVarInt(p.soundEvents());
    }
    public static ResistanceStatePayload decode(FriendlyByteBuf b) { return new ResistanceStatePayload(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt()); }

}
