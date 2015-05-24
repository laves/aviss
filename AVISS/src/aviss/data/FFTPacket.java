package aviss.data;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.queue.CircularFifoQueue;

public class FFTPacket 
{
	private CircularFifoQueue<Float> fftDataBuffer;
	private int fftBandNumber;
	private int historySize;
	private float latestFFTValue = 0f;
	
	private ReentrantLock dataLock;
	
	public FFTPacket(int histSize, int bandNum)
	{
		historySize = histSize;
		dataLock = new ReentrantLock();
		fftDataBuffer = new CircularFifoQueue<Float>(historySize);
		fftBandNumber = bandNum;
	}
	
	public FFTPacket(int histSize, float fftVal, int bandNum)
	{
		historySize = histSize;
		dataLock = new ReentrantLock();
		fftDataBuffer = new CircularFifoQueue<Float>();
		fftDataBuffer.add(fftVal);
		fftBandNumber = bandNum;
	}
	
	public void setBandNumber(int bandNum){
		fftBandNumber = bandNum;
	}
	
	public void resetHistorySize(int histSize)
	{	
		boolean hasLock = false;
		try {
			hasLock = dataLock.tryLock(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(hasLock)
		{
			historySize = histSize;
			CircularFifoQueue<Float> newFFTDataBuffer = new CircularFifoQueue<Float>(historySize);
			for(Float f: fftDataBuffer)
				newFFTDataBuffer.add(f);
			fftDataBuffer = newFFTDataBuffer;
			dataLock.unlock();
		}
	}
	
	public void addFFTData(float fftVal){
		boolean hasLock = false;
		try {
			hasLock = dataLock.tryLock(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(hasLock)
		{
			latestFFTValue = fftVal;
			fftDataBuffer.add(fftVal);
			dataLock.unlock();
		}
	}
	
	public int getBandNumber(){
		return fftBandNumber;
	}
	
	public float getLatestFFTValue(){
		return latestFFTValue;
	}
	
	public float[] getFFTData(){
		boolean hasLock = false;
		try {
			hasLock = dataLock.tryLock(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(hasLock)
		{
			float[] fftData = new float[fftDataBuffer.size()];
			Iterator<Float> itr = fftDataBuffer.iterator();
			int i = 0;
			while(itr.hasNext())
				fftData[i++] = itr.next();
			dataLock.unlock();
			return fftData;
		}
		else
			return null;
	}
	
	public float[] getNMostRecentFFTValues(int n)
	{
		boolean hasLock = false;
		try {
			hasLock = dataLock.tryLock(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (hasLock) {
			int skipSize = fftDataBuffer.size() - n;
			if (skipSize < 0)
				skipSize = 0;
			
			Iterator<Float> itr = fftDataBuffer.iterator();
			float[] retArray = new float[n];
			int i = 0;
			int j = 0;
			while (itr.hasNext()) {
				if (i < skipSize) {
					itr.next();
					i++;
				} else
					retArray [j] = itr.next();
					j++;
			}
			dataLock.unlock();
			return retArray;
		}
		else 
			return null;
	}
	
	public float getFFTAverageOverN(int n)
	{
		boolean hasLock = false;
		try {
			hasLock = dataLock.tryLock(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (hasLock) {
			int skipSize = fftDataBuffer.size() - n;
			if (skipSize < 0)
				skipSize = 0;
			Iterator<Float> itr = fftDataBuffer.iterator();
			float sum = 0;
			int i = 0;
			while (itr.hasNext()) {
				if (i < skipSize) {
					itr.next();
					i++;
				} else
					sum += itr.next();
			}
			dataLock.unlock();
			return (sum / n);
		}
		else 
			return 0f;
	}
}
