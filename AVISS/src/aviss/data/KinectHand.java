package aviss.data;

import KinectPV2.KinectPV2;
import processing.core.PApplet;
import processing.core.PVector;

public class KinectHand {

	public PVector rightHandLocation;
	public PVector leftHandLocation;
	public Integer rightHandState;
	public Integer leftHandState;
	public boolean rightHandOSCReset;
	public boolean leftHandOSCReset;
	
	public static final float M = 100000f;	
	private static final float G = 0.4f;
	
	public KinectHand(){
		rightHandOSCReset = true;
		leftHandOSCReset = true;
	}
	
	public PVector attract(Star s){
		
		PVector force = new PVector(0, 0, 0);
		if(!rightHandState.equals(KinectPV2.HandState_NotTracked) && !rightHandState.equals(KinectPV2.HandState_Unknown))
    	{
			PVector tmpF = PVector.sub(rightHandLocation, s.location); // Calculate direction of force
		    float d = tmpF.mag();                               // Distance between objects
		    d = PApplet.constrain(d, 1.0f, 10000.0f);         // Limiting the distance to eliminate "extreme" results for very close or very far objects
		    float strength = (G * M * s.mass) / (d * d);      // Calculate gravitional force magnitude
		    tmpF.setMag(strength);   // Get force vector --> magnitude * direction
		    force.add(tmpF);
    	}
		if(!leftHandState.equals(KinectPV2.HandState_NotTracked) && !leftHandState.equals(KinectPV2.HandState_Unknown))
    	{
			PVector tmpF = PVector.sub(leftHandLocation, s.location); // Calculate direction of force
		    float d = tmpF.mag();                               // Distance between objects
		    d = PApplet.constrain(d, 1.0f, 10000.0f);         // Limiting the distance to eliminate "extreme" results for very close or very far objects
		    float strength = (G * M * s.mass) / (d * d);      // Calculate gravitional force magnitude
		    tmpF.setMag(strength);   // Get force vector --> magnitude * direction
		    force.add(tmpF);
    	}
	    return force;
	}

}
