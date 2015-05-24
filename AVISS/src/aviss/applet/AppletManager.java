package aviss.applet;

import java.util.concurrent.locks.ReentrantLock;

import processing.core.*;
import processing.opengl.PJOGL;

public class AppletManager {
	
	private static PApplet pApp = null;
	private static ReentrantLock pglLock = new ReentrantLock();
	public static void setApplet(PApplet applet){
		pApp = applet;
	}
	public static PApplet getApplet(){
		return pApp;
	}
	public static PJOGL startPGL(){
		pglLock.lock();
		return (PJOGL)pApp.beginPGL();
	}
	public static void endPGL(){
		pApp.endPGL();
		pglLock.unlock();
	}
}
