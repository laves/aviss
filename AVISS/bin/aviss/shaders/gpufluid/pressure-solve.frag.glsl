#define PRESSURE_BOUNDARY
#define VELOCITY_BOUNDARY

uniform vec2 invresolution;
uniform float aspectRatio;

uniform sampler2D pressure;
uniform sampler2D divergence;
uniform float alpha;//alpha = -(dx)^2, where dx = grid cell size

varying vec2 texelCoord;

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



vec2 clipToSimSpace(vec2 clipSpace){
    return  vec2(clipSpace.x * aspectRatio, clipSpace.y);
}

vec2 simToTexelSpace(vec2 simSpace){
    return vec2(simSpace.x / aspectRatio + 1.0 , simSpace.y + 1.0)*.5;
}

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

    return texture2D(pressure, coord + cellOffset * invresolution).x;
}

//sampling velocity texture factoring in boundary conditions
vec2 sampleVelocity(sampler2D velocity, vec2 coord){
    vec2 cellOffset = vec2(0.0, 0.0);
    vec2 multiplier = vec2(1.0, 1.0);

    //free-slip boundary: the average flow across the boundary is restricted to 0
    //avg(uA.xy, uB.xy) dot (boundary normal).xy = 0
    //walls
    #ifdef VELOCITY_BOUNDARY
    if(coord.x<0.0){
        cellOffset.x = 1.0;
        multiplier.x = -1.0;
    }else if(coord.x>1.0){
        cellOffset.x = -1.0;
        multiplier.x = -1.0;
    }
    if(coord.y<0.0){
        cellOffset.y = 1.0;
        multiplier.y = -1.0;
    }else if(coord.y>1.0){
        cellOffset.y = -1.0;
        multiplier.y = -1.0;
    }
    #endif

    return multiplier * texture2D(velocity, coord + cellOffset * invresolution).xy;
}