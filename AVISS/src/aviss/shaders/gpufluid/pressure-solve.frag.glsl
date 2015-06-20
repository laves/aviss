#define PRESSURE_BOUNDARY
#define VELOCITY_BOUNDARY

uniform vec3 invresolution;
uniform float aspectRatio;

uniform sampler2D pressure;
uniform sampler2D divergence;
uniform float alpha;//alpha = -(dx)^2, where dx = grid cell size

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
  // left, right, bottom, and top x samples
  //texelSize = 1./resolution;
  float L = samplePressue(pressure, texelCoord - vec2(invresolution.x, 0));
  float R = samplePressue(pressure, texelCoord + vec2(invresolution.x, 0));
  float B = samplePressue(pressure, texelCoord - vec2(0, invresolution.y));
  float T = samplePressue(pressure, texelCoord + vec2(0, invresolution.y));

  float bC = texture2D(divergence, texelCoord).x;

  gl_FragColor = vec4( (L + R + B + T + alpha * bC) * .25, 0, 0, 1 );//rBeta = .25
}