package com.wentory.ransom_in_minecraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wentory.ransom_in_minecraft.CrucifixBanishmentTiming;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/** Reads the exported flat elements directly, keeping their authored geometry and UVs. */
final class CrucifixCircleModel implements ResourceManagerReloadListener {
    static final CrucifixCircleModel INSTANCE = new CrucifixCircleModel();
    private static final float MODEL_SCALE = 4.0F / 24.5F;
    private static final float UV_CYCLES_PER_SECOND = 0.1F;
    private List<Plane> planes = List.of();
    private final List<Plane> drawOrder = new ArrayList<>();
    private final Vector3f sortA = new Vector3f();
    private final Vector3f sortB = new Vector3f();

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        List<Plane> loaded = new ArrayList<>();
        try {
            load(resources, "crucifix_circle", false, loaded);
            load(resources, "crucifix_circle_shine", true, loaded);
            planes = List.copyOf(loaded);
        } catch (IOException | RuntimeException error) {
            planes = List.of();
            org.slf4j.LoggerFactory.getLogger(CrucifixCircleModel.class)
                    .error("Cannot load Crucifix circle models", error);
        }
    }

    private static void load(ResourceManager resources, String name, boolean shine, List<Plane> output)
            throws IOException {
        Identifier path = Identifier.fromNamespaceAndPath("ransom_in_minecraft",
                "models/item/" + name + ".json");
        try (var reader = resources.getResourceOrThrow(path).openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject textures = model.getAsJsonObject("textures");
            for (var value : model.getAsJsonArray("elements")) {
                JsonObject element = value.getAsJsonObject();
                Vec3 from = vector(element.getAsJsonArray("from"));
                Vec3 to = vector(element.getAsJsonArray("to"));
                String face;
                Vec3[] vertices;
                if (from.y == to.y) {
                    face = "up";
                    vertices = new Vec3[]{new Vec3(from.x, from.y, from.z), new Vec3(to.x, from.y, from.z),
                            new Vec3(to.x, from.y, to.z), new Vec3(from.x, from.y, to.z)};
                } else if (from.z == to.z) {
                    face = "north";
                    double left = from.z < 8 ? from.x : to.x;
                    double right = from.z < 8 ? to.x : from.x;
                    vertices = new Vec3[]{new Vec3(left, to.y, from.z), new Vec3(right, to.y, from.z),
                            new Vec3(right, from.y, from.z), new Vec3(left, from.y, from.z)};
                } else if (from.x == to.x) {
                    face = "east";
                    double left = from.x < 8 ? to.z : from.z;
                    double right = from.x < 8 ? from.z : to.z;
                    vertices = new Vec3[]{new Vec3(from.x, to.y, left), new Vec3(from.x, to.y, right),
                            new Vec3(from.x, from.y, right), new Vec3(from.x, from.y, left)};
                } else {
                    throw new IllegalArgumentException("Circle elements must be planes: " + name);
                }
                JsonObject data = element.getAsJsonObject("faces").getAsJsonObject(face);
                String textureName = data.get("texture").getAsString();
                while (textureName.startsWith("#")) textureName = textures.get(textureName.substring(1)).getAsString();
                Identifier texture = Identifier.parse(textureName);
                texture = Identifier.fromNamespaceAndPath(texture.getNamespace(),
                        "textures/" + texture.getPath() + ".png");
                JsonArray uv = data.getAsJsonArray("uv");
                JsonObject rotation = element.getAsJsonObject("rotation");
                Vec3 origin = rotation == null ? Vec3.ZERO : vector(rotation.getAsJsonArray("origin"));
                float angle = rotation == null ? 0 : rotation.get("angle").getAsFloat();
                String axis = rotation == null ? "y" : rotation.get("axis").getAsString();
                Vec3 center = from.add(to).scale(0.5).subtract(origin);
                double radians = Math.toRadians(angle);
                double sin = Math.sin(radians), cos = Math.cos(radians);
                center = switch (axis) {
                    case "x" -> new Vec3(center.x, cos * center.y - sin * center.z, sin * center.y + cos * center.z);
                    case "z" -> new Vec3(cos * center.x - sin * center.y, sin * center.x + cos * center.y, center.z);
                    default -> new Vec3(cos * center.x + sin * center.z, center.y, -sin * center.x + cos * center.z);
                };
                center = center.add(origin).subtract(8, 0.7, 8).scale(MODEL_SCALE);
                output.add(new Plane(vertices, texture, origin, angle, axis, shine, center,
                        uv.get(0).getAsFloat() / 16, uv.get(1).getAsFloat() / 16,
                        uv.get(2).getAsFloat() / 16, uv.get(3).getAsFloat() / 16));
            }
        }
    }

    private static Vec3 vector(JsonArray array) {
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    List<Vec3> chainAnchors(float rotation) {
        List<Vec3> anchors = new ArrayList<>(8);
        for (Plane plane : planes) {
            String path = plane.texture.getPath();
            if (plane.shine || !(path.endsWith("/circle_2.png") || path.endsWith("/circle_3.png"))) continue;
            for (Vec3 vertex : plane.vertices) {
                Vec3 offset = vertex.subtract(plane.origin).yRot((float) Math.toRadians(plane.angle));
                Vec3 point = offset.add(plane.origin).subtract(8, 0.7, 8).scale(MODEL_SCALE);
                anchors.add(point.yRot((float) Math.toRadians(rotation)));
            }
        }
        return anchors;
    }

    void render(PoseStack pose, DeferredBuffers buffers, float seconds,
                boolean chest, boolean compatible, boolean shinePass, boolean banishment, boolean chestFloor) {
        drawOrder.clear();
        for (Plane plane : planes) if (plane.shine == shinePass) drawOrder.add(plane);
        if (shinePass) {
            var matrix = pose.last().pose();
            drawOrder.sort((a, b) -> {
                matrix.transformPosition((float) a.center.x, (float) a.center.y, (float) a.center.z, sortA);
                matrix.transformPosition((float) b.center.x, (float) b.center.y, (float) b.center.z, sortB);
                return Float.compare(sortB.lengthSquared(), sortA.lengthSquared());
            });
        }
        for (Plane plane : drawOrder) {
            String texture = plane.texture.getPath();
            boolean inner = !plane.shine && (texture.endsWith("/moon.png") || texture.endsWith("/circle_1.png"));
            if (chest && inner == chestFloor) continue;
            float age = seconds * 20;
            float opacity = !banishment && !chest ? 1 : plane.shine
                    ? CrucifixBanishmentTiming.fade(age, CrucifixBanishmentTiming.CAPTURE_END, CrucifixBanishmentTiming.SHINE_FADE_TICKS)
                    : texture.endsWith("/moon.png")
                    ? CrucifixBanishmentTiming.fade(age, CrucifixBanishmentTiming.MOON_FADE_START, CrucifixBanishmentTiming.MOON_FADE_TICKS)
                    : texture.endsWith("/circle_1.png")
                    ? CrucifixBanishmentTiming.fade(age, CrucifixBanishmentTiming.INNER_FADE_START, CrucifixBanishmentTiming.CIRCLE_FADE_TICKS)
                    : CrucifixBanishmentTiming.fade(age, CrucifixBanishmentTiming.OUTER_FADE_START, CrucifixBanishmentTiming.CIRCLE_FADE_TICKS);
            if (opacity <= 0) continue;
            pose.pushPose();
            try {
                pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
                double verticalOffset = chest && !chestFloor
                        ? -plane.vertices[0].y - (texture.endsWith("/moon.png") ? 0 : 1.2)
                        : -0.7;
                pose.translate(-8, verticalOffset, -8);
                pose.translate(plane.origin.x, plane.origin.y, plane.origin.z);
                Axis axis = switch (plane.axis) { case "x" -> Axis.XP; case "z" -> Axis.ZP; default -> Axis.YP; };
                pose.mulPose(axis.rotationDegrees(plane.angle));
                pose.translate(-plane.origin.x, -plane.origin.y, -plane.origin.z);
                var type = plane.shine || opacity < 1 ? RansomRenderTypes.crucifixShine(plane.texture, compatible)
                        : compatible ? RansomRenderTypes.banishmentSprite(plane.texture)
                        : RansomRenderTypes.crucifixCircle(plane.texture);
                VertexConsumer consumer = buffers.getBuffer(type);
                float shift = plane.shine ? (seconds * UV_CYCLES_PER_SECOND) % 1 : 0;
                // Split at the texture seam: vanilla/Iris never sample outside this texture.
                float start = plane.u0 + shift;
                float end = plane.u1 + shift;
                float seam = (float) Math.floor(start) + 1;
                if (plane.shine && start < seam && end > seam) {
                    float split = (seam - start) / (end - start);
                    emit(pose, consumer, plane, 0, split, start - (float) Math.floor(start), 1, compatible, opacity);
                    emit(pose, consumer, plane, split, 1, 0, end - seam, compatible, opacity);
                } else {
                    float wrap = plane.shine ? (float) Math.floor(start) : 0;
                    emit(pose, consumer, plane, 0, 1, start - wrap, end - wrap, compatible, opacity);
                }
                buffers.endBatch(type);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void emit(PoseStack pose, VertexConsumer consumer, Plane plane,
                             float left, float right, float u0, float u1, boolean compatible, float opacity) {
        vertex(pose, consumer, plane.vertices[0].lerp(plane.vertices[1], left), u0, plane.v0, compatible, opacity);
        vertex(pose, consumer, plane.vertices[0].lerp(plane.vertices[1], right), u1, plane.v0, compatible, opacity);
        vertex(pose, consumer, plane.vertices[3].lerp(plane.vertices[2], right), u1, plane.v1, compatible, opacity);
        vertex(pose, consumer, plane.vertices[3].lerp(plane.vertices[2], left), u0, plane.v1, compatible, opacity);
    }

    private static void vertex(PoseStack pose, VertexConsumer consumer, Vec3 position,
                               float u, float v, boolean compatible, float opacity) {
        consumer.addVertex(pose.last().pose(), (float) position.x, (float) position.y, (float) position.z)
                .setUv(u, v).setColor((Math.round(opacity * 255) << 24) | (compatible ? 0x93EFFF : 0xFFFFFF));
        if (compatible) consumer.setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(pose.last(), 0, 1, 0);
    }

    private record Plane(Vec3[] vertices, Identifier texture, Vec3 origin, float angle, String axis,
                         boolean shine, Vec3 center, float u0, float v0, float u1, float v1) {}
}
