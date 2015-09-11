uniform sampler2D velocity;
uniform float dt;
uniform float dx;
	
uniform vec3 invresolution;
uniform float aspectRatio;

uniform bool isMouseDown;
uniform vec3 mouse;
uniform vec3 lastMouse;

varying vec2 texelCoord;
varying vec2 p;

//Segment
float distanceToSegment(vec2 a, vec2 b, vec2 p, out float fp){
	vec2 d = p - a;
	vec2 x = b - a;

	fp = 0.0; //fractional projection, 0 - 1 in the length of vec2(b - a)
	float lx = length(x);
	
	if(lx <= 0.0001) return length(d);//#! needs improving; hot fix for normalization of 0 vector

	float projection = dot(d, x / lx); //projection in pixel units

	fp = projection / lx;

	if(projection < 0.0)            return length(d);
	else if(projection > length(x)) return length(p - b);
	return sqrt(abs(dot(d,d) - projection*projection));
}


void main(){
	vec2 v = texture2D(velocity, texelCoord).xy;
	v.xy *= 0.999;
	if(isMouseDown){
		vec2 mouseVelocity = -(lastMouse.xy - mouse.xy)/dt;
		// mouse = mouse - (lastMouse - mouse) * 2.0;//predict mouse position
			
		//compute tapered distance to mouse line segment
		float projection;
		float l = distanceToSegment(mouse.xy, lastMouse.xy, p, projection);
		float taperFactor = 0.6;//1 => 0 at lastMouse, 0 => no tapering
		float projectedFraction = 1.0 - clamp(projection, 0.0, 1.0)*taperFactor;
		float R = 0.015;
		float m = exp(-l/R); //drag coefficient
		m *= projectedFraction * projectedFraction;
		vec2 targetVelocity = mouseVelocity * dx * 1.4;
		v += (targetVelocity - v)*m;
	}
	gl_FragColor = vec4(v, 0, 1.);
}
