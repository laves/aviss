package gltoolbox;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.opengl.PGL;
import aviss.applet.PManager;

public class PGeometryTools {

	static HashMap<Integer, PShape> unitQuadCache = new HashMap<Integer, PShape>();
	static public PShape getCachedUnitQuad(int drawMode){
		PShape unitQuad = unitQuadCache.get(drawMode);
		if(unitQuad == null)
		{
            unitQuad = createUnitQuad(drawMode);
            unitQuadCache.put(drawMode, unitQuad);
		}
		return unitQuad;
		
	}
	

    static HashMap<Integer, PShape> clipSpaceQuadCache = new HashMap<Integer, PShape>();
    static public PShape getCachedClipSpaceQuad(int drawMode){
    	PShape clipSpaceQuad = clipSpaceQuadCache.get(drawMode);
        if(clipSpaceQuad == null){
            clipSpaceQuad = createClipSpaceQuad(drawMode);
            clipSpaceQuadCache.put(drawMode, clipSpaceQuad);
        }
        return clipSpaceQuad;
    }

    static public PShape createUnitQuad(int drawMode){
        return createQuad(0, 0, 1, 1, drawMode);
    }

    static public PShape createClipSpaceQuad(int drawMode){
        return createQuad(-1, -1, 2, 2, drawMode);
    }

    static public PShape createQuad(float originX, float originY, float width, float height, int drawMode)
    {
    	PShape sh = PManager.getApplet().createShape();
    	PGraphics im = PManager.getApplet().createGraphics((int)width, (int)height, PApplet.OPENGL);
    	sh.beginShape(drawMode);
    	sh.texture(im);
//    	float[] vertices;
        if(drawMode == PApplet.TRIANGLES)
        {
        	sh.vertex(originX, originY+height); 
        	sh.vertex(originX, originY);    									
        	sh.vertex(originX+width, originY+height);
        	sh.vertex(originX+width, originY);
        	sh.vertex(originX+width, originY+height); 	//  *---4
        	sh.vertex(originX, originY); 				//  |  /|
        												//  | / |
                    									//  5---*                   
        }
        else if (drawMode == PGL.TRIANGLE_FAN)
        {
        	sh.vertex(originX, originY+height);
        	sh.vertex(originX, originY);
        	sh.vertex(originX+width,  originY);
        	sh.vertex(originX+width,  originY+height);        			
        }
        else { // PGL TRIANGLE_STRIP
        	sh.vertex(originX, originY+height); 
        	sh.vertex(originX, originY);    									
        	sh.vertex(originX+width,  originY+height);
        	sh.vertex(originX+width,  originY);
	    							
		//  0---2
		//  |  /|
	    //  | / |
	    //  1---3
	            //TRIANGLE_STRIP builds triangles with the pattern, v0, v1, v2 | v2, v1, v3
	            //by default, anti-clockwise triangles are front-facing         	 
        }
        sh.endShape();
//	    FloatBuffer quad = FloatBuffer.allocate(vertices.length);    
//		quad.put(vertices);
//		quad.rewind();
//		
//		PGL pgl = PManager.getPGL();
//		pgl.genBuffers(1, sh);
//		pgl.bindBuffer(PGL.ARRAY_BUFFER, sh.get(0));
//		pgl.bufferData(PGL.ARRAY_BUFFER, vertices.length, quad, PGL.STATIC_DRAW);
//		pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
//		
        return sh;
    }
}
