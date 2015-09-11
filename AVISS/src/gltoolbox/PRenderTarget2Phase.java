package gltoolbox;

import processing.core.PApplet;
import processing.core.PGraphics;
import aviss.applet.PManager;

public class PRenderTarget2Phase {
	public PGraphics writeTo;
	public PGraphics readFrom;
	
	int width;
	int height;
	
	public PRenderTarget2Phase(int width, int height){
		writeTo = PManager.getApplet().createGraphics(width, height, PApplet.OPENGL);
		readFrom = PManager.getApplet().createGraphics(width, height, PApplet.OPENGL);	
		this.width = width;
		this.height = height;
//		readFrom.beginDraw();
//		readFrom.background(0);
//		readFrom.endDraw();
//		readFrom.beginDraw();
//		readFrom.fill(0);
//		readFrom.noStroke();		
//		readFrom.beginShape(PApplet.TRIANGLE_STRIP);
////		//readFrom.texture(AppletManager.getApplet().loadImage(getClass().getResource("/aviss/resources/black.png").getPath()));		
//		readFrom.vertex(0, height, 0, 1);
//		readFrom.vertex(0, 0, 0, 0);
//		readFrom.vertex(width, height, 1, 1);
//		readFrom.vertex(width, 0, 1, 0);
//		readFrom.endShape();
//		readFrom.endDraw();
		
	}
	
	public void clear()
	{
		writeTo.clear();
		readFrom.clear();
	}
	
	public void resize(int w, int h)
	{
		writeTo = PManager.getApplet().createGraphics(w, h, PApplet.P3D);	
		readFrom = PManager.getApplet().createGraphics(w, h, PApplet.P3D);	
		width = w;
		height = h;
	}
	
	public void swap()
	{
		PGraphics tmp = writeTo;
		writeTo = readFrom;
		readFrom = tmp;
	}
}
