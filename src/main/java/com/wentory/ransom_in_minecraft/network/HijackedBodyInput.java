package com.wentory.ransom_in_minecraft.network;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-selected steering; the client executes ordinary player travel physics. */
public record HijackedBodyInput(float x, float z, float yaw, float pitch, boolean jump) implements CustomPacketPayload {
    private static Consumer<HijackedBodyInput> receiver = packet -> {};
    public static void bind(Consumer<HijackedBodyInput> consumer) { receiver = consumer; }
    public void accept() { receiver.accept(this); }
    public static final Type<HijackedBodyInput> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RansomInMinecraft.MODID, "hijacked_body_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HijackedBodyInput> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeFloat(packet.x); buffer.writeFloat(packet.z); buffer.writeFloat(packet.yaw);
                buffer.writeFloat(packet.pitch); buffer.writeBoolean(packet.jump); },
            buffer -> new HijackedBodyInput(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
