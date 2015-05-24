package aviss.data;

import javax.media.opengl.GL;
import javax.media.opengl.GLBufferStorage;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PJOGL;
import processing.opengl.PShader;

public class GPUFluid {

	public int width;
	public int height;
	
	private float cellSize;
	public int solverIterations= 18;
	
	public float aspectRatio;
	
	// Geometry
	public PGraphics textureQuad;
	
	//Render Targets
	public PGraphics velocityRenderTarget;
	public PGraphics pressureRenderTarget;
	public PGraphics divergenceRenderTarget;
	public PGraphics dyeRenderTarget;
	
	//User Shaders
	private PShader applyForcesShader;
	private PShader updateDyeShader;

	//Internal Shaders
	private PShader advectShader;
	private PShader divergenceShader;
	private PShader pressureSolveShader;
	private PShader pressureGradientSubtractShader;

	private PApplet pApp;
	
	public GPUFluid(int width, int height, float cellSize, int solverIterations, PApplet pApp)
	{
		this.aspectRatio = width / height;
		
		this.width = width;
		this.height = height;
		setCellSize(cellSize);
		this.solverIterations = solverIterations;
		this.pApp = pApp;
		
		// loading shaders
		String vertShader = getClass().getResource("gpufluid/shaders/glsl/fluid/texel-space.vert.glsl").getPath();
		String fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/advect.frag.glsl").getPath();
		advectShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/velocity-divergence.frag.glsl").getPath();
		divergenceShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/pressure-solve.frag.glsl").getPath();
		pressureSolveShader = pApp.loadShader(fragShader, vertShader);
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/pressure-gradient-subtract.frag.glsl").getPath();
		pressureGradientSubtractShader = pApp.loadShader(fragShader, vertShader);
		
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/apply-forces.frag.glsl").getPath();
		setApplyForcesShader(pApp.loadShader(fragShader, vertShader));
		fragShader = getClass().getResource("gpufluid/shaders/glsl/fluid/update-dye.frag.glsl").getPath();
		setUpdateDyeShader(pApp.loadShader(fragShader, vertShader));
		
		velocityRenderTarget = pApp.createGraphics(width, height, PApplet.OPENGL);
		pressureRenderTarget = pApp.createGraphics(width, height, PApplet.OPENGL);
		divergenceRenderTarget = pApp.createGraphics(width, height, PApplet.OPENGL);
		dyeRenderTarget = pApp.createGraphics(width, height, PApplet.OPENGL);
		
		//texel-space parameters
		updateCoreShaderUniforms(advectShader);
		updateCoreShaderUniforms(divergenceShader);
		updateCoreShaderUniforms(pressureSolveShader);
		updateCoreShaderUniforms(pressureGradientSubtractShader);
	}
	
	public void step(float dt){

	  advect(velocityRenderTarget, dt);
		  
		  applyForces(dt);
		  
		  computeDivergence();
		  solvePressure();
		  subtractPressureGradient();
		  
		  updateDye(dt);
		  advect(dyeRenderTarget, dt);
	}
	
	public void resize(int width, int height){
		
		velocityRenderTarget.resize(width, height);
		pressureRenderTarget.resize(width, height);
		divergenceRenderTarget.resize(width, height);
		dyeRenderTarget.resize(width, height);
		
		this.width = width;
		this.height = height;
	}
	
	public void clear(){
		velocityRenderTarget.clear();
		pressureRenderTarget.clear();
		dyeRenderTarget.clear();
	}
	
	public PVector simToClipSpace(PVector sim){
		return new PVector(sim.x/(this.cellSize*this.aspectRatio), sim.y/this.cellSize);
	}
	
	public void advect(PGraphics target, float dt){
		advectShader.set("dt", dt);
		advectShader.set("velocity", velocityRenderTarget);
		advectShader.set("target", target);
		
		renderShaderTo(advectShader, target);
	}
	
	private void applyForces(float dt)
	{
		if(applyForcesShader == null)
			return;
		
		//set uniforms
		applyForcesShader.set("dt", dt);
		applyForcesShader.set("velocity", velocityRenderTarget);
		
		renderShaderTo(applyForcesShader, velocityRenderTarget);
		//velocityRenderTarget.swap()
	}
	
	private void computeDivergence(){
		divergenceShader.set("velocity", velocityRenderTarget);
		renderShaderTo(divergenceShader, divergenceRenderTarget);
	}
	
	private void solvePressure()
	{
		pressureSolveShader.set("divergence", divergenceRenderTarget);	
	
		for(int i = 0; i < solverIterations; i++){
			pressureSolveShader.set("pressure", pressureRenderTarget);
			renderShaderTo(pressureSolveShader, pressureRenderTarget);
		}
	}
	
	private void subtractPressureGradient()
	{
		pressureGradientSubtractShader.set("pressure", pressureRenderTarget);
		pressureGradientSubtractShader.set("velocity", velocityRenderTarget);
		
		renderShaderTo(pressureGradientSubtractShader, velocityRenderTarget);
		//velocityRenderTarget.swap()
	}
	
	private void updateDye(float dt)
	{
		if(updateDyeShader == null)
			return;
		
		//set uniforms
		updateDyeShader.set("dt", dt);
		updateDyeShader.set("dye", dyeRenderTarget);
		
		//render
		renderShaderTo(updateDyeShader, dyeRenderTarget);
		//dyeRenderTarget.swap()
	}
	
	private void updateCoreShaderUniforms(PShader shader){
		if(shader == null)
			return;
		
		//set uniforms
		shader.set("aspectRatio", aspectRatio);
		shader.set("invresolution", new PVector(1/width, 1/height));
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
		advectShader.set("rdx", 1/cellSize);
		divergenceShader.set("halfrdx", 0.5f * (1/cellSize));
		pressureGradientSubtractShader.set("halfrdx", 0.5f * (1/cellSize));
		pressureSolveShader.set("alpha", -cellSize*cellSize);
		return cellSize;
	}
	
	
	private void renderShaderTo(PShader shader, PGraphics target)
	{
		target.shader(shader);
		target.noStroke();
		
		target.beginShape(PApplet.TRIANGLE_STRIP);
		target.vertex(0, pApp.height, 0, 1);
		target.vertex(0, 0, 0, 0);
		target.vertex(pApp.width, pApp.height, 1, 1);
		target.vertex(pApp.width, 0, 1, 0);
		target.endShape();
		
		pApp.image(target, 0, 0);
		//textureQuad = target;
	}

}
