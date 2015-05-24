uniform sampler2D texture;

uniform ivec2 destSize;
uniform ivec4 destRect;

uniform float alpha;
uniform vec3 colourCoeffs;

varying vec4 vertTexCoord;

void main() {
  vec2 st = vertTexCoord.st;   
  
  vec2 dest = vec2(destRect.xy) / vec2(destSize) + st * vec2(destRect.zw) / vec2(destSize);
  
  vec3 destColor = texture2D(texture, dest).rgb;
 
  gl_FragColor = vec4(destColor * clamp(colourCoeffs, 0.8, 1.3), clamp(alpha, 0.0, 1));  
}