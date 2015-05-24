package aviss.visuals;

import oscP5.OscMessage;
import processing.core.PApplet;
import wblut.geom.WB_BezierSurface;
import wblut.geom.WB_Point;
import wblut.hemesh.HEC_FromSurface;
import wblut.hemesh.HEC_Grid;
import wblut.hemesh.HE_Mesh;
import wblut.hemesh.HE_Vertex;
import wblut.processing.WB_Render;
import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import aviss.data.FFTPacket;

public class GridGenerator implements AVGenerator {

	public HE_Mesh mesh;
	private HEC_Grid creator;
	private WB_Render render;	
	
	private int U;
	private int V;
	
	private AVISSApplet pApp;
	private AudioManager aMan;
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) {
		pApp = applet;
		aMan = am;
		render = new WB_Render(pApp);
		
		U = am.getFFTHistory();
		V = am.getFFTAvgSpecSize();
		
		creator = new HEC_Grid();
		creator.setU(U-1);// number of cells in U direction
		creator.setV(V-1);// number of cells in V direction
		creator.setUSize(800);// size of grid in U direction
		creator.setVSize(800);// size of grid in V direction
		mesh = new HE_Mesh(creator);

	}

	@Override
	public void run() {
		pApp.directionalLight(255, 255, 255, 1, 1, -1);
		pApp.directionalLight(255, 255, 255, -1, -1, 1);

		//pApp.translate(300, 300, -900);

		//pApp.rotateY(pApp.mouseX * 1.0f / pApp.width * PApplet.TWO_PI);
		//pApp.rotateX(pApp.mouseY * 1.0f / pApp.height * PApplet.TWO_PI);

		pApp.noStroke();
		render.drawFaces(mesh);
		pApp.stroke(0);
	    render.drawEdges(mesh);
				
		if(aMan.getFFTValCount() >= U){
		float[][] vals = new float[U][V];
		for (int y = 0; y < V; y++) {
			FFTPacket focusFreq = aMan.getFFTPackets()[y];
			for (int x = 0; x < U; x++) {
					float level = focusFreq.getFFTData()[x];
					vals[x][y] = (level * (y * 10)) / 50;
			}
		}
		
		creator.setWValues(vals);
		mesh = new HE_Mesh(creator);
		}
	}

	@Override
	public void reInit() {
		U = aMan.getFFTHistory();
		V = aMan.getFFTAvgSpecSize();
		
		creator = new HEC_Grid();
		creator.setU(U-1);// number of cells in U direction
		creator.setV(V-1);// number of cells in V direction
		creator.setUSize(800);// size of grid in U direction
		creator.setVSize(800);// size of grid in V direction
		mesh = new HE_Mesh(creator);
		
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
