package com.wentory.ransom_in_minecraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Independent visual copy; never changes the player's inventory stack. */
final class CrucifixProjectionModel implements ResourceManagerReloadListener {
    static final CrucifixProjectionModel INSTANCE = new CrucifixProjectionModel();
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "ransom_in_minecraft", "crucifix_projection_emissive");
    private List<Face> faces = List.of();

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        List<Face> loaded = new ArrayList<>();
        try (var reader = resources.getResourceOrThrow(Identifier.fromNamespaceAndPath(
                "ransom_in_minecraft", "models/item/crucifix.json")).openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            for (var elementValue : model.getAsJsonArray("elements")) {
                JsonObject element = elementValue.getAsJsonObject();
                Vec3 from = vector(element.getAsJsonArray("from"));
                Vec3 to = vector(element.getAsJsonArray("to"));
                Matrix4f transform = new Matrix4f();
                if (element.has("rotation")) {
                    JsonObject rotation = element.getAsJsonObject("rotation");
                    Vec3 origin = vector(rotation.getAsJsonArray("origin"));
                    Axis axis = switch (rotation.get("axis").getAsString()) {
                        case "x" -> Axis.XP; case "z" -> Axis.ZP; default -> Axis.YP;
                    };
                    transform.translate((float) origin.x, (float) origin.y, (float) origin.z)
                            .rotate(axis.rotationDegrees(rotation.get("angle").getAsFloat()))
                            .translate((float) -origin.x, (float) -origin.y, (float) -origin.z);
                }
                for (var faceEntry : element.getAsJsonObject("faces").entrySet()) {
                    Vec3[] corners = corners(faceEntry.getKey(), from, to);
                    for (int i = 0; i < 4; i++) {
                        Vector3f point = transform.transformPosition((float) corners[i].x, (float) corners[i].y,
                                (float) corners[i].z, new Vector3f());
                        corners[i] = new Vec3((point.x - 8) / 16, (point.y - 8) / 16, (point.z - 8) / 16);
                    }
                    JsonArray uv = faceEntry.getValue().getAsJsonObject().getAsJsonArray("uv");
                    loaded.add(new Face(corners, uv.get(0).getAsFloat() / 16, uv.get(1).getAsFloat() / 16,
                            uv.get(2).getAsFloat() / 16, uv.get(3).getAsFloat() / 16));
                }
            }
            try (var input = resources.getResourceOrThrow(Identifier.fromNamespaceAndPath(
                    "ransom_in_minecraft", "textures/item/crucifix/crucifix.png")).open()) {
                NativeImage image = NativeImage.read(input);
                // Recolor the user's texture in memory, preserving its alpha and fine shading.
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = net.minecraft.util.ARGB.toABGR(image.getPixel(x, y));
                    float detail = 0.97F + 0.03F * ((pixel & 255) + ((pixel >>> 8) & 255)
                            + ((pixel >>> 16) & 255)) / 765.0F;
                    image.setPixelABGR(x, y, (pixel & 0xFF000000) | (Math.round(255 * detail) << 16)
                            | (Math.round(252 * detail) << 8) | Math.round(242 * detail));
                }
                Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(() -> "Ransom crucifix projection",image));
            }
            faces = List.copyOf(loaded);
        } catch (IOException | RuntimeException error) {
            faces = List.of();
            org.slf4j.LoggerFactory.getLogger(CrucifixProjectionModel.class).error("Cannot load Crucifix projection", error);
        }
    }

    void render(PoseStack pose, DeferredBuffers buffers, float evaporation) {
        var type = RansomRenderTypes.crucifixUnlit(TEXTURE, evaporation > 0);
        VertexConsumer consumer = buffers.getBuffer(type);
        int subdivisions = evaporation > 0 ? 8 : 1;
        for (int index = 0; index < faces.size(); index++) {
            Face face = faces.get(index);
            for (int y = 0; y < subdivisions; y++) for (int x = 0; x < subdivisions; x++) {
                int hash = (index * 73856093) ^ (x * 19349663) ^ (y * 83492791);
                float threshold = (hash & 1023) / 1024.0F;
                if (evaporation > threshold && evaporation > 0) continue;
                float left = x / (float) subdivisions, right = (x + 1F) / subdivisions;
                float top = y / (float) subdivisions, bottom = (y + 1F) / subdivisions;
                vertex(pose, consumer, point(face, left, top), Mth.lerp(left, face.u0, face.u1), Mth.lerp(top, face.v0, face.v1), evaporation);
                vertex(pose, consumer, point(face, right, top), Mth.lerp(right, face.u0, face.u1), Mth.lerp(top, face.v0, face.v1), evaporation);
                vertex(pose, consumer, point(face, right, bottom), Mth.lerp(right, face.u0, face.u1), Mth.lerp(bottom, face.v0, face.v1), evaporation);
                vertex(pose, consumer, point(face, left, bottom), Mth.lerp(left, face.u0, face.u1), Mth.lerp(bottom, face.v0, face.v1), evaporation);
            }
        }
        buffers.endBatch(type);
    }

    private static Vec3 point(Face face, float u, float v) {
        return face.corners[0].lerp(face.corners[1], u).lerp(face.corners[3].lerp(face.corners[2], u), v);
    }
    private static void vertex(PoseStack pose, VertexConsumer consumer, Vec3 point, float u, float v, float fade) {
        consumer.addVertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(255, 255, 255, Math.round(255 * (1 - fade))).setUv(u, v)
                .setLight(0xF000F0).setOverlay(0).setNormal(pose.last(),0,0,1);
    }
    private static Vec3 vector(JsonArray a) {
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }
    private static Vec3[] corners(String face, Vec3 a, Vec3 b) {
        return switch (face) {
            case "north" -> new Vec3[]{new Vec3(b.x,b.y,a.z),new Vec3(a.x,b.y,a.z),new Vec3(a.x,a.y,a.z),new Vec3(b.x,a.y,a.z)};
            case "south" -> new Vec3[]{new Vec3(a.x,b.y,b.z),new Vec3(b.x,b.y,b.z),new Vec3(b.x,a.y,b.z),new Vec3(a.x,a.y,b.z)};
            case "east" -> new Vec3[]{new Vec3(b.x,b.y,b.z),new Vec3(b.x,b.y,a.z),new Vec3(b.x,a.y,a.z),new Vec3(b.x,a.y,b.z)};
            case "west" -> new Vec3[]{new Vec3(a.x,b.y,a.z),new Vec3(a.x,b.y,b.z),new Vec3(a.x,a.y,b.z),new Vec3(a.x,a.y,a.z)};
            case "up" -> new Vec3[]{new Vec3(a.x,b.y,a.z),new Vec3(b.x,b.y,a.z),new Vec3(b.x,b.y,b.z),new Vec3(a.x,b.y,b.z)};
            case "down" -> new Vec3[]{new Vec3(a.x,a.y,b.z),new Vec3(b.x,a.y,b.z),new Vec3(b.x,a.y,a.z),new Vec3(a.x,a.y,a.z)};
            default -> throw new IllegalArgumentException("Unknown model face " + face);
        };
    }
    private record Face(Vec3[] corners, float u0, float v0, float u1, float v1) {}
}
