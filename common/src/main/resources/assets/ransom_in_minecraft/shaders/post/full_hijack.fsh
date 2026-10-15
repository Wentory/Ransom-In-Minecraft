#version 330
uniform sampler2D InSampler;
layout(std140) uniform HijackParams {
    float Strength;
    float ViewX;
    float ViewY;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    vec2 view = vec2(ViewX, ViewY);
    float edge = max(0.0, (1.0 - p.x * p.x) * (1.0 - p.y * p.y));
    p = p / (1.0 + dot(p, view) * 0.065 * edge) - view * 0.022 * edge;
    float angle = max(Strength * 5.0, 0.0001);
    float arcX = sin(p.x * angle) / sin(angle);
    vec2 uv = clamp(vec2(arcX, p.y) * 0.5 + 0.5, vec2(0.0), vec2(1.0));
    fragColor = vec4(texture(InSampler, uv).rgb, 1.0);
}
