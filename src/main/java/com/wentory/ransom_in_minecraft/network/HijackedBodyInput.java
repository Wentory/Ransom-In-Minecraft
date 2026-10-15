package com.wentory.ransom_in_minecraft.network;
import net.minecraft.network.FriendlyByteBuf;

import com.wentory.ransom_in_minecraft.RansomInMinecraft;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/** Server-selected steering; the client executes ordinary player travel physics. */
public record HijackedBodyInput(float x, float z, float yaw, float pitch, boolean jump) {
    private static Consumer<HijackedBodyInput> receiver = packet -> {};
    public static void bind(Consumer<HijackedBodyInput> consumer) { receiver = consumer; }
    public void accept() { receiver.accept(this); }
    public static final Class<HijackedBodyInput> TYPE = HijackedBodyInput.class;
    public static final Object STREAM_CODEC = new Object();
    public static void encode(HijackedBodyInput p, FriendlyByteBuf b) {
        b.writeFloat(p.x());
        b.writeFloat(p.z());
        b.writeFloat(p.yaw());
        b.writeFloat(p.pitch());
        b.writeBoolean(p.jump());
    }
    public static HijackedBodyInput decode(FriendlyByteBuf b) { return new HijackedBodyInput(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()); }

}
