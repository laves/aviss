uniform mat4 transform;

attribute vec3 vertex;
attribute vec4 color;
attribute float size;

varying vec4 colour;

void main() {
  gl_Position = transform * vec4(vertex, 1.0);
  gl_PointSize = size;
  colour = color;
}
