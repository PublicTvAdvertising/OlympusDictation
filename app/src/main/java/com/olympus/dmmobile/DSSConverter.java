package com.olympus.dmmobile;

import java.io.File;
import java.util.ArrayList;
import android.os.Environment;
import android.os.StatFs;

/**
 * DSSConverter is a helper class for converting WAV to DSS format.
 * This class communicate with native library 'native_lib' to perform DSS conversion.
 * @version 1.0.1
 * 
 */
public class DSSConverter {
	
	static{
		System.loadLibrary("native_lib");
	}
	
	/**
	 * native method which set the DSS properties, such as DSS format,DSS header,encryption type and convert WAV file to DSS using DSS Encoder library.
	 * @param jPriority 
	 * @param jEncryptionVersion 
	 * @param jEncryptionEnable 
	 * @param jPassword 
	 * @param jAuthorID 
	 * @param jWorktypeID 
	 * @param jJobNumber 
	 * @param jComment
	 * @param jRecStartDate
	 * @param jRecStartTime
	 * @param jRecEndDate
	 * @param jRecEndTime
	 * @param sourceFile
	 * @param destFile
	 * @param jDssFormat
	 * @param split
	 * @return return 0 if conversion succeeded , else return 1
	 */
	public native int doConvert(int jPriority,int jEncryptionVersion,boolean jEncryptionEnable,String jPassword,String jAuthorID,String jWorktypeID,
			String jJobNumber,String jComment,String jRecStartDate,String jRecStartTime,
			String jRecEndDate,String jRecEndTime,String sourceFile,String destFile,int jDssFormat,boolean split);
	
	/**
	 * native method used to set the start and end points to split the DSS file.
	 * @param from start point
	 * @param to end point
	 * 
	 */
	public native void setSplitPoints(long from,long to);
	
	private final double SIZE_LIMIT=23.0;		// email size limit  //23.0
	private final int SPLITTING=1;
	private final int LAST_SPLIT=0;
	private String BASE_DIR = DMApplication.DEFAULT_DIR+"/Dictations/";
	
	
	private boolean result=false;
	private long file_size;
	private double thumbnail_size=0;
	private double required_size=0;
	private long split_point=0;
	
	
	private long duration=0;
	
	//private long allocateMem=10*1024*1024;
	private long availableMegs;
	private long bytePosition=0;
	private ArrayList<FilesCard> mFilesCards=null;
	private FilesCard mFilesCard=null;
	private int resultcode=-1;
	private StringBuilder filename=null;
	private boolean isEnCrypt=false;
	private long temp=0;
	private File file=null;
	private long splitPointInMilliSecs=0;
	private long availableSpace=0;
	private StatFs stat=null;
	
	/**
	 * method used to get the length of WAV file
	 * @return length of WAV file
	 */
	private long getFileLength(){
		return file_size;
	}
	
	/**
	 * method to get the total duration of WAV file in milliseconds
	 * @return duration
	 */
	private long getDurationInMS(){
		duration=(file_size/32000)*1000;
		return duration;
	}
	
