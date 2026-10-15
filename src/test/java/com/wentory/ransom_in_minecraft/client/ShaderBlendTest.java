package com.wentory.ransom_in_minecraft.client;

import com.google.gson.*;
import com.mojang.blaze3d.shaders.BlendMode;
import net.minecraft.client.renderer.ShaderInstance;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Validate the shader blend parser used by 1.20.1, without a GL context. */
public final class ShaderBlendTest {
    public static void main(String[] args) throws Exception {
        if (!ShaderInstance.parseBlendNode(null).isOpaque()) throw new AssertionError("Baseline changed");
        for (String shader : new String[]{"crucifix_circle", "resistance_bar", "crucifix_light"}) {
            String path = "/assets/ransom_in_minecraft/shaders/core/" + shader + ".json";
            try (var stream = ShaderBlendTest.class.getResourceAsStream(path)) {
                if (stream == null) throw new AssertionError(path);
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                BlendMode blend = ShaderInstance.parseBlendNode(json.getAsJsonObject("blend"));
                if (blend.isOpaque()) throw new AssertionError(shader + " disables blending");
                int destination = shader.equals("crucifix_light") ? 1 : 771;
                check(blend, "srcColorFactor", 770);
                check(blend, "dstColorFactor", destination);
                check(blend, "srcAlphaFactor", shader.equals("crucifix_light") ? 0 : 1);
                check(blend, "dstAlphaFactor", destination);
            }
        }
        System.out.println("Shader blend: missing blend reproduces opaque mode; alpha/additive descriptors parsed correctly");
    }
    private static void check(BlendMode mode, String name, int expected) throws Exception {
        var field = BlendMode.class.getDeclaredField(name);
        field.setAccessible(true);
        if (field.getInt(mode) != expected) throw new AssertionError(name);
    }
}
