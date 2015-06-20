package aviss.visuals;

import java.nio.IntBuffer;

import gltoolbox.GeometryTools;
import gltoolbox.RenderTarget;
import gltoolbox.TextureFactory;


import oscP5.OscMessage;
import processing.core.*;
import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;
import aviss.applet.AVISSApplet;
import aviss.applet.PManager;
import aviss.audio.AudioManager;
import aviss.data.GPUFluid;
import aviss.data.GPUParticles;
import aviss.data.Utils;

public class KinectFluidGenerator implements AVGenerator {

	private AVISSApplet pApp;
	private AudioManager aMan;
	
	private GPUFluid fluid;
	private GPUParticles particles;
	
	private IntBuffer textureQuad;
	private PShader screenTextureShader;
	private PShader renderParticlesShader;
	private PShader mouseDyeShader;
	private PShader mouseForceShader;
	private RenderTarget offScreenTarget;
	
	// window
	private boolean mousePointKnown = false;
	private boolean lastMousePointKnown = false;
	private PVector mouseClipSpace = new PVector(0, 0);
	private PVector lastMouseClipSpace = new PVector(0, 0);
	private float time;
	private float lastTime;
	
	// drawing
	private boolean renderParticlesEnabled = true;
	private boolean renderFluidEnabled = true;
	private int particleCount = 10000;
	private float fluidScale = 1;
	private int fluidIterations;
	private float offScreenScale;
	
	private static boolean OFFSCREEN_RENDER = true;
	
	private enum SimulationQuality{
		UltraHigh,
		High,
		Medium,
		Low,
		UltraLow
	}
	
	public KinectFluidGenerator(){
		setQuality(SimulationQuality.Medium);
	}
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) 
	{
		pApp = applet;
		aMan = am;

		textureQuad = GeometryTools.createQuad(0, 0, 1, 1, PGL.TRIANGLE_STRIP);
//		offScreenTarget = new RenderTarget(
//						  	Math.round(pApp.width*offScreenScale),
//						  	Math.round(pApp.height*offScreenScale),
//						  	new TextureFactory()
//							);
		
		// loading shaders
		String vertShader = getClass().getResource("/aviss/shaders/gpufluid/no-transform.vert.glsl").getPath();
		String fragShader = getClass().getResource("/aviss/shaders/gpufluid/quad-texture.frag.glsl").getPath();
		screenTextureShader = pApp.loadShader(fragShader, vertShader);
		vertShader = getClass().getResource("/aviss/shaders/gpuparticles/renderParticles.vert.glsl").getPath();
		fragShader = getClass().getResource("/aviss/shaders/gpuparticles/renderParticles.frag.glsl").getPath();		
		renderParticlesShader = pApp.loadShader(fragShader, vertShader);
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();		
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/mouseDye.frag.glsl").getPath();
		mouseDyeShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/mouseForce.frag.glsl").getPath();
		mouseForceShader = pApp.loadShader(fragShader, vertShader);
		
		mouseDyeShader.set("mouseClipSpace", mouseClipSpace);
		mouseDyeShader.set("lastMouseClipSpace", lastMouseClipSpace);
		mouseDyeShader.set("dye", 1);
		mouseForceShader.set("mouseClipSpace", mouseClipSpace);
		mouseForceShader.set("lastMouseClipSpace", lastMouseClipSpace);
		mouseForceShader.set("velocity", 1);
		screenTextureShader.set("texture", 1);
		
		PJOGL pgl = PManager.getPGL();
		pgl.disable(PGL.DEPTH_TEST);
		pgl.disable(PGL.CULL_FACE);
		pgl.disable(PGL.DITHER);
		
		fluid = new GPUFluid(Math.round(pApp.width * fluidScale), 
							 Math.round(pApp.height * fluidScale),
							 32f, fluidIterations);
		fluid.setUpdateDyeShader(mouseDyeShader);
		fluid.setApplyForcesShader(mouseForceShader);
		
//		particles = new GPUParticles(particleCount, applet);
//		particles.setFlowScale(fluid.simToClipSpace(new PVector(1,1)));
//		particles.setDragCoefficient(1f);
//		
		lastTime = pApp.millis();
		PManager.endPGL();

	}

	@Override
	public void reInit() {
		// TODO Auto-generated method stub		
	}

	@Override
	public void run() 
	{
		PJOGL pgl = PManager.getPGL();
		
		// *********REPLACE WITH KINECT HANDS UPDATE******
		mouseClipSpace = windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		mousePointKnown = true;
		//**********************
		
		time = pApp.millis();
		float dt = time - lastTime;
		lastTime = time;
		
		if(lastMousePointKnown && mousePointKnown){
			mouseDyeShader.set("isMouseDown", pApp.mousePressed);
			mouseForceShader.set("isMouseDown", pApp.mousePressed);
			mouseDyeShader.set("mouseClipSpace", mouseClipSpace);
			mouseDyeShader.set("lastMouseClipSpace", lastMouseClipSpace);
			mouseForceShader.set("mouseClipSpace", mouseClipSpace);
			mouseForceShader.set("lastMouseClipSpace", lastMouseClipSpace);
		}
		
		// step physics
		fluid.step(dt);
		
//		particles.setFlowVelocityField(fluid.velocityRenderTarget.readFromTexture);
//		if(renderParticlesEnabled)
//			particles.step(dt);
		
		//render to offScreen
//		if(OFFSCREEN_RENDER){
//			pgl.viewport (0, 0, offScreenTarget.width, offScreenTarget.height);
//			pgl.bindFramebuffer(PGL.FRAMEBUFFER, offScreenTarget.fbo.get(0));
//		}
//		else{
		pgl.viewport (0, 0, pApp.width, pApp.height);
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);
//		}
		
		pgl.clearColor(0,0,0,1);
		pgl.clear(PGL.COLOR_BUFFER_BIT);
		
		pgl.enable(PGL.BLEND);
		pgl.blendFunc(PGL.SRC_ALPHA, PGL.SRC_ALPHA);
		pgl.blendEquation(PGL.FUNC_ADD);
		
