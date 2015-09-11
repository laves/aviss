uniform sampler2D velocity;
uniform sampler2D target;
uniform float dt;
uniform float rdx; //reciprocal of grid scale, used to scale velocity into simulation domain

uniform vec3 invresolution;
uniform float aspectRatio;

varying vec2 texelCoord;
varying vec2 p;

vec2 aspectToTexelSpace(vec2 p){
    return vec2(p.x / aspectRatio + 1.0 , p.y + 1.0)*.5;
}

void main(void){
  //texelCoord refers to the center of the texel! Not a corner!
  
  vec2 tracedPos = p - dt * rdx * texture2D(velocity, texelCoord ).xy; //aspect space

  //Bilinear Interpolation of the target field value at tracedPos
  //convert from aspect space to texel space (0 -> 1 | x & y)
  tracedPos = aspectToTexelSpace(tracedPos);
  //convert from texel space to pixel space (0 -> w)
  tracedPos /= invresolution.xy;
  
  vec4 st;
  st.xy = floor(tracedPos-.5)+.5; //left & bottom cell centers
  st.zw = st.xy+1.;               //right & top centers

  vec2 t = tracedPos - st.xy;

  st *= invresolution.xyxy; //to unitary coords
  
  vec4 tex11 = texture2D(target, st.xy );
  vec4 tex21 = texture2D(target, st.zy );
  vec4 tex12 = texture2D(target, st.xw );
  vec4 tex22 = texture2D(target, st.zw );

  //need to bilerp this result
  gl_FragColor = mix(mix(tex11, tex21, t.x), mix(tex12, tex22, t.x), t.y);
}