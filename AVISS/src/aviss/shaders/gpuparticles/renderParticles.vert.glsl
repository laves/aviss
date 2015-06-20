uniform mat4 transform;
uniform sampler2D particleData;
attribute vec2 vertex;
varying vec4 color;

void main(){
	vec2 p = texture2D(particleData, vertex).xy;
	vec2 v = texture2D(particleData, vertex).zw;
	gl_PointSize = 1.0;
	gl_Position = vec4(vertex, 0.0, 1.0);
	color = vec4(1.0, 1.0, 1.0, 1.0);
}