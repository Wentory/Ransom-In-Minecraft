#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float EffectTime;
uniform float LayerDepth;
uniform vec2 ViewOffset;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), f.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0)), f.x), f.y);
}
float mist(vec2 p) {
    float value = 0.0;
    float weight = 0.57;
    for (int i = 0; i < 3; i++) {
        value += noise(p) * weight;
        p = mat2(0.8, -0.6, 0.6, 0.8) * p * 2.03 + vec2(7.1, 3.4);
        weight *= 0.5;
    }
    return value;
}
void main() {
    vec4 mask = texture(Sampler0, texCoord0);
    float alpha = mask.a * vertexColor.a * ColorModulator.a;
    // Empty texels must not occlude the world. Partial alpha remains blended.
    if (alpha <= 0.0) discard;
    vec2 p = (texCoord0 - 0.5) * 2.0;
    float t = EffectTime;
    // Separate drift and view-dependent offsets make the layers visibly slide past each other.
    float farMist = mist((p + ViewOffset * 1.9) * 3.0
                         + vec2(t * 0.18, -t * 0.12) + LayerDepth);
    float nearMist = mist((p + ViewOffset * 0.7) * 5.5
                          + vec2(-t * 0.27, t * 0.19) - LayerDepth * 1.7);
    float detail = mist(p * 10.0 + vec2(t * 0.10, t * 0.24));
    float depth = farMist * 0.45 + nearMist * 0.45 + detail * 0.10;
    float clouds = smoothstep(0.28, 0.60, depth);
    vec3 moonlight = mix(vec3(0.42, 0.82, 1.0), vec3(0.62, 0.96, 1.0), clouds);
    float pulse = 0.97 + 0.03 * sin(t * 1.10 + LayerDepth * 0.45);

    // Bright fronts move across the narrow painted lines every few seconds.
    float front = sin(dot(p, vec2(5.0, 3.5)) - t * 1.8 + nearMist * 2.0 + LayerDepth);
    float ribbon = smoothstep(0.68, 0.96, front);
    moonlight = mix(moonlight * pulse, vec3(0.73, 0.98, 1.0), ribbon * 0.40);

    // Small moving light points behind the mask, like a shallow portal surface.
    vec2 stars = (p + ViewOffset * 1.3) * 9.0 + vec2(t * 0.12, -t * 0.18);
    vec2 cell = floor(stars);
    float seed = hash(cell + LayerDepth * 8.0);
    vec2 center = vec2(hash(cell + 2.7), hash(cell + 11.3));
    float point = (1.0 - smoothstep(0.045, 0.16, length(fract(stars) - center)))
                   * step(0.72, seed) * (0.6 + 0.4 * sin(t * 2.5 + seed * 30.0));
    moonlight = mix(moonlight, vec3(0.92, 1.0, 1.0), point * 0.85);
    // Unlit, no bloom/halo; retain the texture's partial alpha.
    fragColor = vec4(mask.rgb * moonlight * vertexColor.rgb * ColorModulator.rgb,
                     alpha);
}
