package aviss.visuals;

import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import aviss.data.FFTPacket;
import aviss.data.KinectHand;
import aviss.data.Star;
import aviss.data.Star.StarState;
import aviss.data.Utils;
import KinectPV2.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GL4;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLES3;

import netP5.NetAddress;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import oscP5.OscMessage;
import oscP5.OscP5;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureIO;

import processing.core.*;
import processing.opengl.*;

public class KinectStarCloudGenerator implements AVGenerator{

	private AVISSApplet pApp;
	private AudioManager aMan;
	private KinectPV2 kinect;
	private OscP5 osc;
	private NetAddress address;
	private Skeleton[] kinectSkeletonData;
	private LinkedHashMap<Integer, KinectHand> hands;	
	private ArrayList<Star> stars;

	private float a = 0;
	private int zval = 50;
	private float pointCloudScale = 260f;
	private float skeletonScale = 539f;
	private float skeletonTransX, skeletonTransY, pointCloudTransX, pointCloudTransY;
	private float audioDampening = 5f;
	
	//Distance Threshold
	private float maxD = 20f; //meters
	private float minD = -5f;
	
	private int[] klangs = new int[]{700, 400, 1000, 500};
	private float[][] glass = new float[][]{new float[]{0.2f, 3.0f, 5.0f, 7.0f}, 
											new float[]{0.8f, 5.0f, 10.0f, 20.0f}, 
											new float[]{0.8f, 5.0f, 7.0f, 10.0f}};
	private Timer oscEventTimer;
	private boolean oscEventReset;
	
	private int numStars = 1000;
	private boolean drawSkeleton = false;
	private boolean ending = false;
	private boolean forceShow = false;
	private float fadeToWhite = 0;
	private float supernovaCoeff = 100;
	private StarState starState = StarState.No_Move;


	PShader starShader;
	PShader starTextureShader;
	PImage starTexture;
	PImage audioIn;
	
	PGL pgl;
	IntBuffer vBuffID;
	IntBuffer cBuffID;
	IntBuffer sBuffID;
	IntBuffer texID;
	int texLoc;
	
	public FloatBuffer kinectPointBuffer;
	public FloatBuffer colourBuffer;
	public FloatBuffer sizeBuffer;
	
	
	private final NetAddress kAddress = new NetAddress("128.189.254.229", 57120);
	
