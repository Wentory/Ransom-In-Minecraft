package com.wentory.ransom_in_minecraft.client;
import com.wentory.ransom_in_minecraft.network.MobBanishmentPayload;
import com.wentory.ransom_in_minecraft.network.ClientInfectedZombies;
import com.wentory.ransom_in_minecraft.network.WormCreeperPayload;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
public final class MobBanishmentVisuals {
    private static final Map<UUID,Long> STARTS=new HashMap<>();
    public static void accept(MobBanishmentPayload payload){
        MoonlightEffects.mob(payload);
        STARTS.put(payload.id(),payload.start());
        ClientInfectedZombies.remove(payload.id());
        InfectedCreeperVisuals.accept(new WormCreeperPayload(payload.id(),-1));
    }
    public static void clear(){STARTS.clear();}
    public static float progress(LivingEntity entity,float partial){
        Long start=STARTS.get(entity.getUUID());
        if(start==null)return -1;
        return net.minecraft.util.Mth.clamp((entity.level().getGameTime()-start+partial)/com.wentory.ransom_in_minecraft.CrucifixActions.MOB_BANISHMENT_TICKS,0,1);
    }
    private MobBanishmentVisuals(){}
}
