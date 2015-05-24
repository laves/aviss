package aviss.visuals;

import oscP5.OscMessage;
import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;

public interface AVGenerator {

	public void init(AVISSApplet applet, AudioManager am);
	public void reInit();
	public void run();
	public void keyPressed();
	public void oscEvent(OscMessage m);
}