	public KinectStarCloudGenerator(OscP5 o, NetAddress a)
	{
		stars = new ArrayList<Star>();
		osc = o;
		address = a;
	}
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) 
	{
		pApp = applet;
		aMan = am;

		osc = new OscP5(this,12000);	
		oscEventTimer = new Timer();
		
		skeletonTransX = pointCloudTransX = (pApp.width/2) + 80;
		skeletonTransY = pointCloudTransY = (pApp.height/2) + 20;
		
		audioIn = pApp.loadImage(getClass().getResource("/aviss/resources/starTexture.png").getPath());
		starTexture = pApp.loadImage(getClass().getResource("/aviss/resources/green.png").getPath());
		starTexture.loadPixels();
		starShader = pApp.loadShader(getClass().getResource("/aviss/shaders/pointDraw.frag.glsl").getPath(), getClass().getResource("/aviss/shaders/pointDraw.vert.glsl").getPath());
		
		kinect = new KinectPV2(pApp);
		kinect.enableSkeleton(true);
	
		kinect.enableSkeleton3dMap(true);
		kinect.enablePointCloud(true);
		kinect.activateRawDepth(true);
		
		kinect.setLowThresholdPC(minD);
		kinect.setHighThresholdPC(maxD);
		kinect.init();

		hands = new LinkedHashMap<Integer, KinectHand>();
		numStars = kinectPointCloudInit(am);
		initVBO(numStars);
	}

	/*
	 *  Waits for Kinect to give realistic point cloud data
	 *  (there's usually a 2-3 second wait)
	 */
	private int kinectPointCloudInit(AudioManager am)
	{
		ArrayList<Float> validPts = new ArrayList<Float>();
		int validPointCount = Integer.MAX_VALUE;
		while(validPointCount >= 2000000)
		{
			validPointCount = 0;
			validPts.clear();
			FloatBuffer pcb = kinect.getPointCloudDepthPos();	
			for(int buffIdx = 0; buffIdx < pcb.capacity(); buffIdx+=3)
			{
				  float x = pcb.get(buffIdx);
				  float y = pcb.get(buffIdx + 1);
				  float z = pcb.get(buffIdx + 2);
				  
				  if(x != Float.NEGATIVE_INFINITY && 
					 y != Float.NEGATIVE_INFINITY && 
					 z != Float.NEGATIVE_INFINITY)
				  {					  
					  validPointCount++;			
					  validPts.add(x);
					  validPts.add(y);
					  validPts.add(z);
				  }
			}
		}
		
		float[] pts = new float[validPointCount*3];
		float[] colours = new float[validPointCount*4];
		float[] sizes = new float[validPointCount];
		for(int i=0; i<validPointCount; i++)
		{
			int ptIdx = i*3;
			pts[ptIdx] = validPts.get(ptIdx);
			pts[ptIdx+1] = validPts.get(ptIdx+1);
			pts[ptIdx+2] = validPts.get(ptIdx+2);
			
		  	float rZ = zval + pApp.random(-100,0);			
			float rX = pApp.random(0, pApp.width);
			float rY = pApp.random(0, pApp.height);			
			
			int fftBandIdx = i % am.getFFTAvgSpecSize();
			Star newStar = new Star(pApp, this, am.getFFTPackets()[fftBandIdx], 10, i);
			newStar.location = new PVector(rX, rY, rZ);
			newStar.homeLocation = newStar.location;
			newStar.pointCloudLocation = new PVector(pts[i], pts[i+1], pts[i+2]);
			
			sizes[i] = newStar.mass;
			
			int cIdx = i*4;
			colours[cIdx] = newStar.colour.x;
			colours[cIdx+1] = newStar.colour.y;
			colours[cIdx+2] = newStar.colour.z;
			colours[cIdx+3] = newStar.luminosity;
			
			stars.add(newStar);				
		}
		kinectPointBuffer = Utils.allocateDirectFloatBuffer(pts.length);
		kinectPointBuffer.put(pts);
		kinectPointBuffer.rewind();
		
		colourBuffer = Utils.allocateDirectFloatBuffer(colours.length);
		colourBuffer.put(colours);
		colourBuffer.rewind();
		
		sizeBuffer = Utils.allocateDirectFloatBuffer(sizes.length);
		sizeBuffer.put(sizes);
		sizeBuffer.rewind();
		
		return validPointCount;
	}
	
	private void initVBO(int nvert)
	{		
		pgl = pApp.beginPGL();
		
		vBuffID = Utils.allocateDirectIntBuffer(1);
		pgl.genBuffers(1, vBuffID);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, vBuffID.get(0));
		pgl.bufferData(PGL.ARRAY_BUFFER, nvert * 3 * Utils.SIZEOF_FLOAT, kinectPointBuffer, PGL.STATIC_DRAW);
		
		cBuffID = Utils.allocateDirectIntBuffer(1);
		pgl.genBuffers(1, cBuffID);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, cBuffID.get(0));
		pgl.bufferData(PGL.ARRAY_BUFFER, nvert * 4 * Utils.SIZEOF_FLOAT, colourBuffer, PGL.STATIC_DRAW);
		
		sBuffID = Utils.allocateDirectIntBuffer(1);
		pgl.genBuffers(1, sBuffID);
		pgl.bindBuffer(PGL.ARRAY_BUFFER, sBuffID.get(0));
		pgl.bufferData(PGL.ARRAY_BUFFER, nvert * Utils.SIZEOF_FLOAT, sizeBuffer, PGL.STATIC_DRAW);
		
