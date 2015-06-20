uniform mat4 transform;
uniform sampler2D particleData;

attribute vec2 vertex;

varying vec4 color;

void main()
{
	vec2 p = texture2D(particleData, vertex).xy;
	vec2 v = texture2D(particleData, vertex).zw;
	gl_PointSize = 1.0;
	gl_Position = transform * vec4(p, 0.0, 1.0);
	float speed = length(v);
	float x = clamp(speed * 4.0, 0., 1.);
	color.rgb = (
			mix(vec3(40.4, 0.0, 35.0) / 300.0, vec3(0.2, 47.8, 100) / 100.0, x)
			+ (vec3(63.1, 92.5, 100) / 100.) * x*x*x * 0.1
	);
	color.a = 1.0;
}