package aviss.visuals;

import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.hash.TLongDoubleHashMap;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import oscP5.OscMessage;
import javolution.util.FastMap;
import javolution.util.FastTable;
import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import aviss.data.FFTPacket;
import processing.core.*;
import processing.opengl.*;
import wblut.geom.WB_BezierSurface;
import wblut.geom.WB_Point;
import wblut.geom.WB_Vector;
import wblut.hemesh.HEC_Creator;
import wblut.hemesh.HEC_FromSurface;
import wblut.hemesh.HEC_Sphere;
import wblut.hemesh.HEM_Extrude;
import wblut.hemesh.HET_Diagnosis;
import wblut.hemesh.HE_Face;
import wblut.hemesh.HE_Halfedge;
import wblut.hemesh.HE_Mesh;
import wblut.hemesh.HE_Selection;
import wblut.hemesh.HE_Vertex;
import wblut.math.WB_Parameter;
import wblut.processing.WB_Render;


public class ExtrusionGenerator implements AVGenerator
{
	public HE_Mesh mesh;
	private HEC_Creator creator;	
	private HE_Selection selection;
	private HEM_Extrude modifier;
	private WB_Render render;
		
	private int U;
	private int V;
	private int faceMultiplier;
	private PVector location;
	private PVector velocity;
	private PVector acceleration;
	
	private AVISSApplet pApp;
	private AudioManager audioManager;
	
	public boolean isAppletRunning;
	public boolean isAlive;
	
	public ExtrusionGenerator(HEC_Creator cr, int X, int Y, int faceMulti)
	{
		creator = cr;
		mesh = new HE_Mesh(creator);
		selection = new HE_Selection(mesh);
		
		U = X;
		V = Y;
		faceMultiplier = faceMulti;
		
		location = new PVector(300, 300, -1500);
		velocity = new PVector(15, 45, 0);
		acceleration = new PVector(0, 0.05f, 0);
	}
	
	@Override
	public void init(AVISSApplet applet, AudioManager am) {
		pApp = applet;
		audioManager = am;
		render = new WB_Render(applet);		
		
		modifier = new HEM_Extrude();
		modifier.setDistance(0);// extrusion distance, set to 0 for inset faces
		modifier.setRelative(false);// treat chamfer as relative to face size or
									// as absolute value
		modifier.setChamfer(10);// chamfer for non-hard edges
		modifier.setHardEdgeChamfer(200);// chamfer for hard edges
		modifier.setThresholdAngle(1.5 * (Math.PI / 2));// treat edges sharper
														// than this angle as
														// hard edges
		modifier.setFuse(false);// try to fuse planar adjacent planar faces
								// created by the extrude
		modifier.setFuseAngle(0.05 * (Math.PI / 2));// threshold angle to be
													// considered coplanar
		modifier.setPeak(false);// if absolute chamfer is too large for face,
								// create a peak on the face

	}
	
	@Override
	public void run() 
	{		
									
		pApp.directionalLight(255, 255, 255, 1, 1, -1);
		pApp.directionalLight(255, 255, 255, -1, -1, 1);

		//pApp.translate(location.x, location.y, location.z);

	//	pApp.rotateY(pApp.mouseX * 1.0f / pApp.width * PApplet.TWO_PI);
	//	pApp.rotateX(pApp.mouseY * 1.0f / pApp.height * PApplet.TWO_PI);
//		pApp.rotateX(velocity.x / 2);
	//	pApp.rotateY(velocity.y / 2);
		//pApp.rotateZ(velocity.z / 2);
		
		pApp.noStroke();
		render.drawFaces(mesh);
		//pApp.stroke(0);
	//	render.drawEdges(mesh);

		velocity.add(acceleration);
	    location.add(velocity);
	    if(location.x > pApp.width || location.x < 0)
	    	velocity.x *= -1;
	    if(location.y > pApp.height || location.y < 0)
	    	velocity.y *= -1;
	    

		HE_Mesh tmpMesh = new HE_Mesh(creator);
		selection = new HE_Selection(tmpMesh);
		
		HE_Face[] faces = tmpMesh.getFacesAsArray();
		
		for (int i = 0; i < faceMultiplier; i++) {
			for (int y = 0; y < V; y++) {
				float[] fftVals = audioManager.getFFTPackets()[y % V]
						.getFFTData();
				for (int x = 0; x < U; x++) {
					int faceIndex = (y * U) + x + (i * (U * V));
					if(faceIndex < faces.length){
					if (fftVals[x] < 1)
						fftVals[x] = 1;
					selection.add(faces[faceIndex]);
					modifier.setDistance((fftVals[x] * (y * 5)) / 100);
					tmpMesh.modifySelected(modifier, selection);
					selection.remove(faces[faceIndex]);
					}
				}
			}
		}
		mesh = tmpMesh;
	}

	@Override
	public void reInit() {
		U = audioManager.getFFTHistory(); 
		V = (int)(audioManager.getFFTAvgSpecSize()* 0.5);		
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
