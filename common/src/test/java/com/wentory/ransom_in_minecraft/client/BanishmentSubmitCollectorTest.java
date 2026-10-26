package com.wentory.ransom_in_minecraft.client;
import java.lang.reflect.Proxy;
import net.minecraft.client.renderer.SubmitNodeCollector;

/** Verifies full and default submission paths, including ordered layer collectors. */
public final class BanishmentSubmitCollectorTest {
    public static void main(String[] args) {
        int[] values=new int[3];
        SubmitNodeCollector[] source=new SubmitNodeCollector[1];
        source[0]=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class}, (proxy,method,arguments)-> {
            if(method.getName().equals("order")) return source[0];
            if(method.getName().equals("submitModel")) {
                values[0]=(int)arguments[4]; values[1]=(int)arguments[6]; values[2]++;
            }
            return null;
        });
        var flash=new BanishmentSubmitCollector(source[0],0);
        flash.submitModel(null,null,null,(net.minecraft.client.renderer.rendertype.RenderType)null,0,0,0xFFFFFFFF,null);
        check(values[0]==0xF000F0 && values[1]==0xFF55DDFF,"Default overload must flash cyan at full light");
        var fade=new BanishmentSubmitCollector(source[0],.6F);
        fade.order(2).submitModel(null,null,null,(net.minecraft.client.renderer.rendertype.RenderType)null,123,0,0x80643219,null,0,null);
        check(values[0]==123 && Math.abs((values[1]>>>24)-64)<=1 && (values[1]&0xFFFFFF)==0x643219,
                "Ordered model must preserve original tint/light and multiply alpha");
        new BanishmentSubmitCollector(source[0],1).submitModel(null,null,null,
                (net.minecraft.client.renderer.rendertype.RenderType)null,0,0,0xFFFFFFFF,null,0,null);
        check(values[1]>>>24==0 && values[2]==3,"Final model must be transparent; submissions must not be lost");
        System.out.println("Banishment collector: default/full/ordered submissions, cyan fullbright and smooth fade passed");
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
