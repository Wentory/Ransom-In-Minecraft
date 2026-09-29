package com.wentory.ransom_in_minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/** Clips real model polygons into horizontal bands, retaining UVs and depth. */
final class GeometryDistortion {
    record Band(float bottom, float top, float shift, float stretch, boolean hidden) {}

    static SubmitNodeCollector wrap(SubmitNodeCollector collector, float originY, float centerX, float centerZ,
                                    Vector3f right, List<Band> bands) {
        return (SubmitNodeCollector) proxy(collector, SubmitNodeCollector.class, originY, centerX, centerZ, right, bands);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object proxy(OrderedSubmitNodeCollector target, Class<?> api, float originY, float centerX,
                                float centerZ, Vector3f right, List<Band> bands) {
        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (self, method, args) -> {
            if (method.isDefault()) return InvocationHandler.invokeDefault(self, method, args);
            if (method.getName().equals("order")) {
                return proxy(((SubmitNodeCollector) target).order((int) args[0]), OrderedSubmitNodeCollector.class,
                        originY, centerX, centerZ, right, bands);
            }
            if (method.getName().equals("submitModel") && args.length == 10 && args[3] instanceof RenderType type) {
                Model model = (Model) args[0]; Object state = args[1]; PoseStack pose = (PoseStack) args[2];
                int light = (int) args[4], overlay = (int) args[5], color = (int) args[6];
                TextureAtlasSprite sprite = (TextureAtlasSprite) args[7];
                target.submitCustomGeometry(pose, type, (captured, vertices) -> {
                    PoseStack replay = new PoseStack(); replay.last().set(captured);
                    SlicedConsumer sliced = new SlicedConsumer(vertices, originY, centerX, centerZ, right, bands);
                    model.setupAnim(state);
                    model.renderToBuffer(replay, sprite == null ? sliced : sprite.wrap(sliced), light, overlay, color);
                    sliced.finish();
                });
                return null;
            }
            if (method.getName().equals("submitCustomGeometry")) {
                SubmitNodeCollector.CustomGeometryRenderer renderer = (SubmitNodeCollector.CustomGeometryRenderer) args[2];
                target.submitCustomGeometry((PoseStack) args[0], (RenderType) args[1], (pose, vertices) -> {
                    SlicedConsumer sliced = new SlicedConsumer(vertices, originY, centerX, centerZ, right, bands);
                    renderer.render(pose, sliced); sliced.finish();
                });
                return null;
            }
            try { return method.invoke(target, args); }
            catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
        });
    }

    private static final class V {
        float x,y,z,u,v,nx,ny,nz; int color=-1, overlay, light;
        V mix(V b, float t) {
            V c=new V(); c.x=x+(b.x-x)*t; c.y=y+(b.y-y)*t; c.z=z+(b.z-z)*t;
            c.u=u+(b.u-u)*t; c.v=v+(b.v-v)*t;
            c.nx=nx+(b.nx-nx)*t; c.ny=ny+(b.ny-ny)*t; c.nz=nz+(b.nz-nz)*t;
            c.color=color; c.overlay=overlay; c.light=light; return c;
        }
    }
    private static final class SlicedConsumer implements VertexConsumer {
        final VertexConsumer target; final float originY,cx,cz; final Vector3f right; final List<Band> bands;
        final List<V> quad=new ArrayList<>(4); V current;
        SlicedConsumer(VertexConsumer target,float originY,float cx,float cz,Vector3f right,List<Band> bands) {
            this.target=target; this.originY=originY; this.cx=cx; this.cz=cz; this.right=right; this.bands=bands;
        }
        public VertexConsumer addVertex(float x,float y,float z) {
            if(quad.size()==4) flush(); current=new V(); current.x=x;current.y=y;current.z=z;quad.add(current);return this;
        }
        public VertexConsumer setColor(int r,int g,int b,int a) {return setColor((a<<24)|(r<<16)|(g<<8)|b);}
        public VertexConsumer setColor(int c){current.color=c;return this;}
        public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){current.overlay=(v<<16)|u;return this;}
        public VertexConsumer setUv2(int u,int v){current.light=(v<<16)|u;return this;}
        public VertexConsumer setNormal(float x,float y,float z){current.nx=x;current.ny=y;current.nz=z;return this;}
        public VertexConsumer setLineWidth(float w){return this;}
        void finish(){if(quad.size()==4)flush();}
        void flush(){
            for(Band band:bands){
                if(band.hidden)continue;
                List<V> polygon=clip(quad,originY+band.bottom,true);
                polygon=clip(polygon,originY+band.top,false);
                for(int i=1;i+1<polygon.size();i++){
                    emit(polygon.get(0),band);emit(polygon.get(i),band);emit(polygon.get(i+1),band);emit(polygon.get(i+1),band);
                }
            }
            quad.clear();
        }
        List<V> clip(List<V> input,float boundary,boolean above){
            List<V> output=new ArrayList<>(); if(input.isEmpty())return output;
            V previous=input.get(input.size()-1);boolean previousInside=above?previous.y>=boundary:previous.y<=boundary;
            for(V vertex:input){
                boolean inside=above?vertex.y>=boundary:vertex.y<=boundary;
                if(inside!=previousInside)output.add(previous.mix(vertex,(boundary-previous.y)/(vertex.y-previous.y)));
                if(inside)output.add(vertex);previous=vertex;previousInside=inside;
            }
            return output;
        }
        void emit(V v,Band band){
            float lateral=(v.x-cx)*right.x+(v.z-cz)*right.z;
            float delta=band.shift+lateral*(band.stretch-1);
            target.addVertex(v.x+right.x*delta,v.y,v.z+right.z*delta).setColor(v.color).setUv(v.u,v.v)
                    .setOverlay(v.overlay).setLight(v.light).setNormal(v.nx,v.ny,v.nz);
        }
    }
}
