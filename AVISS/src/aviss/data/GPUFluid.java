package aviss.data;

import java.nio.IntBuffer;

import gltoolbox.*;

import javax.media.opengl.*;

import aviss.applet.PManager;
import processing.core.*;
import processing.opengl.*;

public class GPUFluid {

	public int width;
	public int height;
	
	private float cellSize;
	public int solverIterations= 18;
	
	public float aspectRatio;
	
	// Geometry
	public IntBuffer textureQuad;
	
	//Render Targets
	public RenderTarget2Phase velocityRenderTarget;
	private RenderTarget2Phase pressureRenderTarget;
	private RenderTarget divergenceRenderTarget;
	public RenderTarget2Phase dyeRenderTarget;
	
	//User Shaders
	private PShader applyForcesShader;
	private PShader updateDyeShader;

	//Internal Shaders
	private PShader advectShader;
	private PShader divergenceShader;
	private PShader pressureSolveShader;
	private PShader pressureGradientSubtractShader;

	private PApplet pApp;
	
	public GPUFluid(int width, int height, float cellSize, int solverIterations)
	{		
		this.aspectRatio = (float)width / (float)height;
		textureQuad = GeometryTools.getCachedUnitQuad(PGL.TRIANGLE_STRIP);
		
		this.width = width;
		this.height = height;
		this.solverIterations = solverIterations;
		this.pApp = PManager.getApplet();
		
		// loading shaders
		String vertShader = getClass().getResource("/aviss/shaders/gpufluid/texel-space.vert.glsl").getPath();
		String fragShader = getClass().getResource("/aviss/shaders/gpufluid/advect.frag.glsl").getPath();
		advectShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/velocity-divergence.frag.glsl").getPath();
		divergenceShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/pressure-solve.frag.glsl").getPath();
		pressureSolveShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/pressure-gradient-subtract.frag.glsl").getPath();
		pressureGradientSubtractShader = pApp.loadShader(fragShader, vertShader);
		
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/apply-forces.frag.glsl").getPath();
		setApplyForcesShader(pApp.loadShader(fragShader, vertShader));
		fragShader = getClass().getResource("/aviss/shaders/gpufluid/update-dye.frag.glsl").getPath();
		setUpdateDyeShader(pApp.loadShader(fragShader, vertShader));
		
		TextureFactory textureFactory = new TextureFactory(null, PGL.FLOAT, null, null, null, null); 		
		velocityRenderTarget = new RenderTarget2Phase(width, height, textureFactory);
		pressureRenderTarget = new RenderTarget2Phase(width, height, textureFactory);
		divergenceRenderTarget = new RenderTarget(width, height, textureFactory);
		
		// may not support PGL.Linear???
		dyeRenderTarget = new RenderTarget2Phase(width, height, new TextureFactory(PGL.RGB, PGL.FLOAT, PGL.LINEAR, null, null, null));
		
		//texel-space parameters
		updateCoreShaderUniforms(advectShader);
		advectShader.set("velocity", 1);
		advectShader.set("target", 2);
		updateCoreShaderUniforms(divergenceShader);
		divergenceShader.set("velocity", 1);
		updateCoreShaderUniforms(pressureSolveShader);
		pressureSolveShader.set("pressure", 1);
		pressureSolveShader.set("divergence", 2);
		updateCoreShaderUniforms(pressureGradientSubtractShader);
		pressureGradientSubtractShader.set("pressure", 1);
		pressureGradientSubtractShader.set("velocity", 2);
		
		setCellSize(cellSize);
		
//		textureQuad = pApp.createShape();
//		textureQuad.beginShape(PApplet.TRIANGLE_STRIP);
//		textureQuad.vertex(0, pApp.height, 0, 1);
//		textureQuad.vertex(0, 0, 0, 0);
//		textureQuad.vertex(pApp.width, pApp.height, 1, 1);
//		textureQuad.vertex(pApp.width, 0, 1, 0);
//		textureQuad.endShape();
	}
	
	public void step(float dt)
	{
		PJOGL pgl = PManager.getPGL();
		pgl.viewport(0, 0, width, height);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));
		
//		System.out.println("advect shader drawing velocityRenderTarget...");
		advect(velocityRenderTarget, dt);
		
//		System.out.println("applyForces shader drawing velocityRenderTarget...");
		applyForces(dt);
		
//		System.out.println("computeDivergence");
		computeDivergence();
		
//		System.out.println("solvePressure");
		solvePressure();
		
//		System.out.println("subtractPressureGradient");
		subtractPressureGradient();
		
//		System.out.println("updateDye");
		updateDye(dt);

