package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class CrucifixActivationFlash {
    private static long started;
    static void start(Minecraft mc){started=System.nanoTime();}
    public static void clear(){started=0;}
    public static void render(GuiGraphicsExtractor graphics){
        if(started==0)return;
        var mc=Minecraft.getInstance();
        float t=(System.nanoTime()-started)/350_000_000F;
        if(t>=1 || mc.player==null || mc.level==null){clear();return;}
        int alpha=Math.round(230*(1-t)*(1-t));
        graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),alpha<<24|0xB9F3FF);
    }
    private CrucifixActivationFlash(){}
}
