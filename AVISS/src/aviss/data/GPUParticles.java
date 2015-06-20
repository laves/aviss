package aviss.data;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import aviss.applet.PManager;
import processing.core.*;
import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;

public class GPUParticles {

	public PRenderTarget2Phase particleData;
	public FloatBuffer particleUVs;
	public IntBuffer particleBufferID;
	
	public PShader initialConditionsShader;
	public PShader stepParticlesShader;
	
	private PApplet pApp;
	
	public GPUParticles(int count, PApplet applet)
	{	
		pApp = applet;
		
		String vertShader = getClass().getResource("/aviss/shaders/gpuparticles/initialConditions.vert.glsl").getPath();
		String fragShader = getClass().getResource("/aviss/shaders/gpuparticles/initialConditions.frag.glsl").getPath();
		initialConditionsShader = PManager.getApplet().loadShader(fragShader, vertShader);
		vertShader = getClass().getResource("/aviss/shaders/gpuparticles/stepParticles.vert.glsl").getPath();
		fragShader = getClass().getResource("/aviss/shaders/gpuparticles/stepParticles.frag.glsl").getPath();
		stepParticlesShader = PManager.getApplet().loadShader(fragShader, vertShader); 
		
		//set params
		setDragCoefficient(1);
		setFlowScale(new PVector(1, 1));

		//trigger creation of particle textures
		setCount(count);

		//write initial data
		reset();
	}
	
	public void step(float dt)
	{
		stepParticlesShader.set("dt", dt);
		stepParticlesShader.set("particleData", particleData.readFrom.get());
		renderShaderTo(stepParticlesShader, particleData);		
	}
	
	public void reset(){
		renderShaderTo(initialConditionsShader, particleData);
	}

	public int setCount(int newCount){
		//setup particle data
		int dataWidth = (int)Math.ceil( Math.sqrt(newCount) );
		int dataHeight = dataWidth;

		//create particle data texture
		if(particleData != null)
			particleData.resize(dataWidth, dataHeight);
		else
			particleData = new PRenderTarget2Phase(dataWidth, dataHeight);

		//create particle vertex buffers that direct vertex shaders to particles to texel coordinates
		float[] particleArray = new float[dataWidth * dataHeight * 2];
		int idx = 0;
		for(int i = 0; i < dataWidth; i++){
			for(int j = 0; j < dataHeight; j++){
				particleArray[idx++] = ((float)i/(float)dataWidth); 
				particleArray[idx++] = ((float)j/(float)dataHeight);
			}
		}
		particleUVs = Utils.allocateDirectFloatBuffer(particleArray.length);
		particleUVs.put(particleArray);
		particleUVs.rewind();
				
		PJOGL pgl = PManager.getPGL();
		particleBufferID = Utils.allocateDirectIntBuffer(1);
		pgl.genBuffers(1, particleBufferID);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, particleBufferID.get(0));
		pgl.bufferData(PGL.ARRAY_BUFFER, particleArray.length, particleUVs, PGL.STATIC_DRAW);
		PManager.endPGL();
		
		return newCount;
	}

	private void renderShaderTo(PShader shader, PRenderTarget2Phase target)
	{	
		target.writeTo.beginDraw();
		target.writeTo.shader(shader);
		target.writeTo.noStroke();
		
		target.writeTo.beginShape(PApplet.TRIANGLE_STRIP);
		target.writeTo.vertex(0, pApp.height, 0, 1);
		target.writeTo.vertex(0, 0, 0, 0);
		target.writeTo.vertex(pApp.width, pApp.height, 1, 1);
		target.writeTo.vertex(pApp.width, 0, 1, 0);
		target.writeTo.endShape();
		target.writeTo.endDraw();
		
		target.swap();
	}
	
	public void setDragCoefficient(float v) {
		stepParticlesShader.set("dragCoefficient", v);
	}
	
	public void setFlowScale(PVector v){
		stepParticlesShader.set("flowScale", v);
	}
	
	public void setFlowVelocityField(IntBuffer v){  
		stepParticlesShader.set("flowVelocityField", v.get(0));
	}
}









