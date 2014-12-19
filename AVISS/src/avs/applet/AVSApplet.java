package avs.applet;

import g4p_controls.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import peasy.PeasyCam;
import processing.core.*;
import processing.opengl.*;
import wblut.math.*;
import wblut.processing.*;
import wblut.core.*;
import wblut.hemesh.*;
import wblut.geom.*;
import avs.audio.AudioManager;
import avs.data.AVGeneratorType;
import avs.visuals.*;

public class AVSApplet extends PApplet {

	private static final long serialVersionUID = 1L;
	
	private AudioManager audioManager;
	private AVSConsole settingsConsole;
	private HashMap<String, AVGenerator> avGenMap;
	private PeasyCam cam;
	private GImageButton settingsButton;
	
	private ReentrantLock genLock;
	public boolean reinitFlag = false;
	
	public void setup() {
		size(800, 800, OPENGL);						
		smooth(8);
		
		String[] settingsIcon = new String[]{				
				getClass().getResource("settings_24.png").getPath()
		};
		settingsButton= new GImageButton(this, width-35, height-35, settingsIcon);
		genLock = new ReentrantLock();
		
		cam = new PeasyCam(this, 0);
		cam.setMinimumDistance(50);
		cam.setMaximumDistance(2000);
		  
		audioManager = new AudioManager(this, 10);
		new Thread(audioManager).start();
		
		avGenMap = new HashMap<String, AVGenerator>();	
	}

	public void draw() {
		background(0);		
		
		if (aquireGeneratorLock()) {	
			
			if (audioManager.hasValues) {
				for (AVGenerator g : avGenMap.values()) {
					if (g != null)
						g.run();
				}
			}
			new Thread(audioManager).start();
			genLock.unlock();
		}
	}
	
	private void launchSettingConsole()
	{
		settingsConsole = new AVSConsole(this, audioManager);
		new Thread(settingsConsole).start();
	}
	
	public boolean aquireGeneratorLock(){
		boolean hasLock = false;
		try {
			hasLock = genLock.tryLock(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return hasLock;
	}
	
	public void releaseGeneratorLock(){
		genLock.unlock();
	}
	
	public void addAVGenerator(AVGeneratorType avGenType){
		AVGenerator newAVGen = null;
		switch(avGenType){
			case Displaced_Bezier_Surface:
				newAVGen = createBezierSurface();
				break;
			case Displaced_Grid_Surface:
				newAVGen = createGrid();
				break;
			case Geometry_Extrusion:
				newAVGen = createExtrusion();
				break;
			case Particles:
				newAVGen = createParticles();
				break;
		}
		if(newAVGen != null){
			newAVGen.init(this, audioManager);
			if (aquireGeneratorLock()) {	
				avGenMap.put(avGenType.toString(), newAVGen);
				genLock.unlock();
			}
		}
	}
	
	public void removeAVGenerator(String avGenName){
		if (aquireGeneratorLock()) {	
			avGenMap.remove(avGenName);
			genLock.unlock();
		}
	}
	
	public void reinitAVGenerators(){
		for (AVGenerator g : avGenMap.values()) {
			if (g != null)
				g.reInit();
		}
	}
	
	public BezierSurfaceGenerator createBezierSurface(){
		return new BezierSurfaceGenerator();
	}
	
	public GridGenerator createGrid(){
		return new GridGenerator();
	}
	
	public ExtrusionGenerator createExtrusion(){
//		HEC_Box box = new HEC_Box();
//		box.setWidth(200);// size of grid in U direction
//		box.setHeight(200);// size of grid in V direction
//		box.setDepth(200);
//		box.setWidthSegments(audioManager.getFFTHistory()).setHeightSegments((int)(audioManager.getFFTAvgSpecSize()* 0.5));

		HEC_Sphere sphere = new HEC_Sphere();
		sphere.setRadius(500)
			.setUFacets(audioManager.getFFTHistory())
			.setVFacets((int)(audioManager.getFFTAvgSpecSize()* 0.6));
		
		
		return new ExtrusionGenerator(sphere, audioManager.getFFTHistory(), (int)(audioManager.getFFTAvgSpecSize()* 0.6), 1);
	}
	
	public ParticleGenerator createParticles(){
		return new ParticleGenerator();
	}
	
	public void handleButtonEvents(GImageButton button, GEvent event){
		if (button == settingsButton && event == GEvent.CLICKED)
		{	
			launchSettingConsole();
		}
	}
}
