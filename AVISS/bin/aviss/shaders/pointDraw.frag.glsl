varying vec4 colour;

void main()
{
	//vec3 colour;
   // colour = vec3(0.0, 1.0, 0.0);//texture2D(texSampler, gl_PointCoord).rgb;
	gl_FragColor = clamp(colour, 0.0, 1.0);
}