package gltoolbox;

import java.nio.IntBuffer;

import processing.opengl.PGL;
import processing.opengl.PJOGL;
import processing.opengl.PShader;
import aviss.applet.PManager;

public class RenderTarget implements ITargetable{

	public int width;
	public int height;
	public IntBuffer fbo;
	public IntBuffer texture;
	
	private PShader resampleShader;
	private TextureFactory textureFactory;
	private static IntBuffer textureQuad;
	
	public RenderTarget (int width, int height, TextureFactory textureFactory)
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
		
		if(textureQuad == null)
			textureQuad = GeometryTools.getCachedUnitQuad(PGL.TRIANGLE_STRIP);		
		
		resize(width, height);
	}

	@Override
	public void activate() {
		PManager.getPGL().bindFramebuffer(PGL.FRAMEBUFFER, fbo.get(0));
	}
	
	@Override
	public void clear(Integer mask) {
		if(mask == null)
			mask = PGL.COLOR_BUFFER_BIT;
		PJOGL pgl = PManager.getPGL();
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, fbo.get(0));
		pgl.clearColor (0, 0, 0, 1);
		pgl.clear (mask);
	}

	public void dispose()
	{
		PJOGL pgl = PManager.getPGL();
		pgl.deleteFramebuffers(1, fbo);
		pgl.deleteTextures(1, texture);
	}
	
	@Override
	public ITargetable resize(int width, int height) 
	{
		IntBuffer newTexture  = textureFactory.createTexture(width, height);

		fbo = IntBuffer.allocate(1);
		PJOGL pgl = PManager.getPGL();		
		//attach texture to frame buffer object's color component
		pgl.genFramebuffers(1, fbo);
		pgl.bindFramebuffer(PGL.FRAMEBUFFER, fbo.get(0));
		pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0, PGL.TEXTURE_2D, newTexture.get(0), 0);
		
		if(texture!= null)
		{	
			pgl = PManager.getPGL();	
			pgl.bindFramebuffer(PGL.FRAMEBUFFER, fbo.get(0));
			pgl.viewport(0, 0, width, height);

			pgl.bindBuffer(PGL.ARRAY_BUFFER, textureQuad.get(0));

//			resampleShader.set("texture", texture.get(0));
			resampleShader.bind();
			pgl.drawArrays(PGL.TRIANGLE_STRIP, 0, 4);
			resampleShader.unbind();

			pgl.deleteTextures(1 , texture);			
		}
		else 
			clear(null);
		
		this.width = width;
		this.height = height;
		
		texture = newTexture;
		
		return this;
	}
}
