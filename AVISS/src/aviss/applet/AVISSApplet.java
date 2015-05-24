package aviss.applet;

import g4p_controls.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import peasy.PeasyCam;
import processing.core.*;
import wblut.hemesh.*;
import aviss.audio.AudioManager;
import aviss.data.AVGeneratorType;
import aviss.data.Star.StarState;
import aviss.visuals.*;

public class AVISSApplet extends PApplet {

	private static final long serialVersionUID = 1L;
	
	private AudioManager audioManager;
	private AVISSConsole settingsConsole;
	private OscP5 osc;
	private NetAddress address;
	private HashMap<String, AVGenerator> avGenMap;
	private PeasyCam cam;
	
	private String vKey = "ksc";
	
	private GImageButton settingsButton;
	
	private ReentrantLock genLock;
	public boolean reinitFlag = false;
	
	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", AVISSApplet.class.getName() });
	}
	
	public void setup() {
		size(displayWidth, displayHeight, OPENGL);				
		smooth(8);
		noCursor();
		
		String[] settingsIcon = new String[]{				
				getClass().getResource("/aviss/resources/settings_24.png").getPath()
		};
		settingsButton= new GImageButton(this, width-35, height-35, settingsIcon);
		genLock = new ReentrantLock();
		
//		cam = new PeasyCam(this, 1500);
//		cam.setMinimumDistance(0);
//		cam.setMaximumDistance(2000);
		osc = new OscP5(this,12000);
		address = new NetAddress("142.103.165.226", 12000);
		
		AppletManager.setApplet(this);
		audioManager = new AudioManager(this, 40);//, "D:/XPS/Kendrick Lamar - To Pimp a Butterfly (2015) [MP3 320 KBPS]~{VBUc}/15 i.mp3", 1024, true);
		new Thread(audioManager).start();
		
		avGenMap = new HashMap<String, AVGenerator>();		
		//addAVGenerator(AVGeneratorType.Kinect_Star_Cloud);
		//addAVGenerator(AVGeneratorType.Image_Overlay);
		AVGenerator ksc = createKinectStarCloud();
		ksc.init(this, audioManager);
		avGenMap.put("ksc", ksc);
		AVGenerator io = createImageOverlay();
		io.init(this, audioManager);
		avGenMap.put("io", io);
		
		
	}

	public void draw() 
	{
		background(0);		
		
		if (aquireGeneratorLock()) 
		{		
			if (audioManager.hasValues) {
				avGenMap.get(vKey).run();
//				for (AVGenerator g : avGenMap.values()) {
//					if (g != null)
//						g.run();
//				}
			}
			new Thread(audioManager).start();
			genLock.unlock();
		}
	}
	
	private void launchSettingConsole()
	{
		settingsConsole = new AVISSConsole(this, audioManager);
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
			case Kinect_Point_Cloud:
				newAVGen = createKinectPointCloud();
				break;
			case Kinect_Star_Cloud:
				newAVGen = createKinectStarCloud();
				break;
			case Image_Overlay:
				newAVGen = createImageOverlay();
				break;
		default:
			break;
		}
		
		if(newAVGen != null)
		{
			while (!audioManager.hasValues) 
			{
				new Thread(audioManager).start();
			}
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
	
	private BezierSurfaceGenerator createBezierSurface(){
		return new BezierSurfaceGenerator();
	}
	
	private GridGenerator createGrid(){
		return new GridGenerator();
	}
	
	private ExtrusionGenerator createExtrusion(){
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
	
	private ParticleGenerator createParticles(){
		return new ParticleGenerator();
	}
	
	private KinectPointCloudGenerator createKinectPointCloud(){
		return new KinectPointCloudGenerator();
	}
	
	private KinectStarCloudGenerator createKinectStarCloud(){
		return new KinectStarCloudGenerator(osc, address);
	}
	
	private ImageOverlayGenerator createImageOverlay(){
		return new ImageOverlayGenerator(osc, address);
	}
	
	public void handleButtonEvents(GImageButton button, GEvent event){
		if (button == settingsButton && event == GEvent.CLICKED)
		{	
			launchSettingConsole();
		}
	}
	
	public void keyPressed(){
		if (key == '6') 
		{
			vKey = "io";
		}	
		else if(key == '1' || key == '2' || key == '3' || key == '4' || key == '5' || key == '7')
		{
			vKey = "ksc";
		}
		
		if (aquireGeneratorLock()) 
		{		
			for (AVGenerator g : avGenMap.values()) {
				if (g != null)
					g.keyPressed();
			}		
			genLock.unlock();
		}
		
	}
	
	public void oscEvent(OscMessage m) 
	{
		if (aquireGeneratorLock()) 
		{		
			for (AVGenerator g : avGenMap.values()) {
				if (g != null)
					g.oscEvent(m);
			}		
			genLock.unlock();
		}
	}
}