	/**
	 * method used to get the byte position of the given duration in the WAV file
	 * @param ms duration in milliseconds
	 * @return byte position of the given duration
	 */
	private long getBytePosition(long ms){
		getDurationInMS();
		long bytePos=((file_size-44)*ms)/duration;
		return (bytePos+44);
	}
	/**
	 * This method initiate the conversion process of a Dictation. 
	 * If the dictation is splittable, the split points are calculated and set to the converter.
	 * @param dCard DictationCard which consists of all the details about the dictation
	 * @return 0 if conversion is success, else 1
	 */
	public ArrayList<FilesCard> convert(DictationCard dCard){
		mFilesCards=new ArrayList<FilesCard>();
		if(dCard.getIsConverted()==0&&dCard.getIsFlashAir()==0)
		{
			mFilesCard=new FilesCard();
			BASE_DIR=BASE_DIR+dCard.getSequenceNumber()+"/";
			
			// check if there is sufficient free space to perform conversion process
			stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
			if(DMApplication.getExpectedDSSFileSize(dCard.getDssVersion(),BASE_DIR+dCard.getDictFileName())>=availableSpace)
				return null;
			
			isEnCrypt=false;
			if( dCard.isEncryption()==1)
				isEnCrypt=true;
			
			 if(dCard.isFileSplittable() == 1){
				 // split into multiple files if the dss file size exceeds 23 MB
				 if(getSplitPoint(obtainDssversion(dCard.getDssVersion()),dCard.getIsThumbnailAvailable(), BASE_DIR+dCard.getDictFileName()+".wav",BASE_DIR+dCard.getDictFileName()+".jpg") < getDurationInMS()){ 
					duration=getDurationInMS();
					temp=0;
					int i=0;	
					while(bytePosition<file_size){
						i++;
						availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
						if(23>=availableSpace)
							return null;
						filename=new StringBuilder(BASE_DIR);
						filename.append(dCard.getDictationName()+"_"+i+"."+DMApplication.getDssType(dCard.getDssVersion()));
						if(i==1){
							// reduce the size of image file only for the first split
							split_point+=getSplitPoint(obtainDssversion(dCard.getDssVersion()),dCard.getIsThumbnailAvailable(),BASE_DIR+dCard.getDictFileName()+".wav",BASE_DIR+dCard.getDictFileName()+".jpg"); //1
						}
						else{
							split_point+=getSplitPoint(obtainDssversion(dCard.getDssVersion()),0,BASE_DIR+dCard.getDictFileName()+".wav",null);
						}
						
						// if the next split point is greater than duration, the split point is set as duration
						if(split_point>duration){
							split_point=duration;
						}
						// get the byte position of the split point in the WAV file
						bytePosition=getBytePosition(split_point);
						
						if(temp%2 != 0){
							temp +=1;
						}
						setSplitPoints(temp,bytePosition);
						//System.out.println("==============================================================");
						//System.out.println("Split - #"+i);
						//System.out.println("Split points : start - "+temp+" end - "+bytePosition);
						resultcode=doConvert(dCard.getPriority(), dCard.getEncryptionVersion(),isEnCrypt, 
								dCard.getDssEncryptionPassword(), dCard.getAuthor(), dCard.getWorktype(), String.valueOf(dCard.getJobNumber()), 
								dCard.getComments(),DMApplication.getFormattedDate(dCard.getRecStartDate()), DMApplication.getFormattedTime(dCard.getRecStartDate()), 
								DMApplication.getFormattedDate(dCard.getRecEndDate()),DMApplication.getFormattedTime(dCard.getRecEndDate()),
								BASE_DIR+dCard.getDictFileName()+".wav", filename.toString(), obtainDssversion(dCard.getDssVersion()), true);
							
						temp=bytePosition;
						if(resultcode!=0)
							break;
						mFilesCard.setFileId(dCard.getDictationId());
						mFilesCard.setFileIndex(i-1);
						mFilesCard.setFileName(dCard.getDictationName()+"_"+i);
						mFilesCards.add(mFilesCard);
						mFilesCard=new FilesCard();
					}
				}else{
					// dss file size doesn't exceed 23 MB
					resultcode=doConvert(dCard.getPriority(), dCard.getEncryptionVersion(),isEnCrypt, 
							dCard.getDssEncryptionPassword(), dCard.getAuthor(), dCard.getWorktype(), String.valueOf(dCard.getJobNumber()), 
							dCard.getComments(), DMApplication.getFormattedDate(dCard.getRecStartDate()), DMApplication.getFormattedTime(dCard.getRecStartDate()), 
							DMApplication.getFormattedDate(dCard.getRecEndDate()),DMApplication.getFormattedTime(dCard.getRecEndDate()),BASE_DIR+dCard.getDictFileName()+".wav", BASE_DIR+
							dCard.getDictationName() +"."+ DMApplication.getDssType(dCard.getDssVersion()), obtainDssversion(dCard.getDssVersion()), false);
					mFilesCard.setFileId(dCard.getDictationId());
					mFilesCard.setFileIndex(0);
					mFilesCard.setFileName(dCard.getDictationName());
					mFilesCards.add(mFilesCard);
				}
			 }else{
				 // no need to split dss file
				resultcode=doConvert(dCard.getPriority(), dCard.getEncryptionVersion(),isEnCrypt, 
						dCard.getDssEncryptionPassword(), dCard.getAuthor(), dCard.getWorktype(), String.valueOf(dCard.getJobNumber()), 
						dCard.getComments(), DMApplication.getFormattedDate(dCard.getRecStartDate()), DMApplication.getFormattedTime(dCard.getRecStartDate()), 
						DMApplication.getFormattedDate(dCard.getRecEndDate()),DMApplication.getFormattedTime(dCard.getRecEndDate()),BASE_DIR+dCard.getDictFileName()+".wav", BASE_DIR+
						dCard.getDictationName()+"."+DMApplication.getDssType(dCard.getDssVersion()), obtainDssversion(dCard.getDssVersion()), false);
				mFilesCard.setFileId(dCard.getDictationId());
				mFilesCard.setFileIndex(0);
				mFilesCard.setFileName(dCard.getDictationName());
				mFilesCards.add(mFilesCard);
			 }
			 if(resultcode!=0)
				 mFilesCards=null;
		}
		dCard=null;
		return mFilesCards;
	}

	/**
	 * This method is used to get the split points for the respective DSS format after reducing the size of image file attached with
	 * the dictation.
	 * @param type type of DSS format
	 * @param thumbnail 1, if image is attached with the dictation
	 * @param audioFile path of WAV file
	 * @param imgFile path of image attached with the dictation
	 * @return duration in milliseconds at which the file needs to split
	 */
	private long getSplitPoint(int type,int thumbnail,String audioFile,String imgFile){
		splitPointInMilliSecs=0;
		thumbnail_size=0;
		file_size=0;
		
		file=new File(audioFile);
		if(file.exists()){
			file_size=file.length();
		}else{
			return 0;
		}
		if(thumbnail ==1&& imgFile!=null){
			file=new File(imgFile);
			if(file.exists()){
				thumbnail_size=file.length();
			}
			else{
				thumbnail_size=0;
				thumbnail=0;
			}
		}
		if(thumbnail==1)
			required_size=SIZE_LIMIT-(thumbnail_size/(1024*1024));
		else
			required_size=SIZE_LIMIT;
		
		switch(type){
		
			case DMApplication.DSSFORMAT_DSS_SP:
				splitPointInMilliSecs= (long) Math.ceil((888*(required_size*(Math.pow(2,20))-1024)/1536));
				break;
			
			case DMApplication.DSSFORMAT_DS2_SP:
				splitPointInMilliSecs= (long) Math.ceil((888*(required_size*(Math.pow(2,20))-1024)/1536));
				break;
			
			case DMApplication.DSSFORMAT_DS2_QP:
				splitPointInMilliSecs=(long)Math.ceil((4048*(required_size*(Math.pow(2,20))-1536)/14336));
				break;
			}
		return splitPointInMilliSecs;
	}
	
	private int obtainDssversion(int ver){
		switch (ver) {
		case 0:
			return 0;
		case 10:
			return 1;
		case 11:
			return 2;

		default:
			return 0;
		}
	}
}
