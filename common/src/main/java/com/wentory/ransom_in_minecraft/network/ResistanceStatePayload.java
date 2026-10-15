package com.wentory.ransom_in_minecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.function.Consumer;

public record ResistanceStatePayload(boolean active, int acknowledged, int ticks, float cursorX, float cursorY,
        float targetX, float targetY, float progress, boolean inventoryUnlocked, int glitchTicks, int feedback, int soundEvents)
        implements CustomPacketPayload {
    private static Consumer<ResistanceStatePayload> receiver = payload -> {};
    public static void bind(Consumer<ResistanceStatePayload> consumer) { receiver = consumer; }
    public void accept() { receiver.accept(this); }
    public static final Type<ResistanceStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            "ransom_in_minecraft", "resistance_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResistanceStatePayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> { b.writeBoolean(p.active); b.writeVarInt(p.acknowledged); b.writeVarInt(p.ticks);
                b.writeFloat(p.cursorX); b.writeFloat(p.cursorY); b.writeFloat(p.targetX); b.writeFloat(p.targetY);
                b.writeFloat(p.progress); b.writeBoolean(p.inventoryUnlocked); b.writeVarInt(p.glitchTicks); b.writeVarInt(p.feedback); b.writeVarInt(p.soundEvents); },
            b -> new ResistanceStatePayload(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(),
                    b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
