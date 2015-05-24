package gltoolbox;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import aviss.applet.AppletManager;
import processing.core.*;
import processing.opengl.*;

public class GeometryTools {
	
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
        return createQuad(0, 0, 1, 1, drawMode, PGL.STATIC_DRAW);
    }

    static public PShape createClipSpaceQuad(int drawMode){
        return createQuad(-1, -1, 2, 2, drawMode, PGL.STATIC_DRAW);
    }

    static public PShape createQuad(float originX, float originY, float width, float height, int drawMode, int usage)
    {
        PShape sh = AppletManager.getApplet().createShape();
    	sh.beginShape(drawMode);
        if(drawMode == PApplet.TRIANGLE_STRIP)
        {
    	    sh.vertex(originX, originY+height, 0, 1); //anti-clockwise triangle strip
    	    sh.vertex(originX, originY,);    									
    	    sh.vertex(originX+width,  originY+height);
    	    sh.vertex(originX+width,  originY);
    	//  0---2
    	//  |  /|
        //  | / |
        //  1---3
                //TRIANGLE_STRIP builds triangles with the pattern, v0, v1, v2 | v2, v1, v3
                //by default, anti-clockwise triangles are front-facing 
    	    
        }
        else if(drawMode == PApplet.TRIANGLES)
        {
        	sh.vertex(originX,        originY+height);     //  0---2
        	sh.vertex(originX,        originY);            //  |  /|
        	sh.vertex(originX+width,  originY+height);     //  | / |
        	sh.vertex(originX+width,  originY);            //  1---3
        	sh.vertex(originX+width,  originY+height); 	//  *---4
        	sh.vertex(originX,        originY); 		//  |  /|
                    									//  | / |
                    									//  5---*                   
        }
        else if (drawMode == PApplet.TRIANGLE_FAN)
        {
        	sh.vertex(originX,        originY+height);    //  0---3
        	sh.vertex(originX,        originY);           //  |\  |
        	sh.vertex(originX+width,  originY);           //  | \ |
        	sh.vertex(originX+width,  originY+height);    //  1---2
        }
        sh.endShape();
	    
//	    FloatBuffer quad = ByteBuffer.allocateDirect(vertices.length * (Float.SIZE / 8)).order(ByteOrder.nativeOrder()).asFloatBuffer();    
//		quad.put(vertices);
//		quad.rewind();
//		IntBuffer quadID = ByteBuffer.allocateDirect(Integer.SIZE/8).order(ByteOrder.nativeOrder()).asIntBuffer();
//		
//		PGL pgl = AppletManager.startPGL();
//		pgl.genBuffers(1, quadID);
//		pgl.bindBuffer(PGL.ARRAY_BUFFER, quadID.get(0));
//		pgl.bufferData(PGL.ARRAY_BUFFER, vertices.length * 3 * (Float.SIZE/8), quad, usage);
//		pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
//		AppletManager.endPGL();
//		
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
