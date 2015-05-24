uniform sampler2D texture;

varying vec4 vertTexCoord;

void main() {
  vec3 destColor = texture2D(texture, vertTexCoord.st).rgb;
 
  gl_FragColor = vec4(destColor, 1.0);  
}