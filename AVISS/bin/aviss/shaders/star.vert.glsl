#define PROCESSING_TEXTURE_SHADER

uniform mat4 transform;
attribute vec4 vertex;

void main() {
  gl_Position = transform * vertex;    
}