//		System.out.println("advect shader drawing dyeRenderTarget...");
		advect(dyeRenderTarget, dt);
		
	}
	
	public void resize(int width, int height)
	{		
		velocityRenderTarget.resize(width, height);
		pressureRenderTarget.resize(width, height);
		divergenceRenderTarget.resize(width, height);
		dyeRenderTarget.resize(width, height);
		
		this.width = width;
		this.height = height;

	}
	
	public void clear(){
		velocityRenderTarget.clear(null);
		pressureRenderTarget.clear(null);
		dyeRenderTarget.clear(null);
	}
	
	public PVector simToClipSpace(PVector sim)
	{
		return new PVector(sim.x/(this.cellSize*this.aspectRatio), sim.y/this.cellSize);
	}
	
	public void advect(RenderTarget2Phase target, float dt)
	{
		PJOGL pgl = PManager.getPGL();
		
		// load velocity texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 
		
		// load target texture
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, target.readFromTexture.get(0)); 
		advectShader.set("dt", dt);	
	
//		advectShader.set("target", target.readFromTexture.get(0));
//		advectShader.set("velocity", velocityRenderTarget.readFromTexture.get(0));
		
		renderShaderTo(advectShader, target);
		target.swap();
	}
	
	private void applyForces(float dt)
	{
		if(applyForcesShader == null)
			return;
		
		PJOGL pgl = PManager.getPGL();
		
		// load velocity texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 
		applyForcesShader.set("dt", dt);
		
		renderShaderTo(applyForcesShader, velocityRenderTarget);
		velocityRenderTarget.swap();
	}
	
	private void computeDivergence()
	{
		PJOGL pgl = PManager.getPGL();
		
		// load velocity texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 

		renderShaderTo(divergenceShader, divergenceRenderTarget);
	}
	
	private void solvePressure()
	{
		PJOGL pgl = PManager.getPGL();
		
		// loading divergence texture (texture2)
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, divergenceRenderTarget.texture.get(0)); 
//		pressureSolveShader.set("divergence", divergenceRenderTarget.texture.get(0));	
		
		pressureSolveShader.bind();
	
		for(int i = 0; i < solverIterations; i++)
		{
			// loading pressure texture (texture1)
			pgl.activeTexture(PGL.TEXTURE1);
			pgl.bindTexture(PGL.TEXTURE_2D, pressureRenderTarget.readFromTexture.get(0)); 
//			pressureSolveShader.set("pressure", pressureRenderTarget.readFromTexture.get(0));
			
			pressureRenderTarget.activate();
			pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
			pressureRenderTarget.swap();
		}
		pressureSolveShader.unbind();
	}
	
	private void subtractPressureGradient()
	{
		PJOGL pgl = PManager.getPGL();
		
		// load pressure texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE1);
		pgl.bindTexture(PGL.TEXTURE_2D, pressureRenderTarget.readFromTexture.get(0)); 
//		pressureGradientSubtractShader.set("pressure", pressureRenderTarget.readFromTexture.get(0));
		
		// load velocity texture
		pgl.enable(PGL.TEXTURE_2D);
		pgl.activeTexture(PGL.TEXTURE2);
		pgl.bindTexture(PGL.TEXTURE_2D, velocityRenderTarget.readFromTexture.get(0)); 
//		pressureGradientSubtractShader.set("velocity", velocityRenderTarget.readFromTexture.get(0));
		
		renderShaderTo(pressureGradientSubtractShader, velocityRenderTarget);
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
//		updateDyeShader.set("dye", dyeRenderTarget.readFromTexture.get(0));
		updateDyeShader.set("dt", dt);

		renderShaderTo(updateDyeShader, dyeRenderTarget);
		dyeRenderTarget.swap();
	}
	
	private void updateCoreShaderUniforms(PShader shader){
		if(shader == null)
			return;
		
		PVector inv = new PVector(1f/(float)width, 1f/(float)height);
		shader.set("aspectRatio", aspectRatio);
		shader.set("invresolution", inv);
	}
	
	public PShader setApplyForcesShader(PShader v){
		applyForcesShader = v;
		applyForcesShader.set("dx", cellSize);
		updateCoreShaderUniforms(applyForcesShader);
		return applyForcesShader;
	}
	
	public PShader setUpdateDyeShader(PShader v){
		updateDyeShader = v;
		updateDyeShader.set("dx", cellSize);
		updateCoreShaderUniforms(updateDyeShader);
		return updateDyeShader;
	}
	
	public float setCellSize(float v){
		cellSize = v;
		advectShader.set("rdx", 1f/cellSize);
		divergenceShader.set("halfrdx", 0.5f * (1f/cellSize));
		pressureGradientSubtractShader.set("halfrdx", 0.5f * (1f/cellSize));
		pressureSolveShader.set("alpha", -cellSize*cellSize);
		return cellSize;
	}
	
	
	private void renderShaderTo(PShader shader, ITargetable target)
	{			
		PJOGL pgl = PManager.getPGL();
		shader.bind();
		target.activate();
		pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
		shader.unbind();
		
//		target.beginDraw();
//		target.shader(shader);
//		target.noStroke();
//		target.shape(textureQuad);
//		target.endDraw();		
	}

}
