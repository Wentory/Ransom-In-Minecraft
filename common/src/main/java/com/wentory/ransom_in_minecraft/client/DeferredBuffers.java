package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Captures geometry during extraction for submission through the frame collector. */
final class DeferredBuffers {
    private final SubmitNodeCollector collector;
    private final Map<RenderType, RecordingConsumer> layers = new LinkedHashMap<>();
    DeferredBuffers(SubmitNodeCollector collector) { this.collector = collector; }
    VertexConsumer getBuffer(RenderType type) { return layers.computeIfAbsent(type, ignored -> new RecordingConsumer()); }
    void endBatch(RenderType type) {
        RecordingConsumer consumer = layers.remove(type);
        if (consumer == null || consumer.vertices.isEmpty()) return;
        List<Vertex> vertices = List.copyOf(consumer.vertices);
        collector.submitCustomGeometry(new PoseStack(), type, (pose, target) -> {
            for (Vertex v : vertices) {
                target.addVertex(v.x, v.y, v.z).setColor(v.r, v.g, v.b, v.a)
                        .setUv(v.u, v.v).setUv1(v.ou, v.ov).setUv2(v.lu, v.lv)
                        .setNormal(v.nx, v.ny, v.nz).setLineWidth(v.lineWidth);
            }
        });
    }
    void endBatch() { for (RenderType type : List.copyOf(layers.keySet())) endBatch(type); }
    private static final class Vertex {
        float x, y, z, u, v, nx, ny, nz, lineWidth = 1;
        int r = 255, g = 255, b = 255, a = 255, ou, ov, lu, lv;
    }
    private static final class RecordingConsumer implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        Vertex current;
        public VertexConsumer addVertex(float x, float y, float z) {
            current = new Vertex(); current.x = x; current.y = y; current.z = z; vertices.add(current); return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { current.r=r; current.g=g; current.b=b; current.a=a; return this; }
        public VertexConsumer setColor(int color) { return setColor(color >>> 16 & 255, color >>> 8 & 255, color & 255, color >>> 24); }
        public VertexConsumer setUv(float u, float v) { current.u=u; current.v=v; return this; }
        public VertexConsumer setUv1(int u, int v) { current.ou=u; current.ov=v; return this; }
        public VertexConsumer setUv2(int u, int v) { current.lu=u; current.lv=v; return this; }
        public VertexConsumer setNormal(float x, float y, float z) { current.nx=x; current.ny=y; current.nz=z; return this; }
        public VertexConsumer setLineWidth(float width) { current.lineWidth=width; return this; }
    }
}
