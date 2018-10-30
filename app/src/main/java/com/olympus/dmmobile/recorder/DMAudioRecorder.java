package com.olympus.dmmobile.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;

import com.olympus.dmmobile.R;
/**
 * DMAudioRecorder is a helper class to manage audio recording using AudioRecord API
 * @version 1.0.1
 * 
 */
public class DMAudioRecorder{
	static{
		System.loadLibrary("native_lib");
	}
	
	/**
	 * native function to initialize VCVA detection
	 * @param arg sample rate at which the audio is recorded
	 * @param VCVALevel level at which VCVA detects audio frame
	 * @return
	 */
	public native long init(long arg,long VCVALevel);
	
	/**
	 * native function used to tell whether the current audio is voiced audio frame or silence
	 * @param audio buffer with the audio samples recorded
	 * @return returns true if it contains voice frame else return false
	 */
	public native boolean isVoicedFrame(short[] audio);
	
	private static final int SAMPLE_RATE = 16000;				// sample rate : 16 kHz
    private static final int BITS_PER_SAMPLE = 16;				// PCM-16
    private static final int NUMBER_OF_CHANNELS = 1;			// MONO
	private static final int TIMER_INTERVAL =30;				// 30 milliseconds
	private static int BYTE_RATE=(SAMPLE_RATE*NUMBER_OF_CHANNELS)*(BITS_PER_SAMPLE/8);
	private static double mMicrosensitivity;
	public static enum State {INIT, RDY, REC, ERR, STOP};		// various states of recorder
	public enum VCVAMODE {ENABLED,DISABLED};					// VCVA mode
	public int VCVALevel=0;										// VCVA detection level
	
	//types of file processing
	static final int FILE_NEW=0;
	static final int FILE_EXISTING=1;
	static final int FILE_TEMPLATE=2;
	
    private final long LIMIT=2;									// 2 GB
    private final long MAX_LIMIT=1073741824;				
    private final double THRESHOLD=0.50;						//to identify the pause position in first or second half
    	
	static String audioPath;
	long lCount;
	public AudioRecord	audioRecorder = null;
	private int	cAmplitude= 0;
	
	public interface AudioRecordStateListener{
		
		/**
		 * Callback to DictateActivity to update the progress of currently recording dictation
		 * @param status VCVA detection status
		 * @param cAmplitude the highest amplitude sample in the current recorded frame
		 * @param current_duration duration of audio recorded in current session
		 * @param total_duration total duration of the audio file
		 */
		public void updateProgress(boolean status,int cAmplitude,long current_duration,long total_duration);
		/**
		 * Callback to DictateActivity to draw the WAV graph with the latest recorded audio
		 * @param total_duration the total duration of the recorded dictation
		 */
		public void updateGraph(long total_duration);
		
		/**
		 * Callback to DictateActivity to update the progress of recording in VCVA mode.
		 * @param status true if a voice frame is detected, else false
		 * @param duration duration of the recorded dictation
		 */
		public void vcvaProgress(boolean status,long duration);
		
		/**
		 * Callback to DictateActivity when the file size of recording dictation exceeds maximum size limit (2 GB).
		 */
		public void onWriteLimitExceeded();
		
		/**
		 * Callback to DictateActivity when there is no sufficient disk space to save audio
		 */
		public void onLowMemoryAlert();
		
		/**
		 * Callback to DictateActivity when the user attempts to record while another recorder instance is running in the background
		 */
		public void onNotifyError();

		//public void enableRecordUI(boolean state);
		
	}
	
	AudioRecordStateListener mActivityBinder;
	
	int mFileType = 0;
	private State	state;
	private	VCVAMODE	VCVAState;
	private long VCVAOffStateProgress=0;
	private long VCVAPauseStateProgress=0;
	
	private short nChannels;
	private int	sRate;
	private short bSamples;
	private int	bufferSize;
	private int	aSource;
	private int	aFormat;
	
	private int	framePeriod;
	private static SharedPreferences pref;
	RandomAccessFile rafOut;
	public long LoadSize;
	long dataLength=0;
    boolean inValidated=false;
    private static Context context=null;
    
    int read_count=0;
    boolean newDictation=false;
    
    boolean isOncePaused=false;
    boolean isInsert=false;
    String target_file;
    
    boolean isMemoryAvailableToWrite=true;			
    long availableSpaceInBytes=0;
    StatFs stat;
    
