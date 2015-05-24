package aviss.audio;

import ddf.minim.*;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import aviss.data.*;
import processing.core.PApplet;

public class AudioManager implements Runnable
{	
	//22,20 for surface
	public int fftMinOctaveSize = 22;
	public int fftNumOctaveBands = 10;
	public boolean hasValues;
	
	
	private Minim minim;
	private AudioPlayer player;
	private AudioInput in;
	private FFT fft;
	private BeatDetect beatDetector;

	private FFTPacket[] fftPackets;
	private FFTPacket[] sortedFFTPackets;
	
	private int bufferSize = 1024;
	private String inputFilePath;
	private int fftHistorySize;
	private int fftValCount;
	private float outputGain = 5f;
	
	private boolean audioInEnabled;
	private ReentrantLock fftDataLock;
	
	public AudioManager(PApplet pApp, int fftHistorySize)
	{
		minim = new Minim(pApp);
		this.fftHistorySize = fftHistorySize;
		
		enableAudioIn();	
		initFFT(in.sampleRate());
		fftDataLock = new ReentrantLock();
	}
	
	public AudioManager(PApplet pApp, int fftHistorySize, String audioFileIn, int bufferSize, boolean loopFile)
	{
		minim = new Minim(pApp);	
		this.fftHistorySize = fftHistorySize;
		this.bufferSize = bufferSize; 
		
		setPlaybackAudioFile(audioFileIn, loopFile);
		initFFT(player.sampleRate());		
		fftDataLock = new ReentrantLock();
	}

	public boolean setPlaybackAudioFile(String filename, boolean loopFile)
	{			
		this.inputFilePath = filename;
		
		if(player!=null)
			player.close();
		player = minim.loadFile(filename, bufferSize);
		
		if(player != null)
		{
			startAudioFilePlayback(loopFile);
			return true;
		}
		else
			return false;
	}
	
	private void initFFT(float sampleRate)
	{
		fft = new FFT(bufferSize, sampleRate);
		fft.window(FFT.NONE);
		fft.logAverages(fftMinOctaveSize, fftNumOctaveBands);
	
		beatDetector = new BeatDetect();
		initFFTPackets(fft.avgSize());
	}
	
	private void initFFTPackets(int numFFTObjs){
		hasValues = false;
		fftValCount = 0;
		
		fftPackets = new FFTPacket[numFFTObjs];
		sortedFFTPackets = new FFTPacket[numFFTObjs];
		for(int i = 0; i < numFFTObjs; i++)
		{
			FFTPacket f = new FFTPacket(fftHistorySize, i);
			fftPackets[i] = f;
			sortedFFTPackets[i] = f;
		}		
	}
	
	public void startAudioFilePlayback(boolean loopFile){
		
		if(in != null)
			in.mute();
		
		audioInEnabled = false;
		
		if (player != null) {
			if (loopFile)
				player.loop();
			else
				player.play();
		}
	}
	
	public void enableAudioIn(){
		if(player != null){
			if(player.isPlaying()){
				player.pause();
				player.rewind();
			}
		}		
		audioInEnabled = true;
		
		if(in != null)
			in.unmute();
		else
			in = minim.getLineIn(Minim.STEREO, bufferSize);	
	}
	
	public int getAudioFileLength(){
		if(player != null)
			return player.length();
		else
			return -1;
	}
	
	public int getPlayerPosition(){
		if(player != null)
			return player.position();
		else
			return -1;
	}
	
	public boolean isPlayerPlaying(){
		if(player != null)
			return player.isPlaying();
		else
			return false;
	}
	
	public boolean isAudioIn(){
		return audioInEnabled;
	}
	
	public int getFFTAvgSpecSize(){
		return fft.avgSize();
	}
	
	public float[] getRawFFTValues(){
		return fft.getSpectrumReal();
	}
	
	public int getFFTHistory(){
		return fftHistorySize;
	}
	
	public float getMixEnergy(){
		AudioBuffer buff;
		if(audioInEnabled)
			buff = in.mix;
		else
			buff = player.mix;
		return buff.level();
	}
	
	public FFTPacket[] getSortedFFTPackets(int nAvg){
		if(aquireFFTDataLock())
		{
			quickSort(sortedFFTPackets, 0, sortedFFTPackets.length - 1, nAvg);
			fftDataLock.unlock();
		}
		return sortedFFTPackets;
	}
	
	public boolean isBeatOnset(){
		return beatDetector.isOnset();
	}
	
	public FFTPacket[] getFFTPackets(){
		return fftPackets;
	}
	
	public int getFFTValCount(){
		return fftValCount;
	}
	
	public void setInputGain(float newGain){
		if(in != null)
			in.setGain(newGain);
	}
	
