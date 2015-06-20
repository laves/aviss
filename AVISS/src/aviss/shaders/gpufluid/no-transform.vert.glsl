attribute vec4 vertex;
varying vec2 texelCoord;

void main() {
	texelCoord = vertex.xy;
	gl_Position = vec4(vertex.xy*2.0 - vec2(1.0, 1.0), 0.0, 1.0 );//converts to clip space	
}