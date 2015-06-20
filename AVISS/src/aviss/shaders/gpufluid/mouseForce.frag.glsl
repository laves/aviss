uniform sampler2D velocity;
uniform float dt;
uniform float dx;
	
uniform vec3 invresolution;
uniform float aspectRatio;

uniform bool isMouseDown;
uniform vec3 mouseClipSpace;
uniform vec3 lastMouseClipSpace;

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

vec2 clipToSimSpace(vec2 clipSpace){
    return  vec2(clipSpace.x * aspectRatio, clipSpace.y);
}

void main(){
	vec2 v = texture2D(velocity, texelCoord).xy;
	v.xy *= 0.999;
	if(isMouseDown){
		vec2 mouse = clipToSimSpace(mouseClipSpace.xy);
		vec2 lastMouse = clipToSimSpace(lastMouseClipSpace.xy);
		vec2 mouseVelocity = -(lastMouse - mouse)/dt;
			
		//compute tapered distance to mouse line segment
		float fp; //fractional projection
		float l = distanceToSegment(mouse, lastMouse, p, fp);
		float taperFactor = 0.6;//1 => 0 at lastMouse, 0 => no tapering
		float projectedFraction = 1.0 - clamp(fp, 0.0, 1.0)*taperFactor;
		float R = 0.015;
		float m = exp(-l/R); //drag coefficient
		m *= projectedFraction * projectedFraction;
		vec2 targetVelocity = mouseVelocity*dx;
		v += (targetVelocity - v)*m;
	}
	gl_FragColor = vec4(v, 0, 1.);
}