//		texID = IntBuffer.allocate(1);
//		pgl.genTextures(1, texID);
//		pgl.bindTexture(PGL.TEXTURE_2D, 1);
//		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR);
//		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);
//		pgl.texImage2D(PGL.TEXTURE_2D, 0, PGL.RGBA, starTexture.width, starTexture.height, 0, PGL.RGBA, PGL.UNSIGNED_BYTE, IntBuffer.wrap(starTexture.pixels).rewind());
//		pgl.bindTexture(PGL.TEXTURE_2D, 0);
		pApp.endPGL();			

	}

	@Override
	public void reInit() {
		
	}

	@Override
	public void run() 
	{		
		if(ending){
			pApp.background(fadeToWhite);
			if(fadeToWhite<255)
				fadeToWhite+=1f;
		}
		if(starState.equals(StarState.Point_Cloud) || starState.equals(StarState.Supernova))
			fetchPointCloudData();			
		else
		{			
			fetchHandData();
			//System.out.println(stars.get(1).fftObj.getFFTAverageOverN(10));
			for(Star s: stars)
				s.update(hands, starState, audioDampening, forceShow);
		}
		renderGLPoints();
	}
	
	private void fetchHandData()
	{
		kinectSkeletonData = kinect.getSkeleton3d();
		
		for(int i = 0; i < kinectSkeletonData.length; i++) 
		{
			if(kinectSkeletonData[i].isTracked())
			{
				Integer skeletonID = kinectSkeletonData[i].hashCode();
				KinectHand handPair = hands.get(skeletonID);
				if( handPair == null){
					handPair = new KinectHand();
				}
				
				KJoint h = kinectSkeletonData[i].getJoints()[KinectPV2.JointType_HandRight];
				PVector hLoc = new PVector(h.getX(), h.getY(), h.getZ());
				hLoc.set(hLoc.x * skeletonScale*-1, hLoc.y * skeletonScale * -1, hLoc.z);
				//hLoc.mult(skeletonScale);
				//hLoc.rotate(PApplet.PI);
				hLoc.add(skeletonTransX, skeletonTransY, 0);				
				handPair.rightHandLocation = hLoc;
				handPair.rightHandState = h.getState();
				
				if(h.getState() == KinectPV2.HandState_Open)
				{
//					if(handPair.rightHandOSCReset)
//					{	
//				//		System.out.println("Right OSC Triggered");
//						if(starState.equals(StarState.Spring_React))
//						{
//							OscMessage clinkyTrig = new OscMessage("/startNote");
//							clinkyTrig.add(klangs[new Random().nextInt(klangs.length)]);
//							osc.send(clinkyTrig, kAddress);						
//						}
//						else if(starState.equals(StarState.Gravity))
//						{
//							OscMessage glassTrig = new OscMessage("/startNote2");
//							glassTrig.add(glass[new Random().nextInt(glass.length)]);
//							osc.send(glassTrig, kAddress);						
//						}			
//						handPair.rightHandOSCReset = false;
//						oscEventTimer.schedule(new TimerTask(){
//							  @Override
//							  public void run() {
//								  hands.get(skeletonID).rightHandOSCReset = true;
//							  }
//						}, 500);
//					}
				}				
				if(drawSkeleton)
					drawHandState(hLoc, h.getState());
				
				h = kinectSkeletonData[i].getJoints()[KinectPV2.JointType_HandLeft];
				hLoc = new PVector(h.getX(), h.getY(), h.getZ());	
				//hLoc.mult(skeletonScale);
				//hLoc.rotate(PApplet.PI);
				hLoc.set(hLoc.x * skeletonScale*-1, hLoc.y * skeletonScale * -1, hLoc.z);	
				hLoc.add(skeletonTransX, skeletonTransY, 0);
				handPair.leftHandLocation = hLoc;
				handPair.leftHandState = h.getState();
				
				if(handPair.leftHandState == KinectPV2.HandState_Open)
				{
//					if(handPair.leftHandOSCReset)
//					{
//						System.out.println("Left OSC Triggered");
//						if(starState.equals(StarState.Spring_React))
//						{
//							OscMessage clinkyTrig = new OscMessage("/startNote");
//							clinkyTrig.add(klangs[new Random().nextInt(klangs.length)]);
//							osc.send(clinkyTrig, kAddress);
//						}
//						else if(starState.equals(StarState.Gravity))
//						{
//							OscMessage glassTrig = new OscMessage("/startNote2");
//							glassTrig.add(glass[new Random().nextInt(glass.length)]);
//							osc.send(glassTrig, kAddress);						
//						}
//						handPair.leftHandOSCReset = false;
//						oscEventTimer.schedule(new TimerTask(){
//							  @Override
//							  public void run() {
//								  hands.get(skeletonID).leftHandOSCReset = true;
//							  }
//						}, 500);
//					}
				}
				if(drawSkeleton)
					drawHandState(hLoc, h.getState());
				hands.put(skeletonID, handPair);
			}
		
		}
	}
	
	private void fetchPointCloudData()
	{
		  kinect.setLowThresholdPC(minD);
		  kinect.setHighThresholdPC(maxD);
	
		  FloatBuffer pcb = kinect.getPointCloudDepthPos();	
		  
		  int validPointCount = 0;
		  for(int i = 0; i < pcb.capacity()/3; i++)
		  {			
			  int buffIdx = i*3;
			  float x = pcb.get(buffIdx);
			  float y = pcb.get(buffIdx + 1);
			  float z = pcb.get(buffIdx + 2);
			  
			  if( x != Float.NEGATIVE_INFINITY && 
				  y != Float.NEGATIVE_INFINITY && 
				  z != Float.NEGATIVE_INFINITY)
			  {
				  Star s = stars.get(validPointCount);				 
				  if(s!=null && validPointCount<numStars)
				  {
					  buffIdx = (validPointCount*3);
					  s.pointCloudLocation.set(x, y, z);
					  s.update(hands, starState, audioDampening, forceShow);	  
				  }
				  validPointCount++;
			  }
		  }
		  
		  pApp.translate(pointCloudTransX, pointCloudTransY, zval);
		  pApp.scale(pointCloudScale, -1*pointCloudScale, pointCloudScale);
		  pApp.rotate(a, 0.0f, 1.0f, 0.0f);		
		  
		  if(starState.equals(StarState.Supernova)){
			  if(supernovaCoeff>1.1f)
				  supernovaCoeff-=0.4f;
		  }
	}
	
	private void renderGLPoints()
	{	
		  pgl = (PJOGL)pApp.beginPGL();		  
		  starShader.bind();

		  int vertexLocation = pgl.getAttribLocation(starShader.glProgram, "vertex");
		  pgl.enableVertexAttribArray(vertexLocation);
		  pgl.bindBuffer(PGL.ARRAY_BUFFER, vBuffID.get(0));
		  pgl.bufferData(PGL.ARRAY_BUFFER,  kinectPointBuffer.capacity() * Utils.SIZEOF_FLOAT, kinectPointBuffer, PGL.STATIC_DRAW);
		  pgl.vertexAttribPointer(vertexLocation, 3, PGL.FLOAT, false, 0, 0);
		  
		  int colourLocation = pgl.getAttribLocation(starShader.glProgram, "color");
		  pgl.enableVertexAttribArray(colourLocation);
		  pgl.bindBuffer(PGL.ARRAY_BUFFER, cBuffID.get(0));
		  pgl.bufferData(PGL.ARRAY_BUFFER, colourBuffer.capacity() * Utils.SIZEOF_FLOAT, colourBuffer, PGL.STATIC_DRAW);
		  pgl.vertexAttribPointer(colourLocation, 4, PGL.FLOAT, false, 0, 0);
		  
		  int sizeLocation = pgl.getAttribLocation(starShader.glProgram, "size");
		  pgl.enableVertexAttribArray(sizeLocation);
		  pgl.bindBuffer(PGL.ARRAY_BUFFER, sBuffID.get(0));
		  pgl.bufferData(PGL.ARRAY_BUFFER, sizeBuffer.capacity() * Utils.SIZEOF_FLOAT, sizeBuffer, PGL.STATIC_DRAW);
		  pgl.vertexAttribPointer(sizeLocation, 1, PGL.FLOAT, false, 0, 0);
		  
		  pgl.enable(PGL.POINT_SMOOTH);
		  pgl.disable(PGL.DEPTH_TEST);			 
		  pgl.enable(PGL.BLEND);
		  pgl.blendFunc(PGL.SRC_ALPHA, PGL.ONE);
		  
		  // Draw
		  if(starState.equals(StarState.Supernova)){
			  pgl.drawArrays(PGL.LINES, 0, (int)(kinectPointBuffer.capacity()/supernovaCoeff));
		  }
		  else
			  pgl.drawArrays(PGL.POINTS, 0, kinectPointBuffer.capacity());
		 
		  // Unbind
		  starShader.unbind();
		  
		  pgl.disable(PGL.BLEND);		  
		  pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);	
		  pgl.disableVertexAttribArray(vertexLocation);
		  pgl.disableVertexAttribArray(colourLocation);
		  
		  pApp.endPGL();			  

