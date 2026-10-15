#version 150
uniform sampler2D DiffuseSampler;
uniform float Strength;
uniform float ViewX;
uniform float ViewY;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    vec2 view = vec2(ViewX, ViewY);
    // A restrained perspective lean, fading at the perimeter to keep the screen covered.
    float edge = max(0.0, (1.0 - p.x * p.x) * (1.0 - p.y * p.y));
    p = p / (1.0 + dot(p, view) * 0.065 * edge) - view * 0.022 * edge;
    // Reverse fisheye: a concave horizontal screen compresses the centre,
    // with gradually increasing stretch towards its sides.
    // Every row uses the same rounded arc: there is no vertical dependence.
    float angle = max(Strength * 5.0, 0.0001);
    float arcX = sin(p.x * angle) / sin(angle);
    vec2 uv = clamp(vec2(arcX, p.y) * 0.5 + 0.5, vec2(0.0), vec2(1.0));
    // The completed frame is opaque. GUI alpha must not turn the final blit black.
    fragColor = vec4(texture(DiffuseSampler, uv).rgb, 1.0);
}
