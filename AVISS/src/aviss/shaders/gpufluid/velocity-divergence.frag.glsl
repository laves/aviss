#define PRESSURE_BOUNDARY
#define VELOCITY_BOUNDARY

uniform vec3 invresolution;
uniform float aspectRatio;

uniform sampler2D velocity;	//vector fields
uniform float halfrdx;	// .5*1/gridscale

varying vec2 texelCoord;

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

    return multiplier * texture2D(velocity, coord + cellOffset * invresolution.xy).xy;
}

void main(void){
	//compute the divergence according to the finite difference formula
 	//texelSize = 1/resolution
	vec2 L = sampleVelocity(velocity, texelCoord - vec2(invresolution.x, 0));
	vec2 R = sampleVelocity(velocity, texelCoord + vec2(invresolution.x, 0));
	vec2 B = sampleVelocity(velocity, texelCoord - vec2(0, invresolution.y));
	vec2 T = sampleVelocity(velocity, texelCoord + vec2(0, invresolution.y));

	gl_FragColor = vec4( halfrdx * ((R.x - L.x) + (T.y - B.y)), 0, 0, 1);
}