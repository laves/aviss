package aviss.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Utils {

	public static final int SIZEOF_INT = Integer.SIZE / 8;
	public static final int SIZEOF_FLOAT = Float.SIZE / 8;
	
	public static IntBuffer allocateDirectIntBuffer(int n) {
		  return ByteBuffer.allocateDirect(n * SIZEOF_INT).order(ByteOrder.nativeOrder()).asIntBuffer();
	}
		 
	public static FloatBuffer allocateDirectFloatBuffer(int n) {
		return ByteBuffer.allocateDirect(n * SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}


}
