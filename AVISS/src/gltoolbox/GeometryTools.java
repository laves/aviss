package gltoolbox;

import java.nio.*;
import java.util.HashMap;

import aviss.applet.PManager;
import aviss.data.Utils;
import processing.core.*;
import processing.opengl.*;

public class GeometryTools {
	
	static HashMap<Integer, IntBuffer> unitQuadCache = new HashMap<Integer, IntBuffer>();
	static public IntBuffer getCachedUnitQuad(int drawMode){
		IntBuffer unitQuad = unitQuadCache.get(drawMode);
		if(unitQuad == null)
		{
            unitQuad = createUnitQuad(drawMode);
            unitQuadCache.put(drawMode, unitQuad);
		}
		return unitQuad;
		
	}
	

    static HashMap<Integer, IntBuffer> clipSpaceQuadCache = new HashMap<Integer, IntBuffer>();
    static public IntBuffer getCachedClipSpaceQuad(int drawMode){
    	IntBuffer clipSpaceQuad = clipSpaceQuadCache.get(drawMode);
        if(clipSpaceQuad == null){
            clipSpaceQuad = createClipSpaceQuad(drawMode);
            clipSpaceQuadCache.put(drawMode, clipSpaceQuad);
        }
        return clipSpaceQuad;
    }

    static public IntBuffer createUnitQuad(int drawMode){
        return createQuad(0, 0, 1, 1, drawMode);
    }

    static public IntBuffer createClipSpaceQuad(int drawMode){
        return createQuad(-1, -1, 2, 2, drawMode);
    }

    static public IntBuffer createQuad(float originX, float originY, float width, float height, int drawMode)
    {
    	IntBuffer sh = IntBuffer.allocate(1);
    	
    	float[] vertices;
        if(drawMode == PGL.TRIANGLES)
        {
        	vertices = new float[]{ originX, originY+height, 
									originX, originY,    									
									originX+width,  originY+height,
									originX+width,  originY,
									originX+width,  originY+height, //  *---4
									originX,        originY 		//  |  /|
        					        };								//  | / |
                    												//  5---*                   
        }
        else if (drawMode == PGL.TRIANGLE_FAN)
        {
        	vertices = new float[]{	originX, originY+height,
        							originX, originY,
        							originX+width,  originY,
        							originX+width,  originY+height,        			
        							};
        }
        else { // PGL TRIANGLE_STRIP
			vertices = new float[]{ originX, originY+height, 
	    							originX, originY,    									
	    							originX+width,  originY+height,
	    							originX+width,  originY 
	    							};
		//  0---2
		//  |  /|
	    //  | / |
	    //  1---3
	            //TRIANGLE_STRIP builds triangles with the pattern, v0, v1, v2 | v2, v1, v3
	            //by default, anti-clockwise triangles are front-facing         	 
        }
        
	    FloatBuffer quad = FloatBuffer.allocate(vertices.length);    
		quad.put(vertices);
		quad.rewind();
		
		PGL pgl = PManager.getPGL();
		pgl.genBuffers(1, sh);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, sh.get(0));
		pgl.bufferData(PGL.ARRAY_BUFFER, vertices.length, quad, PGL.STATIC_DRAW);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
		PManager.endPGL();
		pgl = null;
		
        return sh;
    }
}

    /*  
    *   OpenGL line drawing
    *   +--X--+-----+-----+
    *   |  '  |     |     |
    *   O---->---->---->--X
    *   |  '  |     |     |
    *   +--^--+-----+-----+
    *   |  '  |     |     |
    *   |  ^  |     |     |
    *   |  '  |     |     |
    *   +--O--+-----+-----+
//    */
//    static public function boundaryLinesArray(width:Int, height:Int)return new Float32Array(//OGL centers lines on the boundary between pixels
//        [
//            //left
//            0.5       , 0,
//            0.5       , height,
//            //top
//            0         , height-0.5,
//            width     , height-0.5,
//            //right
//            width-0.5 , height,
//            width-0.5 , 0,
//            //bottom
//            width     , 0.5,
//            0         , 0.5
//        ]
//    );
