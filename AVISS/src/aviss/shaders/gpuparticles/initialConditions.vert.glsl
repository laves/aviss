attribute vec2 vertex;
varying vec2 texelCoord;
void main(){
	texelCoord = vertex;
	gl_Position = vec4(vertex*2.0 - vec2(1.0, 1.0), 0.0, 1.0 );//converts to clip space	
}