package aviss.visuals;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;
import aviss.applet.AVISSApplet;
import aviss.applet.AppletManager;
import aviss.audio.AudioManager;
import aviss.data.GPUFluid;
import aviss.data.GPUParticles;

public class KinectFluidGenerator implements AVGenerator {

	private AVISSApplet pApp;
	private AudioManager aMan;
	
	private GPUFluid fluid;
	private GPUParticles particles;
	
	private PShader screenTextureShader;
	private PShader renderParticlesShader;
	private PShader updateDyeShader;
	private PShader mouseForceShader;
	private PGraphics offScreenTarget;
	
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
	private SimulationQuality simulationQuality = SimulationQuality.Medium;
	
	private static boolean OFFSCREEN_RENDER = true;
	
	private enum SimulationQuality{
		UltraHigh,
		High,
		Medium,
		Low,
		UltraLow
	}
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) 
	{
		// TODO Auto-generated method stub
		pApp = applet;
		aMan = am;
		
		PJOGL pgl = AppletManager.startPGL();
		pgl.disable(PGL.DEPTH_TEST);
		pgl.disable(PGL.CULL_FACE);
		pgl.disable(PGL.DITHER);
		AppletManager.endPGL();
		
		// loading shaders
		String vertShader = getClass().getResource("gpufluid/shaders/glsl/fluid/texel-space.vert.glsl").getPath();
		String fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/advect.frag.glsl").getPath();
		screenTextureShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/velocity-divergence.frag.glsl").getPath();
		renderParticlesShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/pressure-solve.frag.glsl").getPath();
		updateDyeShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/pressure-gradient-subtract.frag.glsl").getPath();
		mouseForceShader = pApp.loadShader(fragShader, vertShader);
		
		offScreenTarget = pApp.createGraphics(pApp.width, pApp.height, PApplet.OPENGL);
		updateDyeShader.set("mouseClipSpace", mouseClipSpace);
		updateDyeShader.set("lastMouseClipSpace", lastMouseClipSpace);
		mouseForceShader.set("mouseClipSpace", mouseClipSpace);
		mouseForceShader.set("lastMouseClipSpace", lastMouseClipSpace);
		
		setFluidIterations(18);
		float cellScale = 32;
		fluid = new GPUFluid((int)(pApp.width * fluidScale), 
					(int)(pApp.height * fluidScale),
					cellScale, fluidIterations, applet);
		fluid.setUpdateDyeShader(updateDyeShader);
		fluid.setApplyForcesShader(mouseForceShader);
		
		particles = new GPUParticles(particleCount);
		particles.setFlowScale(fluid.simToClipSpace(new PVector(1,1)));
		particles.setDragCoefficient(1);
		
		lastTime = pApp.millis();

	}

	@Override
	public void reInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		
		// *********REPLACE WITH KINECT HANDS UPDATE******
		mouseClipSpace = windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		mousePointKnown = true;
		//**********************
		
		time = pApp.millis();
		float dt = time - lastTime;
		lastTime = time;
		
		if(lastMousePointKnown){
			updateDyeShader.set("isMouseDown", pApp.mousePressed);
			mouseForceShader.set("isMouseDown", pApp.mousePressed);
		}
		
		// step physics
		fluid.step(dt);
		
		particles.setFlowVelocityField(fluid.velocityRenderTarget);
		if(renderParticlesEnabled)
			particles.step(dt);
		
		PJOGL pgl = AppletManager.startPGL();
		pgl.enable(PGL.BLEND);
		pgl.blendFunc(PGL.SRC_ALPHA, PGL.SRC_ALPHA);
		pgl.blendEquation(PGL.FUNC_ADD);
		
		if(renderParticlesEnabled)
			renderParticles();
		if(renderFluidEnabled)
			renderTexture(fluid.dyeRenderTarget);
		
		pgl.disable(PGL.BLEND);
		
		// *********REPLACE WITH KINECT HANDS UPDATE******
		lastMouseClipSpace = windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		lastMousePointKnown = mousePointKnown;
		//**********************
	}

	@Override
	public void keyPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void oscEvent(OscMessage m) {
		// TODO Auto-generated method stub
		
	}
	
	private void renderTexture(PGraphics pg){
		screenTextureShader.set("texture", pg);
		
		pApp.shader(screenTextureShader);
		pApp.noStroke();
		
		pApp.beginShape(PApplet.TRIANGLE_STRIP);
		pApp.vertex(0, pApp.height, 0, 1);
		pApp.vertex(0, 0, 0, 0);
		pApp.vertex(pApp.width, pApp.height, 1, 1);
		pApp.vertex(pApp.width, 0, 1, 0);
		pApp.endShape();
		
	}
	
	private void renderParticles(){
		renderParticlesShader.set("particlesData", x);
	}
	
	private void setQuality(SimulationQuality simQ){
		switch (simQ) {
		case UltraHigh:
			particleCount = 1 << 20;
			fluidScale = 1/2;
			setFluidIterations(30);
			offScreenScale = 1/1;
		case High:
			particleCount = 1 << 20;
			fluidScale = 1/4;
			setFluidIterations(20);
			offScreenScale = 1/1;
		case Medium:
			particleCount = 1 << 18;
			fluidScale = 1/4;
			setFluidIterations(18);
			offScreenScale = 1/1;
		case Low:
			particleCount = 1 << 16;
			fluidScale = 1/5;
			setFluidIterations(14);
			offScreenScale = 1/1;
		case UltraLow:
			particleCount = 1 << 14;
			fluidScale = 1/6;
			setFluidIterations(12);
			offScreenScale = 1/2;
		}
	}
	
	private void setFluidIterations(int v){
		fluidIterations = v;
		if(fluid != null) 
			fluid.solverIterations = v;
	}
	
	private void reset(){
		particles.reset();
		fluid.clear();
	}
	
	private PVector windowToClipSpace(PVector v){	
		return new PVector((v.x/pApp.width)*2 - 1, ((pApp.height-v.y)/pApp.height)*2 - 1);	
	}
	
}
	