package aviss.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import aviss.visuals.KinectStarCloudGenerator;
import KinectPV2.*;
import processing.core.PApplet;
import processing.core.PVector;

public class Star {

	  public PVector location;
	  public PVector homeLocation;
	  public PVector pointCloudLocation;
	  public PVector velocity;
	  public PVector acceleration;
	  public PVector force;
	  
	  public PVector colour;
	  public PVector homeColour;
	  public PVector colourVel;
	  public float luminosity;
	  public float homeLum;
	  public float lumVel;
	  public float mass;
	  public int vIdx;
	  public int cIdx;
	  
	  // for spring effect
	  private float springConstant;
	  private float springDampening;
	  private float gravDampening;
	  
	  public FFTPacket fftObj;
	  
	  private PApplet pApp;
	  private KinectStarCloudGenerator starCloud;
	  
	  public enum StarState{
		  No_Move,
		  Osc,
		  Spring_React,
		  Gravity,
		  Point_Cloud,
		  Supernova
	  }
	  
	  public Star(PApplet applet, KinectStarCloudGenerator kscg, FFTPacket f, int historySize, int idx) {
		    pApp = applet;
		    starCloud = kscg;
		    vIdx = idx*3;
		    cIdx = idx*4;
		    fftObj = f;
		    location = new PVector(0, 0, 0);
		    velocity = new PVector(0, 0, 0);
		    acceleration = new PVector(0, 0, 0);
		    force = new PVector(0, 0, 0);
		    colour = new PVector(pApp.random(0.9f), pApp.random(0.9f), pApp.random(0.9f));
		    homeColour = colour;
		    luminosity = pApp.random(1);
		    homeLum = luminosity;
		    
		    springConstant = pApp.random(0.2f, 0.3f);
		    springDampening = pApp.random(0.98f, 0.99f);
		    gravDampening = pApp.random(0.93f, 0.96f);
		    mass = pApp.random(0.1f, 2f);
		    
	  }

	  public void update(LinkedHashMap<Integer, KinectHand> hands, StarState state, float audioDampening, boolean forceShow) 
	  {
		  float fftAvg = fftObj.getFFTAverageOverN(15);		  
		  float colourFactor; 
		  if(forceShow)
			  colourFactor = 1;
		  else
			  colourFactor = fftAvg/audioDampening;

		  if(state.equals(StarState.Supernova))
			    colour = new PVector(pApp.random(1), pApp.random(1), pApp.random(1));
		  else
		  {
			  colour.set(homeColour);
			  colour.mult(colourFactor);
		  }
		  luminosity = homeLum;
		  luminosity *= colourFactor;
		  
		  if(state.equals(StarState.Point_Cloud)|| state.equals(StarState.Supernova))
		  {
			  location.set(pointCloudLocation);
			  
			  starCloud.kinectPointBuffer.put(vIdx, location.x);
			  starCloud.kinectPointBuffer.put(vIdx+1, location.y);
			  starCloud.kinectPointBuffer.put(vIdx+2, location.z);
			  
			  starCloud.colourBuffer.put(cIdx, colour.x);
			  starCloud.colourBuffer.put(cIdx+1, colour.y);
			  starCloud.colourBuffer.put(cIdx+2, colour.z);
			  starCloud.colourBuffer.put(cIdx+3, luminosity);
			  return;
		  }
		  
		  if (state.equals(StarState.Osc))
		  {
			  float oscFactor = (fftAvg/1000f);
			  force.mult(0);
			  location.add(oscFactor * PApplet.sin(pApp.millis()), oscFactor * PApplet.sin(pApp.millis()), oscFactor * PApplet.tan(pApp.millis()));  
		  }
		  else if(state.equals(StarState.Spring_React))
		  {
			  //float oscFactor = (fftAvg/100f);
			  force.mult(0);			  
			  boolean reaching = false;
			  for(KinectHand h: hands.values())
			  { 			 
				  PVector tmpForce;
					if(PVector.dist(h.rightHandLocation, homeLocation) < 30 && (h.rightHandState.equals(KinectPV2.HandState_Open))){
				    	tmpForce = PVector.sub(location, h.rightHandLocation);
				    	tmpForce.mult(-springConstant);
				    	force.add(tmpForce);
				    	reaching = true;
			    	}	    
					if(PVector.dist(h.leftHandLocation, homeLocation) < 30 && (h.leftHandState.equals(KinectPV2.HandState_Open))){
				    	tmpForce = PVector.sub(location, h.leftHandLocation);
				    	tmpForce.mult(-springConstant);
				    	force.add(tmpForce);
				    	reaching = true;
			    	}	    
			  }
			  if(!reaching)
			  {
				  force = PVector.sub(location, homeLocation);
				  force.mult(-springConstant);		    
			  }			  
			  acceleration.set(PVector.div(force, mass));  
			  velocity.add(acceleration);
			  velocity.mult(springDampening);
			//  location.add(oscFactor * PApplet.sin(pApp.millis()), oscFactor * PApplet.sin(pApp.millis()), oscFactor * PApplet.tan(pApp.millis()));  

		  }
		  else if(state.equals(StarState.Gravity))
		  {
			  force.mult(0);			  
			  for(KinectHand h: hands.values())
			  { 			    
			    	force.add(h.attract(this));		    				    
			  }
			  acceleration.set(PVector.div(force, mass));  
			  velocity.add(acceleration);
			  velocity.mult(gravDampening);
		  }
		  else
		  { 
			  velocity.add(acceleration);
		  }
		  location.add(velocity);  

		  
		  if(location.x < 0)
			  location.x = pApp.width;
		  else if(location.x > pApp.width)
			  location.x = 0;
		  
		  if(location.y < 0)
			  location.y = pApp.height;
		  else if(location.y > pApp.height)
			  location.y = 0;
		  
		  if(location.z > 1000)
			  location.z = -1000;
		  else if(location.z < -1000)
			  location.z = 1000;
		  
		  starCloud.kinectPointBuffer.put(vIdx, location.x);
		  starCloud.kinectPointBuffer.put(vIdx+1, location.y);
		  starCloud.kinectPointBuffer.put(vIdx+2, location.z);
		  
		  starCloud.colourBuffer.put(cIdx, colour.x);
		  starCloud.colourBuffer.put(cIdx+1, colour.y);
		  starCloud.colourBuffer.put(cIdx+2, colour.z);
		  starCloud.colourBuffer.put(cIdx+3, luminosity);
	  }
	  
}
