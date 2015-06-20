uniform sampler2D texture;
varying vec2 texelCoord;
void main(){
	gl_FragColor = texture2D(texture, texelCoord);
}