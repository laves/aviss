uniform sampler2D dye;
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
	vec4 color = texture2D(dye, texelCoord);

	color.r *= (0.9797);
	color.g *= (0.9494);
	color.b *= (0.9696);
	if(isMouseDown){			
		vec2 mouse = clipToSimSpace(mouseClipSpace.xy);
		vec2 lastMouse = clipToSimSpace(lastMouseClipSpace.xy);
		vec2 mouseVelocity = -(lastMouse - mouse)/dt;
		
		//compute tapered distance to mouse line segment
		float fp;//fractional projection
		float l = distanceToSegment(mouse, lastMouse, p, fp);
		float taperFactor = 0.6;
		float projectedFraction = 1.0 - clamp(fp, 0.0, 1.0)*taperFactor;

		float R = 0.025;
		float m = exp(-l/R);
		
		float speed = length(mouseVelocity);
		float x = clamp((speed * speed * 0.02 - l * 5.0) * projectedFraction, 0., 1.);
		color.rgb += m * (
			mix(vec3(2.4, 0, 5.9) / 60.0, vec3(0.2, 51.8, 100) / 30.0, x)
			+ (vec3(100) / 100.) * pow(x, 9.)
		);
	}

	gl_FragColor = color;
}