package aviss.visuals;

import aviss.applet.AVISSApplet;
import aviss.audio.AudioManager;
import aviss.data.FFTPacket;
import KinectPV2.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import oscP5.OscMessage;

import com.jogamp.common.nio.Buffers;

import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;

public class KinectPointCloudGenerator implements AVGenerator{

	private AVISSApplet pApp;
	private AudioManager aMan;

	KinectPV2 kinect;

	float a = 0;
	int zval = 50;
	float scaleVal = 260;
	
	//Distance Threshold
	float maxD = 20f; //meters
	float minD = -5f;
	
	boolean ending = false;
	float endingCoeff = 0f;

	final static int NUM_LAYERS = 4;

	@Override
	public void init(AVISSApplet applet, AudioManager am) {
		pApp = applet;
		aMan = am;
		
		kinect = new KinectPV2(pApp);
		kinect.enablePointCloud(true);
		kinect.activateRawDepth(true);
		
		kinect.setLowThresholdPC(minD);
		kinect.setHighThresholdPC(maxD);
		kinect.init();
	}

	@Override
	public void reInit() {
		
	}

	@Override
	public void run() {
		  
		  kinect.setLowThresholdPC(minD);
		  kinect.setHighThresholdPC(maxD);
		  
		  FFTPacket[] fftPacs = aMan.getSortedFFTPackets(10);
		 		  
		  FloatBuffer pcb = kinect.getPointCloudDepthPos();		  
		  FloatBuffer picBuffer = Buffers.newDirectFloatBuffer(pcb.capacity());
		  
		  for(int layerIdx = 0; layerIdx < NUM_LAYERS; layerIdx++){			  
			  float soundEffect = fftPacs[layerIdx*5].getFFTAverageOverN(5)/300;			  
			  for(int buffIdx = 0; buffIdx < pcb.capacity(); buffIdx++)
			  {  				   
				  picBuffer.put(buffIdx, pcb.get(buffIdx) + ((soundEffect+ endingCoeff) * pApp.random(-1, 1)));
			  }
		  		  
			  PJOGL pgl = (PJOGL)pApp.beginPGL();
			  GL2 gl2 = pgl.gl.getGL2();
	
			  gl2.glEnable( GL2.GL_BLEND );
			  gl2.glEnable(GL2.GL_POINT_SMOOTH);      
			  gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			  gl2.glVertexPointer(3, GL2.GL_FLOAT, 0, picBuffer);
		
			  gl2.glTranslatef(pApp.width/2, pApp.height/2, zval);
			  gl2.glScalef(scaleVal, -1*scaleVal, scaleVal);
			  gl2.glRotatef(a, 0.0f, 1.0f, 0.0f);
		
			  gl2.glDrawArrays(GL2.GL_POINTS, 0, Constants.WIDTHDepth * Constants.HEIGHTDepth);
			  gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			  gl2.glDisable(GL2.GL_BLEND);
			  pApp.endPGL();
		  }
		  
		if (pApp.keyPressed) {
			if (pApp.key == '-') {
				scaleVal -= 5;
				pApp.text("Scale Value: " + scaleVal, 50, pApp.height - 50);
			} else if (pApp.key == '=') {
				scaleVal += 5;
				pApp.text("Scale Value: " + scaleVal, 50, pApp.height - 50);
			} else if (pApp.key == 'q') {
				minD += 0.1;
				pApp.text("Min Depth: " + minD, 50, pApp.height - 50);
			} else if (pApp.key == 'a') {
				minD -= 0.1;
				pApp.text("Min Depth: " + minD, 50, pApp.height - 50);
			} else if (pApp.key == 'w') {
				maxD += 0.1;
				pApp.text("Max Depth: " + maxD, 50, pApp.height - 50);
			} else if (pApp.key == 's') {
				maxD -= 0.1;
				pApp.text("Max Depth: " + maxD, 50, pApp.height - 50);
			}
			else if (pApp.key == ' ') {
				endingCoeff += 0.01;
			}
			else if (pApp.keyCode == PApplet.LEFT) {
				endingCoeff = 2;
			}
			else if (pApp.keyCode == PApplet.UP) {
				endingCoeff = ((float)Math.floor(pApp.random(0f, 20)))/10f;
			}
			else if (pApp.keyCode == PApplet.RIGHT) {
				endingCoeff = 0;
			}
		}
		pApp.stroke(255, 0, 0);	
	}

	@Override
	public void keyPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void oscEvent(OscMessage m) 
	{
		
	}
}
