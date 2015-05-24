package aviss.data;

import java.util.Random;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import processing.core.*;
import processing.opengl.PShader;

public class Nebula {

	private AVISSApplet pApp;
	
	public  CircularFifoQueue<Float> fftData;
	private CircularFifoQueue<Float> colourQ;
	  
	public PShader blender;
	
	public PImage image;
	private PImage nextImage;
	public Nebula blendObject;
	
	public PVector location;
	public PVector velocity;
	public PVector acceleration;
	
	public PVector colour;
	public PVector colourVel;
	
	public float alpha;
	public float alphaVel;
	public float audioMax = 0;
	public boolean isCrossfadingStage1 = false;
	public boolean isCrossfadingStage2 = false;
	
	private String rootPath = getClass().getResource("/aviss/resources/nebs/").getPath();
	private int border = 20;
	
	public Nebula(AVISSApplet applet, Nebula prev, PShader blen){
		pApp = applet;
		
		
		image = pApp.loadImage(rootPath + new Random().nextInt(43) + ".jpg");
		blendObject = prev;
		
		genLocAndVel();
		acceleration = new PVector(0, 0, 0);	 
	    
	    colour = new PVector(1, 1, 1);
	    colourVel = new PVector(0, 0, 0);	
	    alpha = 0.5f;
	    alphaVel = 0;	    
	    
	    blender = blen;
	    
	    fftData = new CircularFifoQueue<Float>(4);
		fftData.add(1f);
		fftData.add(1f);
		fftData.add(1f);
		fftData.add(1f);
		
		colourQ = new CircularFifoQueue<Float>(20);
	}
	
	public void run(){
		update();
		
		if(isCrossfadingStage1 || isCrossfadingStage2)
		{
		    alpha += alphaVel;
			crossfade();
		}
		else
		{
		    alpha = fftData.get(3);
		    alpha = AVISSApplet.constrain(alpha, 0.1f, 0.85f);
			runShader();
		}
	}
	
	public void run(float a){
		update();
		alpha = a;
		runShader();
	}
	
	public void update(){
	    velocity.add(acceleration);
	    location.add(velocity);
	    colour.add(colourVel);

	}
	
	public void addFFTValue(float pwr)
	{
		colourQ.add(pwr);
		float avg = 0;
		for(float f: colourQ){
			avg+=f;
		}
		avg/=colourQ.size();
		fftData.add(avg);
	}
	
	private void crossfade(){
		
		if(isCrossfadingStage1) {
			if(alpha>0)
			{
				runShader();
			}
			else{

				if(nextImage == null)
				{
					alphaVel = 0;
				}
				else
				{
					System.out.println("stage1 complete " + pApp.millis() + " alpha: " + alpha);
					isCrossfadingStage1 = false;			
					isCrossfadingStage2 = true;
					alphaVel = 0.005f;
					
					image = nextImage;
					genLocAndVel();
				}
			}
		}
		else if(isCrossfadingStage2){
			if(alpha >= 0.3)
			{
				isCrossfadingStage2 = false;	
				System.out.println("stage2 complete " + pApp.millis() + " alpha: " + alpha);
			}
			else
			{
				runShader();
			}
		}
	}
	
	private void runShader(){
		
		blender.set("alpha", alpha);
		
		float f1 = fftData.get(0);
		f1 = AVISSApplet.constrain(f1, 0.1f, 1.2f);
		float f2 = fftData.get(1);
		f2 = AVISSApplet.constrain(f2, 0.1f, 1.2f);
		float f3 = fftData.get(2);
		f3 = AVISSApplet.constrain(f3, 0.1f, 1.2f);
		
		blender.set("audioCoeff", new PVector(f1, f2, f3));
		
		blender.set("destSampler", image);
		blender.set("srcSampler", blendObject.image);
		
		blender.set("srcSize", blendObject.image.width, blendObject.image.height);  
		int[] rect = calculateRect(blendObject);		
		blender.set("srcRect", rect[0], rect[1], rect[2], rect[3]);
		
		blender.set("destSize", image.width, image.height);		 		
		rect = calculateRect(this);		
		blender.set("destRect", rect[0], rect[1], rect[2], rect[3]);
		
		draw(image, (float)rect[0], (float)rect[1], (float)rect[2], (float)rect[3]);
	}
	
	private int[] calculateRect(Nebula n){
		
		int rectx1 = (int)n.location.x * -1;
		int recty1 = (int)n.location.y * -1;
		int rectx2 = rectx1 + pApp.width;
		int recty2 = recty1 + pApp.height;
		
		boolean loadNewImage = false;
		
		// bound checking
		if(rectx1 < border)
		{
			if(rectx1 < 0){
				rectx1 = 0;
				rectx2 = rectx1 + pApp.width;
				n.location.x = 0;
				n.velocity.x = 0;
			}
			n.velocity.mult(0.9f);
			loadNewImage = true;
		}
		if(recty1 < border)
		{
			if(recty1 < 0){
				recty1 = 0;
				recty2 = recty1 + pApp.height;
				n.location.y = 0;
				n.velocity.y = 0;
			}
			n.velocity.mult(0.9f);
			loadNewImage = true;
		}
		if(rectx2 >= n.image.width-border)
		{
			if(rectx2 >= n.image.width)
			{	
				int diff = rectx2 - n.image.width;
				rectx1 -= diff;
				n.location.x += diff;
				n.velocity.x = 0;
			}
			n.velocity.mult(0.9f);
			loadNewImage = true;
		}
		if(recty2 >= n.image.height-border)
		{
			if(recty2 >= n.image.height)
			{	
				int diff = recty2 - n.image.height;
				recty1 -= diff;
				n.location.y += diff;
				n.velocity.y = 0;
			}
			n.velocity.mult(0.9f);
			loadNewImage = true;
		}
		
		if(loadNewImage && !isCrossfadingStage1 && !isCrossfadingStage2 )
		{
			System.out.println("startCross " + pApp.millis() + "alpha: " + alpha);
			alphaVel = -0.005f;
			alpha = fftData.get(3);
			isCrossfadingStage1 = true;		
			nextImage = null;
			new Thread(new Runnable(){
				@Override
				public void run() {
					nextImage = pApp.loadImage(rootPath + new Random().nextInt(43) + ".jpg");
				}}).start(); 
		}
		
		int[] rect = {rectx1, recty1, pApp.width, pApp.height};
		return rect;
	}

	
	private void genLocAndVel()
	{
		float xLim = pApp.width - image.width;
		float yLim = pApp.height - image.height;
		location = new PVector(pApp.random(xLim + border+1, -border-1), pApp.random(yLim + border+1, -border-1), 0);
		velocity = new PVector(0, 0, 0);
		if(location.x < xLim/2)
			velocity.x = pApp.random(0.2f, 1f);
		else
			velocity.x = pApp.random(0.2f, 1f) * -1;
		
		if(location.y < yLim/2)
			velocity.y = pApp.random(0.2f, 1f);
		else
			velocity.y = pApp.random(0.2f, 1f) * -1;
	}
	
	private void draw(PImage im, float x1, float y1, float x2, float y2)
	{
		pApp.shader(blender);
		
		pApp.pushMatrix();
		pApp.noStroke();
		
		pApp.beginShape(PApplet.QUAD);
		pApp.vertex(0, 0, 0, 0);
		pApp.vertex(pApp.width, 0, 1, 0);
		pApp.vertex(pApp.width, pApp.height, 1, 1);
		pApp.vertex(0, pApp.height, 0, 1);
		pApp.endShape();
		
		pApp.popMatrix();  
	}
}