	public void setPlayerVolume(float newVol){
		outputGain = newVol;
		if(player != null){
			player.setVolume(newVol);
		}
	}
	
	public void seekPlayer(int time){
		if(player != null){
			player.cue(time);
		}
	}
	
	public void pausePlayer(){
		if(player != null)
			player.pause();
	}
	
	public void playPlayer(){
		if(player != null)
			player.play(player.position());
	}
	
	public void setFFTMinOctaveSize(int val){
			fft.logAverages((fftMinOctaveSize = val), fftNumOctaveBands);	
			initFFTPackets(fft.avgSize());
	}
	
	public void setFFTNumOctaveBands(int val){
			fft.logAverages(fftMinOctaveSize, (fftNumOctaveBands = val));	
			initFFTPackets(fft.avgSize());
	}
	
	public void setFFTWindowType(String winType){
			if (winType.equals("None"))
				fft.window(FFT.NONE);
			else if (winType.equals("Bartlett"))
				fft.window(FFT.BARTLETT);
			else if (winType.equals("Bartlett-Hann"))
				fft.window(FFT.BARTLETTHANN);
			else if (winType.equals("Blackman"))
				fft.window(FFT.BLACKMAN);
			else if (winType.equals("Cosine"))
				fft.window(FFT.COSINE);
			else if (winType.equals("Gauss"))
				fft.window(FFT.GAUSS);
			else if (winType.equals("Hamming"))
				fft.window(FFT.HAMMING);
			else if (winType.equals("Hann"))
				fft.window(FFT.HANN);
			else if (winType.equals("Lanczos"))
				fft.window(FFT.LANCZOS);
			else if (winType.equals("Triangular"))
				fft.window(FFT.TRIANGULAR);
	}
	
	public void setFFTAverageType(String avgType){
			if (avgType.equals("Logarithmic"))
				fft.logAverages(fftMinOctaveSize, fftNumOctaveBands);
			else if (avgType.equals("Linear"))
				fft.linAverages(fftNumOctaveBands);
			
			initFFTPackets(fft.avgSize());
	}

	public void setFFTSize(int size, String winType, String avgType){
		bufferSize = size;
			if (audioInEnabled) {
				if (in == null)
					return;
				in = minim.getLineIn(Minim.STEREO, size);
				fft = new FFT(size, in.sampleRate());
			} else {
				if (player == null)
					return;
				player.close();
				setPlaybackAudioFile(inputFilePath, false);
				fft = new FFT(size, player.sampleRate());
			}
			setFFTWindowType(winType);
			setFFTAverageType(avgType);
	}
	
	public boolean aquireFFTDataLock(){
		boolean hasLock = false;
		try {
			hasLock = fftDataLock.tryLock(200, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return hasLock;
	}
	
	public void releaseFFTDataLock(){
		fftDataLock.unlock();
	}
	
	public void advanceFFT(){
		if (aquireFFTDataLock()) 
		{
			AudioBuffer buff;
			if(audioInEnabled)
			{
				if(in == null)
					return;
				buff = in.mix;
			}
			else
			{
				if(player == null)
					return;
				buff = player.mix;
			}
			beatDetector.detect(buff);
			
			fft.forward(buff);					
			
			for(int i = 0; i < fftPackets.length; i++)
			{
				fftPackets[i].addFFTData(fft.getAvg(i) * (float)Math.log(i+2));
				sortedFFTPackets[i] = fftPackets[i];
			}
			
			if (!hasValues) {
				if (fftValCount < fftHistorySize)
					fftValCount++;
				else
					hasValues = true;
			}
			fftDataLock.unlock();
		}
	}
	
	private int partition(FFTPacket arr[], int left, int right, int nAvg)
	{
	      int i = left, j = right;
	      FFTPacket tmp;
	      float pivot = arr[(left + right) / 2].getFFTAverageOverN(nAvg);
	     
	      while (i <= j) {
	            while (arr[i].getFFTAverageOverN(nAvg) > pivot)
	                  i++;
	            while (arr[j].getFFTAverageOverN(nAvg) < pivot)
	                  j--;
	            if (i <= j) {
	                  tmp = arr[i];
	                  arr[i] = arr[j];
	                  arr[j] = tmp;
	                  i++;
	                  j--;
	            }
	      };	     
	      return i;
	}
	 
	private void quickSort(FFTPacket arr[], int left, int right, int nAvg) {
	      int index = partition(arr, left, right, nAvg);
	      if (left < index - 1)
	            quickSort(arr, left, index - 1, nAvg);
	      if (index < right)
	            quickSort(arr, index, right, nAvg);
	}
	

	@Override
	public void run() 
	{
		advanceFFT();						
	}
}
