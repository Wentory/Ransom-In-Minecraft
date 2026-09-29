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
import java.util.function.UnaryOperator;

final class UvGeometryCollector {
    static SubmitNodeCollector wrap(SubmitNodeCollector target, UnaryOperator<VertexConsumer> transform) {
        return (SubmitNodeCollector) proxy(target, SubmitNodeCollector.class, transform);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object proxy(OrderedSubmitNodeCollector target, Class<?> api, UnaryOperator<VertexConsumer> transform) {
        return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (self, method, args) -> {
            if (method.isDefault()) return InvocationHandler.invokeDefault(self, method, args);
            if (method.getName().equals("order")) return proxy(((SubmitNodeCollector)target).order((int)args[0]), OrderedSubmitNodeCollector.class, transform);
            if (method.getName().equals("submitModel") && args.length==10 && args[3] instanceof RenderType type) {
                Model model=(Model)args[0]; Object state=args[1]; int light=(int)args[4], overlay=(int)args[5], color=(int)args[6];
                TextureAtlasSprite sprite=(TextureAtlasSprite)args[7];
                target.submitCustomGeometry((PoseStack)args[2],type,(pose,vertices)->{
                    PoseStack replay=new PoseStack();replay.last().set(pose);
                    VertexConsumer shifted=transform.apply(vertices);
                    model.setupAnim(state);
                    model.renderToBuffer(replay,sprite==null?shifted:sprite.wrap(shifted),light,overlay,color);
                });return null;
            }
            try{return method.invoke(target,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
        });
    }
}