	long total_audio_length;
	long actual_audio_length=0;
	long pause_position_in_bytes=0;
	boolean mRecorderReadOnce=false;
	static boolean drawWavGraphOnce=true;;
	long total_duration_in_ms=0;
	
	int read_error_count = 0;
	
	/**
	 * This method is used to get a handler to DMAudioRecorder
	 * @param context context of called activity
	 * @return returns a handler to a AudioRecord instance
	 */
    public static DMAudioRecorder getHandler(Context context)
	{
		DMAudioRecorder.context=context;
		DMAudioRecorder handler = null;
		handler = new DMAudioRecorder(AudioSource.DEFAULT,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
		
		pref=PreferenceManager.getDefaultSharedPreferences(context);
		mMicrosensitivity=pref.getInt(context.getResources().getString(R.string.mic_sensitivity_key), 100);
		mMicrosensitivity=mMicrosensitivity/100;
		//System.out.println("Sensitivity coefficient "+mMicrosensitivity);
		return handler;
	}
    
    /**
     * This method is used to get the current state of recorder
     * @return returns the current state of recorder
     */
	public State getState(){
		return state;
	}
	
	/**
	 * Thread to handle recording in VCVA mode
	 */
	Thread VCVAHandler=new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			byte[] buffer;
			short[] sBuffer;
			long space_required=0;
			
			while(state==State.REC){
				if (!DictateActivity.isInsertionProcessRunning) {
					stat.restat(Environment.getExternalStorageDirectory().getPath());
					availableSpaceInBytes = (long) stat.getAvailableBlocks()
							* (long) stat.getBlockSize();
					
					if (isOncePaused) {
						try {
							//calculate the space required to perform insert or overwrite process
							if(isInsert){
								pause_position_in_bytes=getBytePosition(DictateActivity.pausePosition, total_duration_in_ms, actual_audio_length);
								if( pause_position_in_bytes > (actual_audio_length*THRESHOLD)){
									// space required to perform insertion when the pause position is in second half
									space_required=(actual_audio_length-pause_position_in_bytes)+(rafOut.length());
									
									availableSpaceInBytes = availableSpaceInBytes
											- (space_required + 1024); 
								}
								else{
									// space required to perform insertion when the pause position is in first half
									space_required=(actual_audio_length)+(rafOut.length());
									availableSpaceInBytes = availableSpaceInBytes
											- (space_required + 1024); 
								}
							}else{
								//space required to perform overwrite process
								availableSpaceInBytes = availableSpaceInBytes
										- (rafOut.length() + 1024); 
							}
							
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					
					read_count = 0;
					sBuffer = null;
					sBuffer = new short[(int) lCount];
					read_count = audioRecorder.read(sBuffer, 0, (int) lCount);
					
					/*
					 * 
					 */
					if ((read_count == AudioRecord.STATE_UNINITIALIZED
							|| read_count == AudioRecord.ERROR_INVALID_OPERATION || read_count == AudioRecord.ERROR_BAD_VALUE)
							&& !mRecorderReadOnce) {
						read_error_count++;
						//System.out.println("error count - "+read_error_count);
						if(read_error_count == 10){
							mActivityBinder.onNotifyError();
							return;
						}
						return;
					} else {
						mRecorderReadOnce = true;
						read_error_count = 0;
					}
					
					
					
					if (read_count < availableSpaceInBytes) {
						isMemoryAvailableToWrite = true;
					} else {
						isMemoryAvailableToWrite = false;
					}
					if (isVoicedFrame(sBuffer) || newDictation) {
						/*
						 * For a new dictation it will write all the audio data till a valid WAV file has been created (one second).
						 * After a valid WAV file has been created it'll save audio only if voiced frame is detected 
						 * and will discard silence.
						 * 
						 */
						
						if (mMicrosensitivity != 1) {
							for (int i = 0; i < read_count; i++) {

								int amplifiedSample = (int) ((double) sBuffer[i] * mMicrosensitivity);

								if (amplifiedSample > 32767) {
									amplifiedSample = 32767;
								} else if (amplifiedSample < -32768) {
									amplifiedSample = -32768;
								}

								sBuffer[i] = (short) amplifiedSample;
							}
						}

						VCVAOffStateProgress = 0;
						buffer = new byte[(int) lCount * 2];
						ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
								.asShortBuffer().put(sBuffer);

						dataLength += buffer.length;
						LoadSize += buffer.length;

						// for a new dictation, the audio is saved for atleast one second
						if (newDictation) {
							VCVAPauseStateProgress += buffer.length;
							if (getDurationInMS(VCVAPauseStateProgress) >= 500) {
								newDictation = false;
								VCVAPauseStateProgress = 0;
							}
						}
						// get the sample with highest amplitude in the current read buffer
						for (int i = 0; i < buffer.length / 2; i++) {
							// 16bit sample size
							short curSample = getShort(buffer[i * 2],
									buffer[i * 2 + 1]);
							if (curSample > cAmplitude) {
								cAmplitude = curSample;
							}
						}
						//update the progress of recording to DictateActivity
						mActivityBinder.updateProgress(true, cAmplitude,
								getDurationInMS(dataLength),
								getDurationInMS(LoadSize));
						
						cAmplitude = 0;				// reset to 0

						/*int pcmValue[] = new int[buffer.length];
						byte pcm[] = new byte[buffer.length];*/
						try {

							/*if (mMicrosensitivity != 1) {
								for (int i = 0; i < read_count * 2; i++) {

									pcmValue[i] = (int) ((int) buffer[i] * mMicrosensitivity);

									if (pcmValue[i] > 127) {
										pcmValue[i] = 127;
									}
									if (pcmValue[i] < -128) {
										pcmValue[i] = -128;
									}
									pcm[i] = (byte) pcmValue[i];
								}
							}*/
							if (isOncePaused) {
								if (DictateActivity.isInsert) {
									//insertion
									if (((new File(
											DictateActivity.getFilename())
											.length()
											+ rafOut.length() + read_count)
											/ (MAX_LIMIT) < LIMIT)
											&& isMemoryAvailableToWrite) {
										if (read_count > 0) {
											rafOut.write(buffer);
											inValidated = true;
										}
									} else {
										updateHeader(getAudioFile());
										inValidated = false;
										dataLength = 0;
										if (!isMemoryAvailableToWrite) {
											mActivityBinder.onLowMemoryAlert();
										} else {
											mActivityBinder.onWriteLimitExceeded();
										}
										break;
									}
								} else if (!DictateActivity.isInsert) {
									//overwrite
									if (((getBytePosition(DictateActivity.pausePosition, total_duration_in_ms, actual_audio_length)
											+ rafOut.length() + read_count) / (MAX_LIMIT)) < LIMIT
											&& isMemoryAvailableToWrite) {
										if (read_count > 0) {
											rafOut.write(buffer);
											inValidated = true;
										}
									} else {
										updateHeader(getAudioFile());
										inValidated = false;
										dataLength = 0;
										if (!isMemoryAvailableToWrite) {
											mActivityBinder.onLowMemoryAlert();
										} else {
											mActivityBinder.onWriteLimitExceeded();
										}
										break;
									}
								}

							} else {
								//normal recording
								if (((rafOut.length() + read_count) / (MAX_LIMIT)) < LIMIT
										&& isMemoryAvailableToWrite) {
									if (read_count > 0) {
										rafOut.write(buffer);
										inValidated = true;
									}
								} else {
									updateHeader(getAudioFile());
									inValidated = false;
									dataLength = 0;
									if (!isMemoryAvailableToWrite) {
										mActivityBinder.onLowMemoryAlert();
									} else {
										mActivityBinder.onWriteLimitExceeded();
									}
									break;
								}
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							//release the array variables
							//pcmDoub=null;
							/*pcmValue = null;
							pcm = null;*/
						}

					} else {
						
						// update graph when recorder has started and if VCVA doesn't detect voice frame
						if(getPlayBackFileSize()>0 && !inValidated && drawWavGraphOnce){
							mActivityBinder.updateGraph(getDurationInMS(LoadSize));
							DMAudioRecorder.drawWavGraphOnce=false;
						}
						VCVAOffStateProgress += read_count * 2; //buffer.length;
						if (inValidated) {
							//update audio file header and draw graph
							updateHeader(getAudioFile());
							mActivityBinder.updateGraph(getDurationInMS(LoadSize));
							inValidated = false;
							dataLength = 0;
						}
						
						// update the state to DictateActivity when VCVA doesn't detect voice frames.
						if (getDurationInMS(VCVAOffStateProgress) >= 500) {
							mActivityBinder.vcvaProgress(false,
									getDurationInMS(VCVAOffStateProgress));
							VCVAOffStateProgress = 0;
						}
					}
				}
			}
			return;
		}
	});
	
	/**
	 * Thread to handle normal recording 
	 */
	Thread PCMHandler=new Thread(new Runnable() {
		
		@Override
		public void run() {
			byte[] buffer;
			short[] sBuffer;
			total_audio_length=0;
			inValidated=false;
			long space_required=0;
			//mActivityBinder.enableRecordUI(true);
			while(state==State.REC){
				if (!DictateActivity.isInsertionProcessRunning) {
					stat.restat(Environment.getExternalStorageDirectory().getPath());
					availableSpaceInBytes = (long) stat.getAvailableBlocks()
							* (long) stat.getBlockSize();
					
					if (isOncePaused) {
						try {
							//calculate the space required to perform insert or overwrite process
							if(isInsert){
								pause_position_in_bytes=getBytePosition(DictateActivity.pausePosition, total_duration_in_ms, actual_audio_length);
								if( pause_position_in_bytes > (actual_audio_length*THRESHOLD)){
									// space required to perform insertion when the pause position is in second half
									space_required=(actual_audio_length-pause_position_in_bytes)+(rafOut.length());
									
									availableSpaceInBytes = availableSpaceInBytes
											- (space_required + 1024); 
								}
								else{
									// space required to perform insertion when the pause position is in first half
									space_required=(actual_audio_length)+(rafOut.length());
									availableSpaceInBytes = availableSpaceInBytes
											- (space_required + 1024); 
								}
							}else{
								//space required to perform overwrite process
								availableSpaceInBytes = availableSpaceInBytes
										- (rafOut.length() + 1024);
							}
							
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					read_count = 0;
					buffer = null;
					buffer = new byte[(int) lCount * 2];
					
					sBuffer = null;
					sBuffer = new short[(int) lCount];
					read_count = audioRecorder.read(sBuffer, 0, (int) lCount);
					//System.out.println("read_count - "+read_count);
					if ((read_count == AudioRecord.STATE_UNINITIALIZED
							|| read_count == AudioRecord.ERROR_INVALID_OPERATION || read_count == AudioRecord.ERROR_BAD_VALUE)
							&& !mRecorderReadOnce) {
						read_error_count++;
						//System.out.println("error count - "+read_error_count);
						if(read_error_count == 10){
							mActivityBinder.onNotifyError();
							return;
						}
						
					} else {
						mRecorderReadOnce = true;
						read_error_count = 0;
					}
					
					/*dataLength += read_count * 2;
					LoadSize += read_count * 2;*/
					
					if (mMicrosensitivity != 1) {
						for (int i = 0; i < read_count; i++) {

							int amplifiedSample = (int) ((double) sBuffer[i] * mMicrosensitivity);
							/*if(amplifiedSample > 25000){
								System.out.println("multiplied value - "+amplifiedSample);
							}*/
							if (amplifiedSample > 32767) {
								amplifiedSample = 32767;
							} else if (amplifiedSample < -32768) {
								amplifiedSample = -32768;
							}
							
							sBuffer[i] = (short) amplifiedSample;
							
						}
					}
					ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
							.asShortBuffer().put(sBuffer);
					
					dataLength += buffer.length;
					LoadSize += buffer.length;
					
					// get the sample with highest amplitude in the current read buffer
					for (int i = 0; i < read_count / 2; i++) {
						// 16bit sample size
						short curSample = getShort(buffer[i * 2],
								buffer[i * 2 + 1]);
						if (curSample > cAmplitude) {
							cAmplitude = curSample;
						}
					}
					//update the progress of recording to DictateActivity
					mActivityBinder.updateProgress(true, cAmplitude,
							getDurationInMS(dataLength),
							getDurationInMS(LoadSize));
					//System.out.println("rec cur time"+getDurationInMS(dataLength) + "total duration "+getDurationInMS(LoadSize));
					cAmplitude = 0;				// reset to 0
					
					if (read_count < availableSpaceInBytes) {
						isMemoryAvailableToWrite = true;
					} else {
						isMemoryAvailableToWrite = false;
					}
					
					try {
						
						if (isOncePaused) {
							if (DictateActivity.isInsert) {
								//insertion
								if (((new File(DictateActivity.getFilename())
										.length() + rafOut.length() + read_count)
										/ (MAX_LIMIT) < LIMIT)
										&& isMemoryAvailableToWrite) {
									if (read_count > 0) {
										rafOut.write(buffer);
										inValidated = true;
									}
								} else {
									updateHeader(getAudioFile());
									inValidated = false;
									dataLength = 0;
									if (!isMemoryAvailableToWrite) {
										mActivityBinder.onLowMemoryAlert();
									} else {
										mActivityBinder.onWriteLimitExceeded();
									}
									break;
								}
							} else if (!DictateActivity.isInsert) { 
								//overwrite
								if (((getBytePosition(DictateActivity.pausePosition, total_duration_in_ms, actual_audio_length)
										+ rafOut.length() + read_count) / (MAX_LIMIT)) < LIMIT
										&& isMemoryAvailableToWrite) {
									if (read_count > 0) {
										rafOut.write(buffer);
										inValidated = true;
									}
								} else {
									updateHeader(getAudioFile());
									inValidated = false;
									dataLength = 0;
									if (!isMemoryAvailableToWrite) {
										mActivityBinder.onLowMemoryAlert();
									} else {
										mActivityBinder.onWriteLimitExceeded();
									}
									break;
								}
							}

						} else {
							// normal recording
							if ((rafOut.length() + read_count) / (MAX_LIMIT) < LIMIT
									&& isMemoryAvailableToWrite) {
								if (read_count > 0) {
									rafOut.write(buffer);
									inValidated = true;
								}
							} else {
								updateHeader(getAudioFile());
								inValidated = false;
								dataLength = 0;
								if (!isMemoryAvailableToWrite) {
									mActivityBinder.onLowMemoryAlert();
								} else {
									mActivityBinder.onWriteLimitExceeded();
								}
								break;
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						//release the array variables
					}
				}
    		}
			if(inValidated){
				//update wav header
				updateHeader(getAudioFile());
				inValidated=false;
				dataLength=0;
			}
			return;
		}
	});
	
	/**
	 * This method calculate the total duration in milli seconds
	 * @param length length of the audio file
	 * @return duration of audio file in milliseconds
	 */
	public long getDurationInMS(long length){
		return (long)(((float)(length/32000))*1000);
	}
	public double getVCVAOFFDurationInMS(long length){
		return (double)(((float)(length/32)));
	}
	
	/**
	 * This method reset template file after each insertion or overwrite process
	 */
	public void resetTempFile(){
		
		if(mFileType==DMAudioRecorder.FILE_TEMPLATE){
				closeFileWriter();
				createFileWriter();
				createWAVHeader(SAMPLE_RATE,BYTE_RATE,NUMBER_OF_CHANNELS,BITS_PER_SAMPLE,0);
		}
	}
	
	/**
	 * class constructor which initializes an instance of AudioRecord with the given configurations
	 * @param audioSource the source through which the audio is recorded
	 * @param sampleRate sample rate at which the audio is recorded (16 kHz)
	 * @param channelConfig audio channel (MONO)
	 * @param audioFormat format of audio to record (PCM)
	 */
	public DMAudioRecorder(int audioSource, int sampleRate, int channelConfig, int audioFormat){
		stat = new StatFs(Environment.getExternalStorageDirectory().getPath());	// Avaialble Memory Calculation
		
		if(audioRecorder!=null){
			audioRecorder.release();
			audioRecorder=null;
		}
		
		try{
			if (audioFormat == AudioFormat.ENCODING_PCM_16BIT){
				bSamples = 16;
			}else{
				bSamples = 8;
			}
			if (channelConfig == AudioFormat.CHANNEL_IN_MONO){
				nChannels = 1;
			}else{
				nChannels = 2;
			}
			
			aSource = audioSource;
			sRate   = sampleRate;
			aFormat = audioFormat;
			framePeriod = sampleRate * TIMER_INTERVAL / 1000;
			
			bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
			
			//release audio recorder before creating new instance
			
			
			audioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
			
			//audioRecorder.setRecordPositionUpdateListener(updateListener);
			//audioRecorder.setPositionNotificationPeriod(framePeriod);
			//if(audioRecorder.getState() == AudioRecord.STATE_INITIALIZED){
				state = State.INIT;
			//}
			}catch (IllegalArgumentException e) {
				Log.e(DMAudioRecorder.class.getName(), "Illegal Argument exception");
			}catch (Exception e){
				Log.e(DMAudioRecorder.class.getName(), "Unknown error occured while initializing recording");
				state = State.ERR;
			}
		
	}
	/**
	 * This method is called to get the highest amplitude sample recorded
	 * @return sample with highest amplitude
	 */
	public int getMaxAmplitude(){ 
		int result = cAmplitude; 
		cAmplitude = 0;
		return result; 
	} 
	
	/**
	 * prepare the AudioRecord instance before the recorder starts recording
	 */
	public void prepare(){
		if(VCVAState==VCVAMODE.ENABLED){
			// initialize VCVA
			initVCVA();
		}else{
			// set the number of samples to read
			setSamplesPerFrame();
		}
		try{
			if (state == State.INIT){
				
				if ((audioRecorder.getState() == AudioRecord.STATE_INITIALIZED)){
					state = State.RDY;
				}else{
					if(audioRecorder!=null){
						audioRecorder.release();
						//audioRecorder=null;
					}
					Log.e(DMAudioRecorder.class.getName(), "prepare error #1");
					state = State.ERR;
				}
				
			}else{
				Log.e(DMAudioRecorder.class.getName(), "prepare error #2");
				if(audioRecorder!=null){
					audioRecorder.release();
					//audioRecorder=null;
				}
				state = State.ERR;
			}
		}catch(Exception e){
			if (e.getMessage() != null){
				Log.e(DMAudioRecorder.class.getName(), "prepare error #3 : "+e.getMessage());
			}else{
				Log.e(DMAudioRecorder.class.getName(), " prepare error #4");
			}
			state = State.ERR;
		}
	}
	
	/**
	 * set the path of audio file to which the recorded audio is saved
	 * @param argPath
	 */
	public void setAudioFile(String argPath){
		try{
			if (state == State.INIT){
				//Log.e(DMAudioRecorder.class.getName(), "setting audio file - "+argPath);
				audioPath = argPath;
			}
		}
		catch (Exception e){
			if (e.getMessage() != null){
				//Log.e(DMAudioRecorder.class.getName(), e.getMessage());
			}
			
			state = State.ERR;
		}
	}
	
	/**
	 * release the current AudioRecord instance
	 */
	public void release(){
		if (state == State.REC){
			stop();
		}else{
			if ((state == State.RDY) ){
			}
		}
		if (audioRecorder != null){
			//Log.e(DMAudioRecorder.class.getName(), "releasing recorder");
			audioRecorder.release();
		}
		
	}
	
	/**
	 * reset the current AudioRecord instance
	 */
	public void reset()
	{
		try{
			if (state != State.ERR){
				release();
				audioRecorder = new AudioRecord(aSource, sRate, nChannels, aFormat, bufferSize);
				state = State.INIT;
			}
		}catch (Exception e){
			//Log.e(DMAudioRecorder.class.getName(), e.getMessage());
			state = State.ERR;
		}
	}
	
	/**
	 * starts the recorder if it is properly initialized and is ready to record.
	 */
	public void start(){
		if (state == State.RDY){
			// Log.e(DMAudioRecorder.class.getName(), "starting recorder");
			availableSpaceInBytes = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
			
				LoadSize = 0;
				try{
					audioRecorder.startRecording();
					mRecorderReadOnce=false;
					
					state = State.REC;
					createFileWriter();
					if(VCVAState==VCVAMODE.ENABLED){
						VCVAHandler.start();
					}else{
						PCMHandler.start();
					}
				}catch(IllegalStateException ise){
					Log.e(DMAudioRecorder.class.getName(), "start illegal error");
				}
				
		}
		else{
			if(audioRecorder!=null){
				audioRecorder.release();
				//audioRecorder=null;
			}
			mActivityBinder.onNotifyError();
			state = State.ERR;
		}
	}
	
	/**
	 * stop the recorder if it is in recording mode
	 */
	public void stop(){
		//close file stream (fOut)
		
		if (state == State.REC){
			//Log.e(DMAudioRecorder.class.getName(), "stoping recorder");
			audioRecorder.stop();
			state = State.STOP;
			updateHeader(getAudioFile());
			if(audioRecorder!=null){
				audioRecorder.release();
				//audioRecorder=null;
			}
			
		}else{
			if(audioRecorder!=null){
				audioRecorder.release();
				//audioRecorder=null;
			}
			state = State.ERR;
		}
	}
	
	/**
	 * close the file writer which is opened to write the recorded audio buffer to audio file
	 */
	public void closeFileWriter(){
		if(rafOut!=null){
			try {
				rafOut.close();
				rafOut=null;
				if(mFileType==DMAudioRecorder.FILE_TEMPLATE){
					new File(getAudioFile()).delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * used to set the audio file name and type of recording file. 
	 * @param filename name of audio file
	 * @param type type of recording file. example: EXISTING or TEMPLATE 
	 */
	public void setFile(String filename,int type){
		audioPath=filename;
		mFileType=type;
	}
	
	/**
	 * 
	 * @return returns the name of audio file
	 */
	public String getAudioFile(){
		 return audioPath;
	}
	/**
	 * create a file writer to the currently set audio file
	 */
	public void createFileWriter(){
		try {
			rafOut=null;
			rafOut=new RandomAccessFile(getAudioFile(), "rw");
			if(mFileType==FILE_EXISTING){
				LoadSize=rafOut.length();
				if(rafOut.length()>0){
					rafOut.seek(rafOut.length());
				}else{
					newDictation=true;
					VCVAPauseStateProgress=0;
					createWAVHeader(SAMPLE_RATE,BYTE_RATE,NUMBER_OF_CHANNELS,BITS_PER_SAMPLE,LoadSize);
				}
			}
			else{
				if(isOncePaused){
					// set the total duration as the duration of original file
					LoadSize+=actual_audio_length;
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * create and write wav header to the audio file
	 * @param sRate sample rate at which the audio is recorded
	 * @param byteRate byte rate of audio recorded
	 * @param nChannels type of channel used
	 * @param BitsPerSample bits per sample
	 * @param length length of audio file
	 */
	public void createWAVHeader(int sRate,int byteRate,int nChannels,int BitsPerSample,long length){
		 byte[] header = new byte[44];
		 header[0]  = 'R';  // RIFF/WAVE header
	     header[1]  = 'I';
	     header[2]  = 'F';
	     header[3]  = 'F'; 
	     
	     header[4]  = (byte) (length & 0xff);
	     header[5]  = (byte) ((length >> 8) & 0xff);
	     header[6]  = (byte) ((length >> 16) & 0xff);
	     header[7]  = (byte) ((length >> 24) & 0xff);
	     
	     header[8]  = 'W';
	     header[9]  = 'A';
	     header[10] = 'V';
	     header[11] = 'E';
	     header[12] = 'f';  // 'fmt ' chunk
	     header[13] = 'm';
	     header[14] = 't';
	     header[15] = ' ';
	     
	     header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
	     header[17] = 0;
	     header[18] = 0;
	     header[19] = 0;
	     header[20] = 1;  // format = 1
	     header[21] = 0;
	     header[22] = (byte) nChannels;
	     header[23] = 0;
	     header[24] = (byte) (sRate & 0xff);
	     header[25] = (byte) ((sRate >> 8) & 0xff);
	     header[26] = (byte) ((sRate >> 16) & 0xff);
	     header[27] = (byte) ((sRate >> 24) & 0xff);
	     header[28] = (byte) (byteRate & 0xff);
	     header[29] = (byte) ((byteRate >> 8) & 0xff);
	     header[30] = (byte) ((byteRate >> 16) & 0xff);
	     header[31] = (byte) ((byteRate >> 24) & 0xff);
	     header[32] = (byte) (2 * 16 / 8);  // block align
	     header[33] = 0;
	     header[34] = (byte) BitsPerSample;  // bits per sample
	     header[35] = 0;
	     header[36] = 'd';
	     header[37] = 'a';
	     header[38] = 't';
	     header[39] = 'a';
	     
	     header[40] = (byte) (length & 0xff);
	     header[41] = (byte) ((length >> 8) & 0xff);
	     header[42] = (byte) ((length >> 16) & 0xff);
	     header[43] = (byte) ((length >> 24) & 0xff);
	     
	     try {
	    	 rafOut.write(header, 0, 44);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * updates the total length of audio to the wav header
	 * @param inFile the path of audio file whose header is updated
	 */
	public void updateHeader(String inFile){
		 
		long totalAudioLen = 0;
	    long totalDataLen =0;		// totalAudioLen + 36
	  	FileInputStream in = null;
	  	try
	  	{
	  	in = new FileInputStream(inFile);
	  	totalAudioLen = (in.getChannel().size()-44);
	  	
	  	int reminder=(int) (totalAudioLen%2);
	  	if(reminder>0){
	  		totalAudioLen=totalAudioLen-reminder;
	  	}
	  	
	  	totalDataLen = (totalAudioLen) + 36;
	    
	      RandomAccessFile invFile = new RandomAccessFile(inFile, "rw");
	      invFile.seek(4);
	      invFile.write((byte) (totalDataLen & 0xff));
	      invFile.write((byte) ((totalDataLen >> 8) & 0xff));
	      invFile.write((byte) ((totalDataLen >> 16) & 0xff));
	      invFile.write((byte) ((totalDataLen >> 24) & 0xff));
	      invFile.seek(40);
	      invFile.write((byte) (totalAudioLen & 0xff));
	      invFile.write((byte) ((totalAudioLen >> 8) & 0xff));
	      invFile.write((byte) ((totalAudioLen >> 16) & 0xff));
	      invFile.write((byte) ((totalAudioLen >> 24) & 0xff));
	      invFile.close();
	  	}catch (Exception e) {
	  		// TODO: handle exception
	  	}
	  }
	private short getShort(byte argB1, byte argB2){ 
		return (short)(argB1 | (argB2 << 8));
	} 
	
	/**
	 * bind the recorder with the activity
	 * @param activity called activity
	 */
	public void bindActivity(Activity activity){
		mActivityBinder=(AudioRecordStateListener)activity;
	}
	
	/**
	 * Initializes VCVA by calling the native method
	 */
	public void initVCVA(){
		lCount=init(SAMPLE_RATE,VCVALevel);
	}
	
	/**
	 * number of samples read by the audio recorder
	 */
	public void setSamplesPerFrame(){
		lCount=(long)Math.ceil((double)(0.03*SAMPLE_RATE));
	}
	
	/**
	 * set the vcva state if vcva is turned on
	 * @param enabled state of vcva
	 * @param level level at which the vcva detects the audio
	 */
	public void setVCVAState(boolean enabled,int level){
		if(enabled){
			VCVAState=VCVAMODE.ENABLED;
			VCVALevel=level;
		}else{
			VCVAState=VCVAMODE.DISABLED;
		}
	}
	/**
	 * This method is used to set the current state of dictation.
	 * @param pause_state
	 * @param isInsert
	 * @param filename
	 * @param file_length
	 * @param pause_position
	 * @param total_duration_ms
	 */
	public void setAudioState(boolean pause_state,boolean isInsert,String filename,long file_length,long pause_position,long total_duration_ms){
		this.isOncePaused=pause_state;
		this.isInsert=isInsert;
		this.target_file=filename;
		this.actual_audio_length=file_length;
		this.pause_position_in_bytes=pause_position;
		this.total_duration_in_ms=total_duration_ms;
	}
	

	/**
	 * This method is used to get the byte position for the respective duration in milli seconds
	 * @param ms current pause position in milli seconds
	 * @param totaldur total duration of the audio file
	 * @param fileSize total length of the audio file
	 * @return returns the position in bytes for the pause position given
	 */
	public long getBytePosition(long ms, long totaldur, long fileSize) {
		long bytePos = 0;
		try {
			bytePos = ((fileSize * ms) / totaldur) + 44;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bytePos;
	}
	
	/**
	 * This method is called to get the total size of WAV file.
	 * @return returns the total size of audio file
	 */
	public long getPlayBackFileSize() {
		File playFile;
		if(isOncePaused){
			playFile = new File(target_file);
		}else{
			playFile = new File(getAudioFile());
		}
		if (playFile.exists()) {
			return (playFile.length() - 44);
		} else {
			return -1; // no file exists
		}
	}
	
	/**
	 * This method reset the recording mode. 
	 * It is used when the recording mode has to be changed from insert or overwrite to append mode.
	 * @param filename name of audio file
	 * @param type type of audio being recorded
	 */
	public void resetRecordingMode(String filename,int type){
		audioPath=filename;
		mFileType=type;
		try {
			if(rafOut!=null){
				rafOut.close();
				rafOut=null;
			}
			rafOut=new RandomAccessFile(getAudioFile(), "rw");
			if(mFileType==FILE_EXISTING){
				LoadSize=rafOut.length();
				if(rafOut.length()>0){
					rafOut.seek(rafOut.length());
				}else{
					newDictation=true;
					VCVAPauseStateProgress=0;
					createWAVHeader(SAMPLE_RATE,BYTE_RATE,NUMBER_OF_CHANNELS,BITS_PER_SAMPLE,LoadSize);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
