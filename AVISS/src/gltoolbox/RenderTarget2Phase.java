package gltoolbox;

import java.nio.IntBuffer;

import aviss.applet.PManager;
import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;

public class RenderTarget2Phase implements ITargetable{

	public int width;
	public int height;
	public IntBuffer writeFBO;
	public IntBuffer writeToTexture;
	public IntBuffer readFBO;
	public IntBuffer readFromTexture;
	
	private PShader resampleShader;
	private TextureFactory textureFactory;
	private static IntBuffer textureQuad;
	
	public RenderTarget2Phase (int width, int height, TextureFactory textureFactory)
	{
		if(textureFactory == null)
			textureFactory = new TextureFactory();
		this.width = width;
		this.height = height;
		this.textureFactory = textureFactory;		
		resampleShader = PManager.getApplet().loadShader(
							getClass().getResource("/aviss/shaders/resample.frag.glsl").getPath(), 
							getClass().getResource("/aviss/shaders/resample.vert.glsl").getPath()
						 );
		resampleShader.set("texture", 1);
		
		if(textureQuad == null)
			textureQuad = GeometryTools.getCachedUnitQuad(PGL.TRIANGLE_STRIP);
		
		resize(width, height);
	}

	@Override
	public void activate() {
		PJOGL pgl = PManager.getPGL();
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, writeFBO.get(0));
	}

	public void swap(){
		IntBuffer tmpFBO = writeFBO;
		writeFBO = readFBO;
		readFBO = tmpFBO;

		IntBuffer tmpTex = writeToTexture;
		writeToTexture = readFromTexture;
		readFromTexture = tmpTex;
	}
	
	@Override
	public void clear(Integer mask) {
		clearRead(mask);
		clearWrite(mask);
	}

	public void clearRead(Integer mask){
		if(mask == null)
			mask = PGL.COLOR_BUFFER_BIT;
		PJOGL pgl = PManager.getPGL();
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, readFBO.get(0));
		pgl.clearColor (0, 0, 0, 1);
		pgl.clear (mask);
	}

	public void clearWrite(Integer mask)
	{
		if(mask == null)
			mask = PGL.COLOR_BUFFER_BIT;
		PJOGL pgl = PManager.getPGL();
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, writeFBO.get(0));
		pgl.clearColor (0, 0, 0, 1);
		pgl.clear (mask);
	}

	public void dispose()
	{
		PJOGL pgl = PManager.getPGL();
		pgl.deleteFramebuffers(1, writeFBO);
		pgl.deleteFramebuffers(1, readFBO);
		pgl.deleteTextures(1, writeToTexture);
		pgl.deleteTextures(1, readFromTexture);
	}
	
	@Override
	public ITargetable resize(int width, int height) {
		IntBuffer newWriteToTexture  = textureFactory.createTexture(width, height);
		IntBuffer newReadFromTexture = textureFactory.createTexture(width, height);

		PJOGL pgl = PManager.getPGL();
		//attach texture to frame buffer object's color component
		this.writeFBO = IntBuffer.allocate(1);
		pgl.genFramebuffers(1, writeFBO);
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, writeFBO.get(0));
		pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, PGL.TEXTURE_2D, newWriteToTexture.get(0), 0);

		//attach texture to frame buffer object's color component
		this.readFBO = IntBuffer.allocate(1);
		pgl.genFramebuffers(1, readFBO);
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, readFBO.get(0));
		pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, PGL.TEXTURE_2D, newReadFromTexture.get(0), 0);
		
		if(this.readFromTexture != null)
		{		
			pgl = PManager.getPGL();		
			pgl.bindFramebuffer(PGL.FRAMEBUFFER, readFBO.get(0));
			pgl.viewport(0, 0, width, height);

			pgl.activeTexture(PGL.TEXTURE1);
			pgl.enable(PGL.TEXTURE_2D);  
			pgl.bindTexture(PGL.ARRAY_BUFFER, textureQuad.get(0));
			
			resampleShader.set("texture", this.readFromTexture.get(0));
			resampleShader.bind();
			pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
			resampleShader.unbind();

			pgl.deleteTextures(1 , readFromTexture);
			pgl.bindTexture(PGL.TEXTURE_2D, 0);  
		}
		else 
			clearRead(null);

		if(this.writeToTexture != null){	
			pgl.deleteTextures(1, writeToTexture);	
		}
		else clearWrite(null);
		
		
		this.width = width;
		this.height = height;
		this.writeToTexture = newWriteToTexture;
		this.readFromTexture = newReadFromTexture;
		  
		return this;
	}
	
}
