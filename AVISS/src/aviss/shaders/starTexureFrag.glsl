#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;

uniform vec2 texOffset;

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main() {
	vec2 longitudeLatitude = vec2((atan(vertTexCoord.y, vertTexCoord.x) / 3.1415926 + 1.0) * 0.5,
                                  (asin(vertTexCoord.z) / 3.1415926 + 0.5));
	gl_FragColor = texture2D(texture, longitudeLatitude);
}