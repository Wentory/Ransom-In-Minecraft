#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 Cells;
uniform float Progress;
uniform float InventoryThreshold;
uniform float Opacity;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec2 uv = clamp((floor(texCoord0 * Cells) + 0.5) / Cells, vec2(0.0), vec2(1.0));
    vec4 color = texture(Sampler0, uv);
    if (uv.x >= 2.0 / 128.0 && uv.x < (2.0 + 124.0 * Progress) / 128.0) {
        vec4 fill = texture(Sampler1, uv) * vec4(147.0 / 255.0, 239.0 / 255.0, 1.0, 1.0);
        float alpha = fill.a + color.a * (1.0 - fill.a);
        color.rgb = (fill.rgb * fill.a + color.rgb * color.a * (1.0 - fill.a)) / max(alpha, 0.0001);
        color.a = alpha;
    }
    float threshold = (2.0 + 124.0 * InventoryThreshold) / 128.0;
    if (abs(uv.x - threshold) < 0.5 / Cells.x && uv.y > 1.0 / 16.0 && uv.y < 15.0 / 16.0)
        color = vec4(1.0);
    fragColor = color * vertexColor * ColorModulator;
    fragColor.a *= Opacity;
    if (fragColor.a < 0.001) discard;
}
