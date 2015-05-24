package aviss.data;

import aviss.applet.AppletManager;
import processing.core.*;
import processing.opengl.PShader;

public class GPUParticles {

	public PGraphics particleData;
	public float[] particleUVs;
	
	public PShader initialConditionsShader;
	public PShader stepParticlesShader;
	
	public float dragCoefficient;
	public PVector flowScale;
	public PImage flowVelocityField;
	
	public GPUParticles(int count)
	{	
		String vertShader = getClass().getResource("gpufluid/shaders/glsl/fluid/initialConditions.vert.glsl").getPath();
		String fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/initialConditions.frag.glsl").getPath();
		initialConditionsShader = AppletManager.getApplet().loadShader(fragShader, vertShader);
		vertShader = getClass().getResource("gpufluid/shaders/glsl/fluid/texel-space.vert.glsl").getPath();
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/advect.frag.glsl").getPath();
		stepParticlesShader = AppletManager.getApplet().loadShader(fragShader, vertShader);
		
		//set params
		this.dragCoefficient = 1;
		this.flowScale = new PVector(1, 1);

		//trigger creation of particle textures
		setCount(count);

		//write initial data
		reset();
	}
	
	public void step(float dt)
	{
		stepParticlesShader.set("dt", dt);
		stepParticlesShader.set("particleData", particleData);
		renderShaderTo(stepParticlesShader, particleData);		
	}
	
	public void reset(){
		renderShaderTo(initialConditionsShader, particleData);
	}

	public int setCount(int newCount){
		//setup particle data
		int dataWidth = (int)Math.ceil( Math.sqrt(newCount) );
		int dataHeight = dataWidth;

		PApplet pApp = AppletManager.getApplet();
		//create particle data texture
		if(particleData != null)
			particleData.resize(dataWidth, dataHeight);
		else
			particleData = pApp.createGraphics(pApp.width, pApp.height, PApplet.OPENGL);

		//create particle vertex buffers that direct vertex shaders to particles to texel coordinates
		particleUVs = new float[dataWidth * dataHeight * 2];
		int idx = 0;
		for(int i = 0; i < dataWidth; i++){
			for(int j = 0; j < dataHeight; j++){
				particleUVs[idx++] = i/dataWidth; 
				particleUVs[idx++] = j/dataHeight;
			}
		}

		return newCount;
	}

	private void renderShaderTo(PShader shader, PGraphics target){
		
		PApplet pApp = AppletManager.getApplet();
		
		target.shader(shader);
		target.noStroke();
		
		target.beginShape(PApplet.TRIANGLE_STRIP);
		target.vertex(0, pApp.height, 0, 1);
		target.vertex(0, 0, 0, 0);
		target.vertex(pApp.width, pApp.height, 1, 1);
		target.vertex(pApp.width, 0, 1, 0);
		target.endShape();
		
		pApp.image(target, 0, 0);
	}

	private float get_dragCoefficient()   	{ return dragCoefficient; }
	private PVector get_flowScale()        	{ return flowScale; }
	private PImage get_flowVelocityField()	{ return flowVelocityField; }

	private void set_dragCoefficient(float v) {
		dragCoefficient = v;
		stepParticlesShader.set("dragCoefficient", v);
	}
	private void set_flowScale(PVector v){
		flowScale = v;
		stepParticlesShader.set("flowScale", v);
	}
	private void set_flowVelocityField(PImage v){  
		flowVelocityField = v;
		stepParticlesShader.set("flowVelocityField", v);
	}
}









