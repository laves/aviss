#define PRESSURE_BOUNDARY
#define VELOCITY_BOUNDARY

uniform vec3 invresolution;
uniform float aspectRatio;

uniform sampler2D pressure;
uniform sampler2D velocity;
uniform float halfrdx;
	
varying vec2 texelCoord;

//sampling pressure texture factoring in boundary conditions
float samplePressue(sampler2D pressure, vec2 coord){
    vec2 cellOffset = vec2(0.0, 0.0);

    //pure Neumann boundary conditions: 0 pressure gradient across the boundary
    //dP/dx = 0
    //walls
    #ifdef PRESSURE_BOUNDARY
    if(coord.x < 0.0)      cellOffset.x = 1.0;
    else if(coord.x > 1.0) cellOffset.x = -1.0;
    if(coord.y < 0.0)      cellOffset.y = 1.0;
    else if(coord.y > 1.0) cellOffset.y = -1.0;
    #endif

    return texture2D(pressure, coord + cellOffset * invresolution.xy).x;
}

	
void main(void){
  float L = samplePressue(pressure, texelCoord - vec2(invresolution.x, 0));
  float R = samplePressue(pressure, texelCoord + vec2(invresolution.x, 0));
  float B = samplePressue(pressure, texelCoord - vec2(0, invresolution.y));
  float T = samplePressue(pressure, texelCoord + vec2(0, invresolution.y));

  vec2 v = texture2D(velocity, texelCoord).xy;

  gl_FragColor = vec4(v - halfrdx*vec2(R-L, T-B), 0, 1);
}
