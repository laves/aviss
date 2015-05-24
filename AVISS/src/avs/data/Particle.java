package avs.data;

import java.util.Iterator;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import processing.core.PApplet;
import processing.core.PVector;

public class Particle {
	// A simple Particle class

	  public PVector location;
	  public PVector startingLocation;
	  public PVector velocity;
	  public PVector acceleration;
	  public FFTPacket fftObj;
	  
	  private CircularFifoQueue<Float> sizeQ;
	  private CircularFifoQueue<Float> colourQ;
	  
	  private PApplet pApp;
	  public float lifespan;
	  public boolean isDead;
	  
	  public Particle(PVector l, PApplet pApp) {
	    location = l.get();
	    startingLocation = l.get();
	    this.pApp = pApp;
	    lifespan = 255.0f;
	    velocity = new PVector(0,0);
	    acceleration = new PVector(0,0);
	    sizeQ = new CircularFifoQueue<Float>(10);
	    colourQ = new CircularFifoQueue<Float>(10);
	  }
	  
	  public Particle(PApplet pApp, FFTPacket a) {
		    location = new PVector(0,0);
		    this.pApp = pApp;
		    fftObj = a;
		    isDead = true;
		    velocity = new PVector(0,0);
		    acceleration = new PVector(0,0);
		    sizeQ = new CircularFifoQueue<Float>(10);
		    colourQ = new CircularFifoQueue<Float>(10);
		  }
	  
	  public Particle(PVector l, PApplet pApp, float span, PVector v, PVector a) {
		    location = l.get();
		    startingLocation = l.get();
		    this.pApp = pApp;
		    lifespan = span*10;
		    velocity = v;
		    acceleration = a;
		    sizeQ = new CircularFifoQueue<Float>(10);
		    colourQ = new CircularFifoQueue<Float>(10);
		  }

	  public void run() {		
		update();
		display();
	  }

	  
	  public void revive(){
		  float fftAvg = fftObj.getLatestFFTValue();
		  location.set(pApp.random(pApp.width),pApp.random(pApp.height), 0);
		  velocity.set(Math.round(pApp.random(-1, 1)) * fftAvg, Math.round(pApp.random(-1, 1)) * fftAvg);
		  lifespan = 100 + (fftAvg * 3);
		  acceleration.set(0f, 0.05f);
		  isDead = false;
	  }

	  public void update() {
	    velocity.add(acceleration);
	    location.add(velocity);
	    if(location.x > pApp.width || location.x < 0)
	    	velocity.x *= -0.95;
	    else if(location.y > pApp.height || location.y < 0)
	    	velocity.y *= -0.95;
	    
	    lifespan -= 1.0;
	    if(lifespan <0)
	    	isDead = true; 
	  }

	  void display() {

		float fftAvg = fftObj.getFFTAverageOverN(19);
		
	    float colour = PApplet.constrain(fftAvg * 10, 0 ,255);
	    float size = fftAvg;
	    sizeQ.add(size);
	    colourQ.add(colour);
	    Iterator<Float> itr = sizeQ.iterator();
	    float avgSum = 0;
	    while(itr.hasNext())
	    	avgSum += itr.next();
	    size = avgSum / sizeQ.size();
	    
	    itr = colourQ.iterator();
	    avgSum = 0;
	    while(itr.hasNext())
	    	avgSum += itr.next();
	    colour = avgSum / colourQ.size();
	    
	    
	   // float grn = (127 - (PApplet.abs(colour-128)))*2;
	    				
	    
	    pApp.stroke(255-colour, 0, colour, lifespan);
	    pApp.fill(255-colour, 0, colour, lifespan);
	    
	    //pApp.fill(255, 255, 255, lifespan);
	    pApp.ellipse(location.x, location.y, size*2, size*2);
	    
	    //pApp.ellipse(location.x, location.y, location.z, 10, 10);
	    //pApp.sphere(8);
	  }
}
