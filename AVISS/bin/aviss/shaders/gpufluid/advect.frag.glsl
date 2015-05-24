uniform sampler2D velocity;
uniform sampler2D target;
uniform float dt;
uniform float rdx; //reciprocal of grid scale, used to scale velocity into simulation domain

uniform vec2 invresolution;
uniform float aspectRatio;

varying vec2 texelCoord;
varying vec2 p;

void main(void){
  //texelCoord refers to the center of the texel! Not a corner!
  
  vec2 tracedPos = p - dt * rdx * texture2D(velocity, texelCoord ).xy;

  //Bilinear Interpolation of the target field value at tracedPos 
  tracedPos = simToTexelSpace(tracedPos)/invresolution; // texel coordinates
  
  vec4 st;
  st.xy = floor(tracedPos-.5)+.5; //left & bottom cell centers
  st.zw = st.xy+1.;               //right & top centers

  vec2 t = tracedPos - st.xy;

  st*=invresolution.xyxy; //to unitary coords
  
  vec4 tex11 = texture2D(target, st.xy );
  vec4 tex21 = texture2D(target, st.zy );
  vec4 tex12 = texture2D(target, st.xw );
  vec4 tex22 = texture2D(target, st.zw );

  //need to bilerp this result
  gl_FragColor = mix(mix(tex11, tex21, t.x), mix(tex12, tex22, t.x), t.y);
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