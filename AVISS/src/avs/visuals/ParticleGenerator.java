package avs.visuals;

import java.util.ArrayList;

import avs.applet.AVSApplet;
import avs.audio.AudioManager;
import avs.data.FFTPacket;
import avs.data.Particle;

public class ParticleGenerator implements AVGenerator {
	private AVSApplet pApp;
	private AudioManager aMan;
	
	private ArrayList<Particle> particleList;
	
	@Override
	public void init(AVSApplet applet, AudioManager am) {
		
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
}
