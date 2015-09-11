package aviss.visuals;

import java.nio.IntBuffer;

import gltoolbox.GeometryTools;
import gltoolbox.ITargetable;
import gltoolbox.RenderTarget;
import gltoolbox.RenderTarget2Phase;
import gltoolbox.TextureFactory;
import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;
import aviss.applet.AVISSApplet;
import aviss.applet.PManager;
import aviss.audio.AudioManager;

public class GraphicsExperiments implements AVGenerator{

	private PApplet pApp;
	private PShader advectShader;
	private PShader screenTextureShader;
	private PShader mouseForceShader;
	private PShader divergenceShader;
	private PShader pressureSolveShader;
	private PShader updateDyeShader;
	private PShader pressureGradientSubtractShader;
	
	private RenderTarget2Phase velocityRenderTarget;
	private RenderTarget2Phase pressureRenderTarget;
	private RenderTarget divergenceRenderTarget;
	private RenderTarget2Phase dyeRenderTarget;	
	
	private RenderTarget offScreenTarget;
	
	
	private IntBuffer textureQuad;
	private IntBuffer textureQuadScreen;
	private float time;
	private float lastTime;
	
	private boolean mousePointKnown = false;
	private boolean lastMousePointKnown = false;
	private PVector mouseFluid = new PVector(0, 0);
	private PVector lastMouseFluid = new PVector(0, 0);
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) 
	{	
		pApp = applet;
		String vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();
		String fragShader = getClass().getResource("/aviss/shaders/gpufluid/advect.frag.glsl").getPath();
		advectShader = pApp.loadShader(fragShader, vertShader);
		advectShader.set("rdx", 1f/32f);
		advectShader.set("velocity", 1);
		advectShader.set("target", 2);
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/no-transform.vert.glsl").getPath();
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/quad-texture.frag.glsl").getPath();
		screenTextureShader = pApp.loadShader(fragShader, vertShader);
		screenTextureShader.set("texture", 1);
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();				
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/mouseForce.frag.glsl").getPath();
		mouseForceShader = pApp.loadShader(fragShader, vertShader);
		mouseForceShader.set("dx", 32f);
		mouseForceShader.set("velocity", 1);
		mouseForceShader.set("mouse", mouseFluid);
		mouseForceShader.set("lastMouse", lastMouseFluid);
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();						
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/velocity-divergence.frag.glsl").getPath();
		divergenceShader = pApp.loadShader(fragShader, vertShader);
		divergenceShader.set("halfrdx", 0.5f * (1f/32f));
		divergenceShader.set("velocity", 1);
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();								
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/pressure-solve.frag.glsl").getPath();
		pressureSolveShader = pApp.loadShader(fragShader, vertShader);
		pressureSolveShader.set("pressure", 1);
		pressureSolveShader.set("divergence", 2);
		pressureSolveShader.set("alpha", -1f*(1024f));
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();										
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/pressure-gradient-subtract.frag.glsl").getPath();
		pressureGradientSubtractShader = pApp.loadShader(fragShader, vertShader);		
		pressureGradientSubtractShader.set("pressure", 1);
		pressureGradientSubtractShader.set("velocity", 2);
		pressureGradientSubtractShader.set("halfrdx", 0.5f * (1f/32f));		
		vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();		
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/mouseDye.frag.glsl").getPath();
		updateDyeShader = pApp.loadShader(fragShader, vertShader);
		updateDyeShader.set("mouse", mouseFluid);
		updateDyeShader.set("lastMouse", lastMouseFluid);
		updateDyeShader.set("dye", 1);
		updateDyeShader.set("dx", 32f);
		
		updateCoreShaderUniforms(advectShader);
		updateCoreShaderUniforms(mouseForceShader);
		updateCoreShaderUniforms(divergenceShader);
		updateCoreShaderUniforms(pressureSolveShader);
		updateCoreShaderUniforms(pressureGradientSubtractShader);
		updateCoreShaderUniforms(updateDyeShader);
		
		PJOGL pgl = PManager.getPGL();		
		pgl.disable(PGL.DEPTH_TEST);
		pgl.disable(PGL.CULL_FACE);
		pgl.disable(PGL.DITHER);
		
		offScreenTarget = new RenderTarget(pApp.width, pApp.height, new TextureFactory());
		textureQuad = GeometryTools.getCachedUnitQuad(PGL.TRIANGLE_STRIP);		
		textureQuadScreen = GeometryTools.createQuad(0, 0, 1, 1, PGL.TRIANGLE_STRIP);
		TextureFactory textureFactory = new TextureFactory(null, PGL.FLOAT, null, null, null, null); 				
		velocityRenderTarget = new RenderTarget2Phase(applet.width, applet.height, textureFactory);
		pressureRenderTarget = new RenderTarget2Phase(applet.width, applet.width, textureFactory);		
		divergenceRenderTarget = new RenderTarget(applet.width, applet.height, textureFactory);
		dyeRenderTarget = new RenderTarget2Phase(applet.width, applet.height, textureFactory);//new TextureFactory(PGL.RGB, PGL.FLOAT, PGL.LINEAR, null, null, null));

		PManager.endPGL();
	}

	@Override
	public void reInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// MOUSE AND TIME**********
		mouseFluid = clipToAspectSpace(windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY)));//windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		mousePointKnown = true;
	
		time = pApp.millis();
		float dt = 0.016f;//time - lastTime;
		lastTime = time;
		
		if(lastMousePointKnown && mousePointKnown){
			mouseForceShader.set("isMouseDown", true);
			mouseForceShader.set("mouse", mouseFluid);
			mouseForceShader.set("lastMouse", lastMouseFluid);
			updateDyeShader.set("isMouseDown", true);
			updateDyeShader.set("mouse", mouseFluid);
			updateDyeShader.set("lastMouse", lastMouseFluid);
		}
		//***********************
		
		
		PJOGL pgl = PManager.getPGL();
		pgl.disable(PGL.DEPTH_TEST);
		pgl.disable(PGL.CULL_FACE);
		pgl.disable(PGL.DITHER);
		pgl.viewport(0, 0, pApp.width, pApp.height);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));
		PManager.endPGL();
		
