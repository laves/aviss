varying vec2 texelCoord;

void main(){
	vec2 ip = vec2((texelCoord.x), (texelCoord.y)) * 2.0 - 1.0;
	vec2 iv = vec2(0,0);
	gl_FragColor = vec4(ip, iv);
}