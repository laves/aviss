package aviss.data;

import gltoolbox.GeometryTools;
import gltoolbox.RenderTarget2Phase;
import gltoolbox.TextureFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import aviss.applet.PManager;
import processing.core.*;
import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;

public class GPUParticles {

	public RenderTarget2Phase particleData;
	public FloatBuffer particleUVs;
	public IntBuffer textureQuad;
	public IntBuffer particleBufferID;
	public IntBuffer flowVelocityField;
	
	public PShader initialConditionsShader;
	public PShader stepParticlesShader;
	
	public int count;
	
	private boolean resetFlag = true;
	private PApplet pApp;
	
	public GPUParticles(int count)
	{	
		pApp = PManager.getApplet();
		textureQuad = GeometryTools.getCachedUnitQuad(PGL.TRIANGLE_STRIP);
		
		String vertShader = getClass().getResource("/aviss/shaders/gpuparticles/initialConditions.vert.glsl").getPath();
		String fragShader = getClass().getResource("/aviss/shaders/gpuparticles/initialConditions.frag.glsl").getPath();
		initialConditionsShader = PManager.getApplet().loadShader(fragShader, vertShader);
		vertShader = getClass().getResource("/aviss/shaders/gpuparticles/stepParticles.vert.glsl").getPath();
		fragShader = getClass().getResource("/aviss/shaders/gpuparticles/stepParticles.frag.glsl").getPath();
		stepParticlesShader = PManager.getApplet().loadShader(fragShader, vertShader); 
		stepParticlesShader.set("particleData", 1);
		stepParticlesShader.set("flowVelocityField", 2);
		
		//set params
		setDragCoefficient(1f);
		setFlowScale(new PVector(1f, 1f));

		//trigger creation of particle textures
		setCount(count);


	}
	
	public void step(float dt)
	{
		PJOGL pgl = PManager.getPGL();
		
		if(resetFlag){
			reset();
			resetFlag = false;
		}
		// load particleData texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, particleData.readFromTexture.get(0)); 

		// load velocity texture
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, flowVelocityField.get(0)); 	
		
		stepParticlesShader.set("dt", dt);
		
//		stepParticlesShader.set("particleData", particleData.readFrom.get());
		renderShaderTo(stepParticlesShader, particleData);		
	}
	
	public void reset(){
//		PManager.getPGL().bindTexture(PGL.TEXTURE_2D, 0);
		renderShaderTo(initialConditionsShader, particleData);
	}

	public int setCount(int newCount)
	{		
		//setup particle data
		int dataWidth = (int)Math.ceil( Math.sqrt(newCount) );
		int dataHeight = dataWidth;

		//create particle data texture
		if(particleData != null)
			particleData.resize(dataWidth, dataHeight);
		else
			particleData = new RenderTarget2Phase(dataWidth, dataHeight, new TextureFactory(null, PGL.FLOAT, null, null, null, null));

		//create particle vertex buffers that direct vertex shaders to particles to texel coordinates
		float[] particleArray = new float[dataWidth * dataHeight * 2];
		int idx = 0;
		for(int i = 0; i < dataWidth; i++){
			for(int j = 0; j < dataHeight; j++){
				particleArray[idx++] = ((float)i/(float)dataWidth); 
				particleArray[idx++] = ((float)j/(float)dataHeight);
			}
		}
		particleUVs = FloatBuffer.allocate(particleArray.length);
		particleUVs.put(particleArray);
		particleUVs.rewind();
				
		PJOGL pgl = PManager.getPGL();
		particleBufferID = IntBuffer.allocate(1);
		pgl.genBuffers(1, particleBufferID);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, particleBufferID.get(0));
		pgl.bufferData(PGL.ARRAY_BUFFER, particleArray.length, particleUVs, PGL.STATIC_DRAW);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
		
		return (this.count = newCount);
	}

	private void renderShaderTo(PShader shader, RenderTarget2Phase target)
	{	
		PJOGL pgl = PManager.getPGL();
		pgl.viewport(0, 0, target.width, target.height);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));
		
		target.activate();

		shader.bind();
		pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
		shader.unbind();

		target.swap();
		
//		target.writeTo.beginDraw();
//		target.writeTo.shader(shader);
//		target.writeTo.noStroke();
//		
//		target.writeTo.beginShape(PApplet.TRIANGLE_STRIP);
//		target.writeTo.vertex(0, pApp.height, 0, 1);
//		target.writeTo.vertex(0, 0, 0, 0);
//		target.writeTo.vertex(pApp.width, pApp.height, 1, 1);
//		target.writeTo.vertex(pApp.width, 0, 1, 0);
//		target.writeTo.endShape();
//		target.writeTo.endDraw();
//		
//		target.swap();
	}
	
	public void setDragCoefficient(float v) 
	{
		stepParticlesShader.set("dragCoefficient", v);
	}
	
	public void setFlowScale(PVector v)
	{
		stepParticlesShader.set("flowScale", v);
	}
	
	public void setFlowVelocityField(IntBuffer v)
	{  
		flowVelocityField = v;
		
		// load velocity texture
		PJOGL pgl = PManager.getPGL();
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, flowVelocityField.get(0)); 		
//		stepParticlesShader.set("flowVelocityField", v.get(0));
	}
}









