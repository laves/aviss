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
	private RenderTarget2Phase target;

	private  IntBuffer textureQuad;
	private float time;
	private float lastTime;
	
	private boolean mousePointKnown = false;
	private boolean lastMousePointKnown = false;
	private PVector mouseClipSpace = new PVector(0, 0);
	private PVector lastMouseClipSpace = new PVector(0, 0);
	
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
		
		mouseForceShader.set("mouseClipSpace", mouseClipSpace);
		mouseForceShader.set("lastMouseClipSpace", lastMouseClipSpace);
						
		updateCoreShaderUniforms(advectShader);
		updateCoreShaderUniforms(mouseForceShader);
		
		PManager.getPGL();		
		textureQuad = GeometryTools.getCachedUnitQuad(PGL.TRIANGLE_STRIP);		
		TextureFactory textureFactory = new TextureFactory(null, PGL.FLOAT, null, null, null, null); 				
		target = new RenderTarget2Phase(applet.width, applet.height, textureFactory);
		PManager.endPGL();
	}

	@Override
	public void reInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// MOUSE AND TIME**********
		mouseClipSpace = windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		mousePointKnown = true;
	
		time = pApp.millis();
		float dt = time - lastTime;
		lastTime = time;
		
		if(lastMousePointKnown && mousePointKnown){
			mouseForceShader.set("isMouseDown", true);
			mouseForceShader.set("mouseClipSpace", mouseClipSpace);
			mouseForceShader.set("lastMouseClipSpace", lastMouseClipSpace);
		}
		//***********************
		
		
		PJOGL pgl = PManager.getPGL();
		pgl.viewport(0, 0, pApp.width, pApp.height);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));
		advect(target, dt);	
		applyForces(dt);
		
		pgl.viewport (0, 0, pApp.width, pApp.height);
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);
		
		
		pgl.clearColor(0,0,0,1);
		pgl.clear(PGL.COLOR_BUFFER_BIT);
		
		pgl.enable(PGL.BLEND);
		pgl.blendFunc(PGL.SRC_ALPHA, PGL.SRC_ALPHA);
		pgl.blendEquation(PGL.FUNC_ADD);
		
		renderTexture(target.readFromTexture);
		
		lastMouseClipSpace = windowToClipSpace(new PVector(pApp.mouseX, pApp.mouseY));
		lastMousePointKnown = mousePointKnown;		
		
		pgl.disable(PGL.BLEND);	
		PManager.endPGL();
		pgl = null;
	}

	public void advect(RenderTarget2Phase target, float dt)
	{
		PJOGL pgl = PManager.getPGL();
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, target.readFromTexture.get(0)); 
		
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, target.readFromTexture.get(0)); 
		advectShader.set("dt", dt);	
//		advectShader.set("velocity", target.readFromTexture.get(0));
//		advectShader.set("target", target.readFromTexture.get(0));
		
		renderShaderTo(advectShader, target);
		target.swap();
	}
	
	private void applyForces(float dt)
	{
		if(mouseForceShader == null)
			return;
		
		PJOGL pgl = PManager.getPGL();
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, target.readFromTexture.get(0)); 
//		mouseForceShader.set("velocity", target.readFromTexture.get(0));
		mouseForceShader.set("dt", dt);
		
		renderShaderTo(mouseForceShader, target);
		target.swap();
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
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, texture.get(0)); 

//		screenTextureShader.set("texture", texture.get(0));	
		
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
