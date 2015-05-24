package aviss.visuals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import netP5.NetAddress;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.*;
import processing.opengl.PShader;
import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import aviss.data.FFTPacket;
import aviss.data.Nebula;

public class ImageOverlayGenerator implements AVGenerator{

	private Nebula[] nebulae;
	
	private AVISSApplet pApp;	
	private AudioManager aMan;
	
	private OscP5 osc;
	private NetAddress address;
	private ArrayList<NetAddress> phones = new ArrayList<NetAddress>();
	
	private PShader[] blenders;
	private PShader simpleDrawer;
	private PImage space;
	
	private int numActivePaintings = 3;
	private int focalPainting = 0;
	private float audioMax = Float.MIN_VALUE;
	private float brightnessCoeff =100f;
	
	private int timesRun = 0;
	private boolean fade = false;
	private float universalAlpha = 0;
	private final float INTRO_FRAMES = 1500;
	
	public ImageOverlayGenerator(OscP5 o, NetAddress a){
		osc = o;
		address = a;
	}
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) 
	{
		pApp = applet;
		aMan = am;
		
		pApp.frameRate(30);		
		timesRun = 0;
		
		// loading shaders for image blending
		blenders = new PShader[4];
		blenders[0] = pApp.loadShader(getClass().getResource("/aviss/shaders/imageblend/dodge.glsl").getPath());
		blenders[1] = pApp.loadShader(getClass().getResource("/aviss/shaders/imageblend/burn.glsl").getPath());
		blenders[2] = pApp.loadShader(getClass().getResource("/aviss/shaders/imageblend/overlay.glsl").getPath());
		blenders[3] = pApp.loadShader(getClass().getResource("/aviss/shaders/imageblend/difference.glsl").getPath());
			
		// base image
		space = pApp.loadImage(getClass().getResource("/aviss/resources/nebs/space.jpg").getPath());
		simpleDrawer = pApp.loadShader(getClass().getResource("/aviss/shaders/simpleDraw.glsl").getPath());
		
		// init nebulae with image sources
		nebulae = new Nebula[numActivePaintings];
		
		float alphaVal = 230f;
	    float alphaStep = 180/numActivePaintings;
		for(int i=0; i< nebulae.length;i++){
			
			Nebula prev = null;
			if(i!=0)
				prev = nebulae[i-1];
			
			nebulae[i] = new Nebula(pApp, prev, blenders[new Random().nextInt(blenders.length)]);
			nebulae[i].colour = new PVector(pApp.random(0.8f, 1),pApp.random(0.8f, 1),pApp.random(0.8f, 1));
			nebulae[i].alpha = pApp.random(alphaVal-49/255f,alphaVal/255f);
			if(i==0)
				nebulae[i].velocity = new PVector(0,0,0);
			alphaVal-=alphaStep;
		}
	}

	@Override
	public void reInit() {
		
	}

	@Override
	public void run() 
	{
		
		FFTPacket[] fftData = aMan.getSortedFFTPackets(aMan.getFFTHistory());
		
		// drawing base images		
		//simpleDraw(space, new PVector(-200f, -200f), 1f, new PVector(1f,1f,1f));
		
		// shaded and blended overlays	
		for(int i=0; i < numActivePaintings; i++)	
		{
			Nebula n = nebulae[i];
			float pwr = fftData[i].getFFTAverageOverN(aMan.getFFTHistory()-i);
			if(pwr != Float.NaN)
			{
				pwr /= brightnessCoeff;				
				n.fftData.add(pwr);
			}			
			
			if(i>0){
				if(timesRun < INTRO_FRAMES){
					n.run(universalAlpha);
					timesRun++;
					universalAlpha += (0.8f / INTRO_FRAMES);					
				}
				else if(fade)
				{
					n.run(universalAlpha);
					universalAlpha -= 0.001f;					
				}
				else
					n.run();
			}
			else
			{
				n.update();		
				simpleDraw(n.image, PVector.mult(n.location, -1), universalAlpha, new PVector(n.fftData.get(0), n.fftData.get(1), n.fftData.get(2)));						
			}			
		}
	}
	
	@Override
	public void keyPressed() 
	{	
		if (pApp.key == '1' && nebulae.length>0) 
			focalPainting = 1;
		else if(pApp.key == '2' && nebulae.length>1)
			focalPainting = 2;
		else if(pApp.key == '3'&& nebulae.length>2)
			focalPainting = 3;
		else if(pApp.key == '4'&& nebulae.length>3)
			focalPainting = 4;
		else if (pApp.key == '5'&& nebulae.length>4) 
			focalPainting = 5;
		else if(pApp.key == '6'&& nebulae.length>5)
			focalPainting = 6;
		else if(pApp.key == '7'&& nebulae.length>6)
			focalPainting = 7;
		else if(pApp.key == '8'&& nebulae.length>7)
			focalPainting = 8;
		else if(pApp.key == '9'&& nebulae.length>8)
			focalPainting = 9;
		else if(pApp.key == '0'&& nebulae.length>9)
			focalPainting = 10;
		
		if(focalPainting < nebulae.length){
		if (pApp.keyCode == PApplet.RIGHT) 
			 nebulae[focalPainting].velocity.x += 1;		
		else if (pApp.keyCode == PApplet.LEFT) 	 
			nebulae[focalPainting].velocity.x -= 1;
		else if (pApp.keyCode == PApplet.DOWN) 
			nebulae[focalPainting].velocity.y += 1;
		else if (pApp.keyCode == PApplet.UP) 
			nebulae[focalPainting].velocity.y -= 1;
		
		if(pApp.key == 'w')
			 nebulae[focalPainting].colour.x += 1;
		else if(pApp.key == 'a')	 
			nebulae[focalPainting].colour.x -= 1;
		else if(pApp.key == 's')
			nebulae[focalPainting].colour.y += 1;
		else if(pApp.key == 'd')
			nebulae[focalPainting].colour.y -= 1;
		else if(pApp.key == 'e')
			nebulae[focalPainting].colour.z += 1;
		else if(pApp.key == 'q')
			nebulae[focalPainting].colour.z -= 1;
		}
		
		if(pApp.key == 'v')
			brightnessCoeff -= 0.5f;
		else if(pApp.key == 'b')
			brightnessCoeff += 0.5f;
		else if(pApp.key == 'n'){
			fade = true;
		}
	}	
	
	private void checkBounds(Nebula n)
	{
		float nextXStep = n.location.x + n.velocity.x;
		if( nextXStep > 0)
			n.velocity.x=0;
		else if(nextXStep < (pApp.width - n.image.width))
			n.velocity.x=0;
		
		float nextYStep = n.location.y + n.velocity.y;
		if( nextYStep > 0)
			n.velocity.y=0;
		else if(nextYStep < (pApp.height - n.image.height))
			n.velocity.y=0;		
	}
	
	private void simpleDraw(PImage im, PVector loc, float alpha, PVector colourCoeffs)
	{
		simpleDrawer.set("alpha", alpha);
		simpleDrawer.set("colourCoeffs", colourCoeffs);
		simpleDrawer.set("texture", im);
		simpleDrawer.set("destSize", im.width, im.height);		 		
		simpleDrawer.set("destRect", (int)loc.x, (int)loc.y, pApp.width, pApp.height);
		
		pApp.shader(simpleDrawer);
		
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

	@Override
	public void oscEvent(OscMessage m) 
	{
		for(int i = 0; i < phones.size(); i++)
		{
			if(m.netAddress().equals(phones.get(i)))
			{
				System.out.println("Message recieved from " + m.netAddress());
				if(m.checkAddrPattern("/sensors"))
				{
					System.out.println("Sensor data recieved from " + m.netAddress());
					nebulae[i].velocity.x = PApplet.map(m.get(6).floatValue(), -PApplet.PI, PApplet.PI, -1, 1);
					nebulae[i].velocity.y = PApplet.map(m.get(7).floatValue(), -PApplet.PI/2, PApplet.PI/2, -1, 1);
				}
				return;
			}
		}
		
		if(phones.size() + 1 <= numActivePaintings)
			phones.add(m.netAddress());
	}
}
