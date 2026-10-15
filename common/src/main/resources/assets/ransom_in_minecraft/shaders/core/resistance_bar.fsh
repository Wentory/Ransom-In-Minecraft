#version 330
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
in vec2 texCoord0;
in vec4 vertexColor;
flat in vec2 barInfo;
out vec4 fragColor;
void main() {
    float opacity = vertexColor.a;
    float pixelSize = 1.0 + 11.0 * (1.0-opacity);
    vec2 cells = vec2(max(1.0,barInfo.y),16.0) / pixelSize;
    float progress = barInfo.x/32767.0;
    vec2 uv = clamp((floor(texCoord0*cells)+0.5)/cells,vec2(0.0),vec2(1.0));
    vec4 color = texture(Sampler0,uv);
    if (uv.x >= 2.0/128.0 && uv.x < (2.0+124.0*progress)/128.0) {
        vec4 fill = texture(Sampler1,uv)*vec4(147.0/255.0,239.0/255.0,1.0,1.0);
        float alpha = fill.a+color.a*(1.0-fill.a);
        color.rgb = (fill.rgb*fill.a+color.rgb*color.a*(1.0-fill.a))/max(alpha,0.0001);
        color.a=alpha;
    }
    float threshold=(2.0+124.0*0.30)/128.0;
    if (abs(uv.x-threshold)<0.5/cells.x && uv.y>1.0/16.0 && uv.y<15.0/16.0) color=vec4(1.0);
    fragColor=color;
    fragColor.a*=opacity;
    if (fragColor.a<0.001) discard;
}
