#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV1;
out vec2 texCoord0;
out vec4 vertexColor;
flat out vec2 barInfo;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position,1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    barInfo = vec2(UV1);
}
