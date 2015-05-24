uniform float dt;
uniform sampler2D particleData;

uniform float dragCoefficient;
uniform vec2 flowScale;
uniform sampler2D flowVelocityField;
void main(){
	vec2 p = texture2D(particleData, texelCoord).xy;
	vec2 v = texture2D(particleData, texelCoord).zw;
	vec2 vf = texture2D(flowVelocityField, (p+1.)*.5).xy * flowScale;//(converts clip-space p to texel coordinates)
	v += (vf - v) * dragCoefficient;
	p+=dt*v;
	gl_FragColor = vec4(p, v);
}