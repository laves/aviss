package aviss.visuals;

import java.util.ArrayList;

import oscP5.OscMessage;
import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import aviss.data.FFTPacket;
import aviss.data.Particle;

public class ParticleGenerator implements AVGenerator {
	private AVISSApplet pApp;
	private AudioManager aMan;
	
	private ArrayList<Particle> particleList;
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) {
		
		pApp = applet;
		aMan = am;
		
		particleList = new ArrayList<Particle>();
		
		FFTPacket[] fftObjs = aMan.getFFTPackets();
		for(FFTPacket a: fftObjs){
			Particle p = new Particle(pApp, a);	
			particleList.add(p);
		}
	}

	@Override
	public void run() {
		for(Particle p: particleList){
			if(p.isDead)	
				p.revive();						
			else			
				p.run();			
		}
	}
	
	
	@Override
	public void reInit() {
		particleList = new ArrayList<Particle>();
		
		FFTPacket[] fftObjs = aMan.getFFTPackets();
		for(FFTPacket a: fftObjs){
			Particle p = new Particle(pApp, a);	
			particleList.add(p);
		}
	}

	@Override
	public void keyPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void oscEvent(OscMessage m) {
		// TODO Auto-generated method stub
		
	}
}
