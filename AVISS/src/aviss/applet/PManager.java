package aviss.applet;

import java.util.concurrent.locks.ReentrantLock;

import processing.core.*;
import processing.opengl.PJOGL;

public class PManager {
	
	private static PApplet pApp = null;
	private static PJOGL pgl = null;
	private static ReentrantLock pglLock = new ReentrantLock();
	private static boolean pglActive = false;
	
	public static void setApplet(PApplet applet)
	{
		pApp = applet;
	}
	public static PApplet getApplet()
	{
		return pApp;
	}
	public static PJOGL getPGL()
	{
		pglLock.lock();
		if(pglActive)
		{
			pglLock.unlock();
			return pgl;
		}
		else
		{
			pgl = (PJOGL)pApp.beginPGL();
			pglActive = true;
			pglLock.unlock();			
			return pgl;
		}
	}
	public static void endPGL()
	{
		pglLock.lock();
		if(pglActive)
		{

			pglActive = false;
			pApp.endPGL();
			pgl = null;

		}
		pglLock.unlock();
//		pglLock.unlock();
	}
}
