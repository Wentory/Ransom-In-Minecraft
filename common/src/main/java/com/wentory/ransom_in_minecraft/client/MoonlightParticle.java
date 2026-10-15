package com.wentory.ransom_in_minecraft.client;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
/** Engine-managed particle, fixed authored texture and a smooth lifetime envelope. */
final class MoonlightParticle extends SingleQuadParticle {
    private static final Layer[] LAYERS = java.util.Arrays.stream(new String[]{"tiny","small","normal","big"})
            .map(name -> new Layer(true, Identifier.fromNamespaceAndPath("ransom_in_minecraft",
                    "textures/item/crucifix/moonlight_star_" + name + ".png"), MoonlightPipelines.PARTICLE))
            .toArray(Layer[]::new);
    private static final Layer[] COMPATIBLE_LAYERS=java.util.Arrays.stream(LAYERS)
            .map(layer->new Layer(true,layer.textureAtlasLocation(),net.minecraft.client.renderer.RenderPipelines.TRANSLUCENT_PARTICLE)).toArray(Layer[]::new);
    private final Layer layer;
    private final float size;
    private final MoonlightEffects.Profile profile;
    private final double outwardX, outwardZ;
    private final boolean pulsing;
    private final float pulsePhase, pulseSpeed, pulseAmplitude;
    private final double originY, heightFadeStart, heightFadeEnd;
    private final float rotationSpeed;

    MoonlightParticle(ClientLevel level, Vec3 position, Vec3 velocity, Vec3 outward,
                      MoonlightEffects.Profile profile) {
        super(level, position.x, position.y, position.z, null);
        this.profile = profile;
        originY = position.y;
        if (profile == MoonlightEffects.GEYSER) {
            heightFadeEnd = Mth.lerp(random.nextDouble(), MoonlightEffects.GEYSER_FADE_END_MIN,
                    MoonlightEffects.GEYSER_FADE_END_MAX);
            heightFadeStart = Math.min(heightFadeEnd - 0.5,
                    Mth.lerp(random.nextDouble(), MoonlightEffects.GEYSER_FADE_START_MIN,
                            MoonlightEffects.GEYSER_FADE_START_MAX));
        } else if (profile == MoonlightEffects.AMBIENT) {
            heightFadeEnd = Mth.lerp(random.nextDouble(), MoonlightEffects.AMBIENT_FADE_END_MIN,
                    MoonlightEffects.AMBIENT_FADE_END_MAX);
            heightFadeStart = Math.max(1, heightFadeEnd - 1.5);
        } else {
            heightFadeStart = heightFadeEnd = 0;
        }
        layer = LAYERS[random.nextInt(LAYERS.length)];
        size = profile.size() * (0.8F + random.nextFloat() * 0.4F);
        lifetime = profile.lifeMin() + random.nextInt(profile.lifeVariation() + 1);
        xd = velocity.x;
        yd = velocity.y;
        zd = velocity.z;
        outwardX = outward.x;
        outwardZ = outward.z;
        hasPhysics = false;
        gravity = 0;
        pulsing = random.nextFloat() < MoonlightEffects.PULSE_CHANCE;
        pulsePhase = random.nextFloat() * Mth.TWO_PI;
        pulseSpeed = 0.045F + random.nextFloat() * 0.035F;
        pulseAmplitude = 0.05F + random.nextFloat() * 0.07F;
        rotationSpeed = random.nextFloat() < MoonlightEffects.ROTATION_CHANCE
                ? (float) Math.toRadians(MoonlightEffects.ROTATION_DEGREES_PER_SECOND)
                    / 20 * (0.6F + random.nextFloat() * 0.4F) * (random.nextBoolean() ? 1 : -1)
                : 0;
        setSize(size * 2, size * 2);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        // The geyser widens gradually after rising; no per-tick random forces or gravity.
        double spread = profile.spreadAcceleration() * Math.sin(Math.PI * Math.min(age / 30.0, 1));
        xd = xd * profile.drag() + outwardX * spread;
        zd = zd * profile.drag() + outwardZ * spread;
        yd = yd * profile.drag() + profile.drift() * (1 - profile.drag());
        move(xd, yd, zd);
    }


    @Override public void extract(QuadParticleRenderState state, Camera camera, float partial) {
        float time=age+partial;
        float fade=smooth(time/MoonlightEffects.FADE_IN_TICKS)*smooth((lifetime-time)/profile.fadeOutTicks());
        if(heightFadeEnd>0) {
            double height=Mth.lerp(partial,yo,y)-originY;
            fade*=1-smooth((float)((height-heightFadeStart)/(heightFadeEnd-heightFadeStart)));
        }
        alpha=fade;
        boolean compatible=CrucifixIrisCompatibility.useCompatibleRenderer();
        rCol=compatible?147/255F:1;gCol=compatible?239/255F:1;bCol=1;
        quadSize=size*(pulsing?1+pulseAmplitude*Mth.sin(time*pulseSpeed+pulsePhase):1);
        roll=time*rotationSpeed; oRoll=roll;
        super.extract(state,camera,partial);
    }
    private static float smooth(float value) {float t=Mth.clamp(value,0,1);return t*t*(3-2*t);}
    @Override protected Layer getLayer(){
        if(!CrucifixIrisCompatibility.useCompatibleRenderer())return layer;
        for(int i=0;i<LAYERS.length;i++)if(layer==LAYERS[i])return COMPATIBLE_LAYERS[i];
        return layer;
    }
    @Override protected float getU0(){return 0;}
    @Override protected float getU1(){return 1;}
    @Override protected float getV0(){return 0;}
    @Override protected float getV1(){return 1;}
    @Override protected int getLightCoords(float partial){return 0xF000F0;}
}