//		advect(velocityRenderTarget, dt);	
//		applyForces(dt);
//		computeDivergence();
//		solvePressure();
//		subtractPressureGradient();
		updateDye(dt);
//		advect(dyeRenderTarget, dt);
		
		pgl = PManager.getPGL();
		pgl.viewport (0, 0, pApp.width, pApp.height);
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);
//		pgl.viewport (0, 0, offScreenTarget.width, offScreenTarget.height);
//		pgl.bindFramebuffer(PGL.FRAMEBUFFER, offScreenTarget.fbo.get(0));
		
		pgl.clearColor(0,0,0,1);
		pgl.clear(PGL.COLOR_BUFFER_BIT);
		
		pgl.enable(PGL.BLEND);
		pgl.blendFunc(PGL.SRC_ALPHA, PGL.SRC_ALPHA);
		pgl.blendEquation(PGL.FUNC_ADD);
		
		renderTexture(dyeRenderTarget.readFromTexture);
		pgl.disable(PGL.BLEND);	
		
//		pgl.viewport (0, 0, pApp.width, pApp.height);
//		pgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);
//		renderTexture(offScreenTarget.texture);
		lastMouseFluid = mouseFluid;
		lastMousePointKnown = mousePointKnown;	
		

		PManager.endPGL();
		pgl = null;
	}

	public void advect(RenderTarget2Phase target, float dt)
	{
		PJOGL pgl = PManager.getPGL();
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 

		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, target.readFromTexture.get(0)); 
		
		advectShader.set("dt", dt);	
		advectShader.set("velocity", 1);
		advectShader.set("target", 2);
		
		renderShaderTo(advectShader, target);
		PManager.endPGL();
		target.swap();
	}
	
	private void applyForces(float dt)
	{
		if(mouseForceShader == null)
			return;
		
		PJOGL pgl = PManager.getPGL();
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 
		mouseForceShader.set("velocity", 1);
		mouseForceShader.set("dt", dt);
		
		renderShaderTo(mouseForceShader, velocityRenderTarget);
		PManager.endPGL();
		velocityRenderTarget.swap();
	}
	
	private void computeDivergence()
	{
		PJOGL pgl = PManager.getPGL();
		
		// load velocity texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 
		
		divergenceShader.set("velocity", 1);	

		renderShaderTo(divergenceShader, divergenceRenderTarget);
		PManager.endPGL();
	}
	
	private void solvePressure()
	{
		PJOGL pgl = PManager.getPGL();
		pressureSolveShader.bind();
		
		for(int i = 0; i < 8; i++)
		{
			// loading pressure texture (texture1)					
			pgl.enable(PGL.TEXTURE_2D);
			pgl.activeTexture(PGL.TEXTURE1);
			pgl.bindTexture(PGL.TEXTURE_2D, pressureRenderTarget.readFromTexture.get(0)); 
			pressureSolveShader.set("pressure", 1);
			pgl.activeTexture(PGL.TEXTURE1);
			pgl.bindTexture(PGL.TEXTURE_2D, divergenceRenderTarget.texture.get(0)); 
			pressureSolveShader.set("divergence", 2);	
//			renderShaderTo(pressureSolveShader, pressureRenderTarget);

			pressureRenderTarget.activate();
			pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);

			pressureRenderTarget.swap();
		}
		pressureSolveShader.unbind();
		PManager.endPGL();
	}

	private void subtractPressureGradient()
	{
		PJOGL pgl = PManager.getPGL();
		
		// load pressure texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, pressureRenderTarget.readFromTexture.get(0)); 
		pressureGradientSubtractShader.set("pressure", 1);
		
		// load velocity texture
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 
		pressureGradientSubtractShader.set("velocity", 2);
		
		renderShaderTo(pressureGradientSubtractShader, velocityRenderTarget);
		PManager.endPGL();
		velocityRenderTarget.swap();
	}
	
	private void updateDye(float dt)
	{
		if(updateDyeShader == null)
			return;
		
		PJOGL pgl = PManager.getPGL();
		
		// loading dye texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, dyeRenderTarget.readFromTexture.get(0));
		updateDyeShader.set("dye", 1);
		updateDyeShader.set("dt", dt);

		renderShaderTo(updateDyeShader, dyeRenderTarget);
		PManager.endPGL();
		dyeRenderTarget.swap();
	}
	
	private void renderShaderTo(PShader shader, ITargetable target)
	{			
		PJOGL pgl = PManager.getPGL();
		shader.bind();
		target.activate();
		pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
		shader.unbind();		
	}
	
	private void renderTexture(IntBuffer texture)
	{
		PJOGL pgl = PManager.getPGL();
	
		// activate
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuadScreen.get(0));
		
		// texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, texture.get(0)); 
		screenTextureShader.set("texture", 1);	
		
		// draw
		screenTextureShader.bind();
		pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
		screenTextureShader.unbind();
	}
	
	private void updateCoreShaderUniforms(PShader shader){
		if(shader == null)
			return;
		
		PVector inv = new PVector(1f/(float)pApp.width, 1f/(float)pApp.height);
		shader.set("aspectRatio", (float)pApp.width / (float)pApp.height);
		shader.set("invresolution", inv);
	}
	
	public PVector clipToAspectSpace(PVector clip)
	{
		return new PVector(clip.x * ((float)pApp.width / (float)pApp.height), clip.y);
	}
	
	@Override
	public void keyPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void oscEvent(OscMessage m) {
		// TODO Auto-generated method stub
		
	}

	
	private PVector windowToClipSpace(PVector v)
	{	
		return new PVector((v.x/pApp.width)*2 - 1, ((pApp.height-v.y)/pApp.height)*2 - 1);	
	}	
}
