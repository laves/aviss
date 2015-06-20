package gltoolbox;

import java.nio.IntBuffer;

import aviss.applet.PManager;
import processing.opengl.PGL;
import processing.opengl.PJOGL;

public class TextureFactory
{
	private int channelType = PGL.RGBA;
	private int dataType = PGL.UNSIGNED_BYTE;
	private int filter = PGL.NEAREST;
	private int wrapS = PGL.CLAMP_TO_EDGE;
	private int wrapT = PGL.CLAMP_TO_EDGE;
	private int unpackAlignment = 4;
	
	public TextureFactory()
	{
	}
	
	public TextureFactory(Integer channelType, Integer dataType, Integer filter, Integer wrapS, Integer wrapT, Integer unpackAlignment)
	{
		if(channelType != null) this.channelType = channelType;
		if(dataType != null) this.dataType = dataType;
		if(filter != null) this.filter = filter;
		if(wrapS != null) this.wrapS = wrapS;
		if(wrapT != null) this.wrapT = wrapT;
		if(unpackAlignment != null) this.unpackAlignment = unpackAlignment;
	}
	
	public IntBuffer createTexture(int width, int height){
		IntBuffer texture = IntBuffer.allocate(1);
		
		PJOGL pgl = PManager.getPGL();
		pgl.genTextures(1, texture);
		pgl.bindTexture(PGL.TEXTURE_2D, texture.get(0));

		//set params
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MIN_FILTER, filter); 
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MAG_FILTER, filter); 
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_WRAP_S, wrapS);
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_WRAP_T, wrapT);

		pgl.pixelStorei(PGL.UNPACK_ALIGNMENT, unpackAlignment); //see (see http://www.khronos.org/opengles/sdk/docs/man/xhtml/glPixelStorei.xml)

		//set data
		pgl.texImage2D(PGL.TEXTURE_2D, 0, channelType, width, height, 0, channelType, dataType, null);

		//unbind
		pgl.bindTexture(PGL.TEXTURE_2D, 0);
		
		return texture;
	}
};