//		  FloatBuffer sizes = FloatBuffer.wrap(new float[2]);
//		  gl2.glGetFloatv(GL2.GL_ALIASED_POINT_SIZE_RANGE, sizes);
//		  float quadratic[] =  { 1.0f, 0.0f, 0.01f };
//		  gl2.glPointParameterfv( GL2.GL_POINT_DISTANCE_ATTENUATION, quadratic , 1);
//		  gl2.glPointParameterf( GL2.GL_POINT_FADE_THRESHOLD_SIZE, 60.0f);
		    
//		  gl2.glPointParameterf(GL2.GL_POINT_SIZE_MIN, sizes.get(0));
//		  gl2.glPointParameterf(GL2.GL_POINT_SIZE_MAX, sizes.get(1));
//		  gl2.glTexEnvf(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL2.GL_TRUE);

	}

	@Override
	public void keyPressed() 
	{
		if (pApp.key == '-') {
			if(starState.equals(StarState.Point_Cloud) || starState.equals(StarState.Supernova)){
				pointCloudScale -= 5;
				System.out.println("Scale Value: " + pointCloudScale);
			}
			else{
				skeletonScale-=1;
				System.out.println("SkeletonScale: "+ skeletonScale);
			}
				
		} else if (pApp.key == '=') {
			if(starState.equals(StarState.Point_Cloud)|| starState.equals(StarState.Supernova)){				
				pointCloudScale += 5;
				System.out.println("Scale Value: " + pointCloudScale);
			}
			else{
				skeletonScale+=1;
				System.out.println("SkeletonScale: "+ skeletonScale);
			}
		} else if (pApp.key == ',') {
			audioDampening -= 0.1;
			System.out.println("Audio Dampening: " + audioDampening);
		} else if (pApp.key == '.') {
			audioDampening += 0.1;
			System.out.println("Audio Dampening: " + audioDampening);
		} else if (pApp.key == 'u') {
			minD += 0.1;
			System.out.println("Min Depth: " + minD);
		} else if (pApp.key == 'j') {
			minD -= 0.1;
			System.out.println("Min Depth: " + minD);
		} else if (pApp.key == 'i') {
			maxD += 0.1;
			System.out.println("Max Depth: " + maxD);
		} else if (pApp.key == 'k') {
			maxD -= 0.1;
			System.out.println("Max Depth: " + maxD);
		}		
		else if(pApp.key == 'f')
			forceShow = !forceShow;
		else if (pApp.key == 's')
			drawSkeleton = !drawSkeleton;
		else if (pApp.key == '1')
			starState = StarState.No_Move;
		else if (pApp.key == '2')
			starState = StarState.Spring_React;	
		else if (pApp.key == '3')
			starState = StarState.Gravity;	
		else if (pApp.key == '4')
			starState = StarState.Point_Cloud;
		else if (pApp.key == '6')
		{
			starState = StarState.Supernova;
			supernovaCoeff = 100;
		}

		else if (pApp.key == ' ')
			ending = true;

		if (pApp.key == PApplet.CODED) 
		{
			if(starState.equals(StarState.Point_Cloud) || starState.equals(StarState.Supernova)){
			    if (pApp.keyCode == PApplet.UP){
			    	pointCloudTransY+=1;
			    	System.out.println("Point Cloud Translation Y: "+ pointCloudTransY);
			    }
			    else if (pApp.keyCode == PApplet.RIGHT){
			    	pointCloudTransX+=1;
			    	System.out.println("Point Cloud Translation X: "+ pointCloudTransX);
			    }
			    else if (pApp.keyCode == PApplet.DOWN){
			    	pointCloudTransY-=1;
			    	System.out.println("Point Cloud Translation Y: "+ pointCloudTransY);
			    }
			    else if (pApp.keyCode == PApplet.LEFT){
			    	pointCloudTransX-=1;
			    	System.out.println("Point Cloud Translation X: "+ pointCloudTransX);
			    }
		    }
			else
			{
				if (pApp.keyCode == PApplet.UP){
			    	skeletonTransY+=1;
			    	System.out.println("Skeleton Translation Y: "+ skeletonTransY);
			    }
			    else if (pApp.keyCode == PApplet.RIGHT){
			    	skeletonTransX+=1;
			    	System.out.println("Skeleton Translation X: "+ skeletonTransX);
			    }
			    else if (pApp.keyCode == PApplet.DOWN){
			    	skeletonTransY-=1;
			    	System.out.println("Skeleton Translation Y: "+ skeletonTransY);
			    }
			    else if (pApp.keyCode == PApplet.LEFT){
			    	skeletonTransX-=1;
			    	System.out.println("Skeleton Translation X: "+ skeletonTransX);
			    }				
			}
		}
	}	
	
	//use different color for each skeleton tracked
	private int getIndexColor(int index) {
	  int col = pApp.color(255);
	  if (index == 0)
	    col = pApp.color(255, 0, 0);
	  if (index == 1)
	    col = pApp.color(0, 255, 0);
	  if (index == 2)
	    col = pApp.color(0, 0, 255);
	  if (index == 3)
	    col = pApp.color(255, 255, 0);
	  if (index == 4)
	    col =pApp. color(0, 255, 255);
	  if (index == 5)
	    col = pApp.color(255, 0, 255);

	  return col;
	}
	
	private void drawBody(KJoint[] joints) {
		  drawBone(joints, KinectPV2.JointType_Head, KinectPV2.JointType_Neck);
		  drawBone(joints, KinectPV2.JointType_Neck, KinectPV2.JointType_SpineShoulder);
		  drawBone(joints, KinectPV2.JointType_SpineShoulder, KinectPV2.JointType_SpineMid);

		  drawBone(joints, KinectPV2.JointType_SpineMid, KinectPV2.JointType_SpineBase);
		  drawBone(joints, KinectPV2.JointType_SpineShoulder, KinectPV2.JointType_ShoulderRight);
		  drawBone(joints, KinectPV2.JointType_SpineShoulder, KinectPV2.JointType_ShoulderLeft);
		  drawBone(joints, KinectPV2.JointType_SpineBase, KinectPV2.JointType_HipRight);
		  drawBone(joints, KinectPV2.JointType_SpineBase, KinectPV2.JointType_HipLeft);

		  // Right Arm    
		  drawBone(joints, KinectPV2.JointType_ShoulderRight, KinectPV2.JointType_ElbowRight);
		  drawBone(joints, KinectPV2.JointType_ElbowRight, KinectPV2.JointType_WristRight);
		  drawBone(joints, KinectPV2.JointType_WristRight, KinectPV2.JointType_HandRight);
		  drawBone(joints, KinectPV2.JointType_HandRight, KinectPV2.JointType_HandTipRight);
		  drawBone(joints, KinectPV2.JointType_WristRight, KinectPV2.JointType_ThumbRight);

		  // Left Arm
		  drawBone(joints, KinectPV2.JointType_ShoulderLeft, KinectPV2.JointType_ElbowLeft);
		  drawBone(joints, KinectPV2.JointType_ElbowLeft, KinectPV2.JointType_WristLeft);
		  drawBone(joints, KinectPV2.JointType_WristLeft, KinectPV2.JointType_HandLeft);
		  drawBone(joints, KinectPV2.JointType_HandLeft, KinectPV2.JointType_HandTipLeft);
		  drawBone(joints, KinectPV2.JointType_WristLeft, KinectPV2.JointType_ThumbLeft);

		  // Right Leg
		  drawBone(joints, KinectPV2.JointType_HipRight, KinectPV2.JointType_KneeRight);
		  drawBone(joints, KinectPV2.JointType_KneeRight, KinectPV2.JointType_AnkleRight);
		  drawBone(joints, KinectPV2.JointType_AnkleRight, KinectPV2.JointType_FootRight);

		  // Left Leg
		  drawBone(joints, KinectPV2.JointType_HipLeft, KinectPV2.JointType_KneeLeft);
		  drawBone(joints, KinectPV2.JointType_KneeLeft, KinectPV2.JointType_AnkleLeft);
		  drawBone(joints, KinectPV2.JointType_AnkleLeft, KinectPV2.JointType_FootLeft);

		  drawJoint(joints, KinectPV2.JointType_HandTipLeft);
		  drawJoint(joints, KinectPV2.JointType_HandTipRight);
		  drawJoint(joints, KinectPV2.JointType_FootLeft);
		  drawJoint(joints, KinectPV2.JointType_FootRight);

		  drawJoint(joints, KinectPV2.JointType_ThumbLeft);
		  drawJoint(joints, KinectPV2.JointType_ThumbRight);

		  drawJoint(joints, KinectPV2.JointType_Head);
		}

		private void drawJoint(KJoint[] joints, int jointType) {
		  pApp.strokeWeight(2.0f + joints[jointType].getZ()*8);
		  pApp.point(joints[jointType].getX(), joints[jointType].getY(), joints[jointType].getZ());
		}

		private void drawBone(KJoint[] joints, int jointType1, int jointType2) {
			pApp.strokeWeight(2.0f + joints[jointType1].getZ()*8);
			pApp.point(joints[jointType2].getX(), joints[jointType2].getY(), joints[jointType2].getZ());
		}

		private void drawHandState(PVector handLoc, int handState) {
		  handState(handState);
		  pApp.pushMatrix();
		  pApp.translate(handLoc.x, handLoc.y, handLoc.z);
		  pApp.ellipse(0, 0, 60, 60);
		  pApp.popMatrix();
		}

		private void handState(int handState) {
		  switch(handState) {
		  case KinectPV2.HandState_Open:
			  pApp.fill(0, 255, 0);
		    break;
		  case KinectPV2.HandState_Closed:
			  pApp.fill(255, 0, 0);
		    break;
		  case KinectPV2.HandState_Lasso:
			  pApp.fill(0, 0, 255);
		    break;
		  case KinectPV2.HandState_NotTracked:
			  pApp.fill(100, 100, 100);
		    break;
		  }
		}

	@Override
	public void oscEvent(OscMessage m) {
		// TODO Auto-generated method stub
		
	}
	
}