//		if(renderParticlesEnabled)
//			renderParticles(pgl);
		
		if(renderFluidEnabled)
			renderTexture(fluid.dyeRenderTarget.readFromTexture);
			

		// *********REPLACE WITH KINECT HANDS UPDATE******
		lastMouseClipSpace = windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		lastMousePointKnown = mousePointKnown;
		//**********************
		
		pgl.disable(PGL.BLEND);				
		PManager.endPGL();
		pgl = null;
	}

	@Override
	public void keyPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void oscEvent(OscMessage m) {
		// TODO Auto-generated method stub
		
	}

	private void renderTexture(IntBuffer texture)
	{
		PJOGL pgl = PManager.getPGL();
		
		// bind inner quad
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));
		
		// loading texture 
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, texture.get(0)); 
//		screenTextureShader.set("texture", texture.get(0));	
		
		screenTextureShader.bind();
		pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
		screenTextureShader.unbind();
		
		
		
		//System.out.println("screenTextureShader");	
//		pApp.shader(screenTextureShader);
//		
//		pApp.noStroke();
//		pApp.beginShape(PApplet.TRIANGLE_STRIP);
//		pApp.vertex(0, pApp.height, 0, 1);
//		pApp.vertex(0, 0, 0, 0);
//		pApp.vertex(pApp.width, pApp.height, 1, 1);
//		pApp.vertex(pApp.width, 0, 1, 0);
//		pApp.endShape();

	}
	
	private void renderParticles(PJOGL pgl)
	{
//		renderParticlesShader.set("particleData", particles.particleData.readFrom.get());		
//		renderParticlesShader.bind();		
//		int vertexLocation = pgl.getAttribLocation(renderParticlesShader.glProgram, "vertex");
//		pgl.enableVertexAttribArray(vertexLocation);
//		pgl.bindBuffer(PGL.ARRAY_BUFFER, particles.particleBufferID.get(0));
//		pgl.bufferData(PGL.ARRAY_BUFFER,  particles.particleUVs.capacity(), particles.particleUVs, PGL.STATIC_DRAW);
//		pgl.vertexAttribPointer(vertexLocation, 2, PGL.FLOAT, false, 0, 0);		
//		pgl.drawArrays(PGL.POINTS, 0, particles.particleUVs.capacity());
//		renderParticlesShader.unbind();
	}
	
	private void setQuality(SimulationQuality simQ){
		switch (simQ) {
		case UltraHigh:
			particleCount = 1 << 20;
			fluidScale = 0.5f;
			setFluidIterations(30);
			offScreenScale = 1/1;
		case High:
			particleCount = 1 << 20;
			fluidScale = 0.25f;
			setFluidIterations(20);
			offScreenScale = 1/1;
		case Medium:
			particleCount = 1 << 18;
			fluidScale = 0.25f;
			setFluidIterations(18);
			offScreenScale = 1/1;
		case Low:
			particleCount = 1 << 16;
			fluidScale = 0.2f;
			setFluidIterations(14);
			offScreenScale = 1/1;
		case UltraLow:
			particleCount = 1 << 14;
			fluidScale = 1/6f;
			setFluidIterations(12);
			offScreenScale = 1/2;
		}
	}
	
	private void setFluidIterations(int v)
	{
		fluidIterations = v;
		if(fluid != null) 
			fluid.solverIterations = v;
	}
	
	private void reset()
	{
		particles.reset();
		fluid.clear();
	}
	
	private PVector windowToClipSpace(PVector v)
	{	
		return new PVector((v.x/pApp.width)*2 - 1, ((pApp.height-v.y)/pApp.height)*2 - 1);	
	}	
}
	