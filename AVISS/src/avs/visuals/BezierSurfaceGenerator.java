package avs.visuals;

import avs.applet.AVSApplet;
import avs.audio.AudioManager;
import avs.data.FFTPacket;
import processing.core.*;
import processing.opengl.*;
import wblut.geom.WB_BezierSurface;
import wblut.geom.WB_Point;
import wblut.hemesh.HEC_FromSurface;
import wblut.hemesh.HET_Diagnosis;
import wblut.hemesh.HE_Mesh;
import wblut.hemesh.HE_Vertex;
import wblut.processing.WB_Render;


public class BezierSurfaceGenerator implements AVGenerator
{
	public HE_Mesh mesh;
	private WB_BezierSurface avs;
	private HEC_FromSurface creator;
	private HE_Vertex[] vertices;
	private WB_Render render;	
	
	double gridStepX;
	double gridStepY;
	
	private static final int U = 30;
	private static final int V = 30;
	private final double iU = 1.0 / (U-1);
	private final double iV = 1.0 / (V-1);
	
	private AVSApplet pApp;
	private AudioManager aMan;
	
	@Override
	public void init(AVSApplet applet, AudioManager am) {
		pApp = applet;
		aMan = am;
		render = new WB_Render(applet);		
		creator = new HEC_FromSurface();	
		
		gridStepX = pApp.width/(aMan.getFFTHistory()-1);
		gridStepY = pApp.height/(aMan.getFFTAvgSpecSize()-1);	
		
		WB_Point[][] points = new WB_Point[aMan.getFFTHistory()][aMan.getFFTAvgSpecSize()];
		for (int y = 0; y < aMan.getFFTAvgSpecSize(); y++) {	
			for (int x = 0; x < aMan.getFFTHistory(); x++) {
				 points[x][y]=new WB_Point((x*gridStepX), (y*gridStepY),
		        		Math.random()*300);
			}
		}

		avs = new WB_BezierSurface(points);		
			
		creator.setSurface(avs);// surface can be any implementation of the		
		creator.setU(U-1);// steps in U direction
		creator.setV(V-1);// steps in V direction
		creator.setUWrap(false);// wrap around in U direction
		creator.setVWrap(false);// wrap around in V direction
		mesh = new HE_Mesh(creator);
		
		HET_Diagnosis.validate(mesh);
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
		
		for (int y = 0; y < aMan.getFFTAvgSpecSize(); y++) {
			FFTPacket focusFreq = aMan.getFFTPackets()[y];
			for (int x = 1; x < aMan.getFFTHistory(); x++) {
				avs.points()[x][y].setX((x - 1) * gridStepX);
				avs.points()[x - 1][y].set(avs.points()[x][y]);

				if (x == aMan.getFFTHistory() - 1) {
					float level = focusFreq.getFFTData()[x];
					WB_Point newDataPt = new WB_Point((x * gridStepX) ,
							(y * gridStepY), ((level * (y * 10)) / 100));
					avs.points()[x][y] = newDataPt;
				}
			}
		}

		vertices = mesh.getVerticesAsArray();
		for (int y = 0; y < V; y++) {
			final double v = avs.lowerv() + y * iV
					* (avs.upperv() - avs.lowerv());
			for (int x = 0; x < U; x++) {
				double u = avs.loweru() + x * iU
						* (avs.upperu() - avs.loweru());
				WB_Point wbp = avs.surfacePoint(u, v);
				vertices[(y * V) + x].getPoint().set(wbp);
			}

		}
		mesh.resetFaces();
	}

	@Override
	public void reInit() {
		WB_Point[][] points = new WB_Point[aMan.getFFTHistory()][aMan.getFFTAvgSpecSize()];
		for (int y = 0; y < aMan.getFFTAvgSpecSize(); y++) {	
			for (int x = 0; x < aMan.getFFTHistory(); x++) {
				 points[x][y]=new WB_Point((x*gridStepX), (y*gridStepY),
		        		Math.random()*300);
			}
		}

		avs = new WB_BezierSurface(points);		
		
		creator.setSurface(avs);// surface can be any implementation of the		
		creator.setU(U-1);// steps in U direction
		creator.setV(V-1);// steps in V direction
		creator.setUWrap(false);// wrap around in U direction
		creator.setVWrap(false);// wrap around in V direction
		mesh = new HE_Mesh(creator);
	}
}
