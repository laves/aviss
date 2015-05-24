package avs.visuals;

import avs.applet.AVSApplet;
import avs.audio.AudioManager;

public interface AVGenerator {

	public void init(AVSApplet applet, AudioManager am);
	public void reInit();
	public void run();	
	
}
