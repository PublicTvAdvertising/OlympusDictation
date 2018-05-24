package com.olympus.dmmobile;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * FilesCard class is used to keep the details of converted file of a dictation.
 * If there are multiple converted files for a dictation, details of each converted file represent a FilesCard
 * @version 1.0.1
 */
public class FilesCard implements Parcelable{

	private int fileId = 0;
	private int fileIndex=0;
	private int transferStatus=-1;
	private int retryCount=0;
	private long fileSize=0;
	private String fileName="";
	private String transferId="";
	private String jobNumber="";

	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	public int getFileId() {
		return fileId;
	}
	public void setFileId(int fileId) {
		this.fileId = fileId;
	}
	public int getFileIndex() {
		return fileIndex;
	}
	public void setFileIndex(int fileIndex) {
		this.fileIndex = fileIndex;
	}
	public int getTransferStatus() {
		return transferStatus;
	}
	public void setTransferStatus(int transferStatus) {
		this.transferStatus = transferStatus;
	}
//	public int isSplitUploading() {
//		return isSplitUploading;
//	}
//	public void setSplitUploading(int isSplitUploading) {
//		this.isSplitUploading = isSplitUploading;
//	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getTransferId() {
		return transferId;
	}
	public void setTransferId(String transferId) {
		this.transferId = transferId;
	}
	public String getJobNumber() {
		return jobNumber;
	}
	public void setJobNumber(String jobNumber) {
		this.jobNumber = jobNumber;
	}
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(fileId);
		dest.writeInt(fileIndex);
		dest.writeInt(transferStatus);
		dest.writeInt(retryCount);
		dest.writeLong(fileSize);
		dest.writeString(fileName);
		dest.writeString(transferId);
		dest.writeString(jobNumber);
	}
	public static final Parcelable.Creator<FilesCard> CREATOR 
				=new Parcelable.Creator<FilesCard>() {
		@Override
		public FilesCard createFromParcel(Parcel source) {
			
			return new FilesCard(source);
		}
		@Override
		public FilesCard[] newArray(int size) {
			// TODO Auto-generated method stub
			return new FilesCard[size];
		}
	};
	public FilesCard(Parcel source){
		fileId = source.readInt();
		fileIndex = source.readInt();
		transferStatus = source.readInt();
		retryCount = source.readInt();
		fileSize = source.readLong();
		fileName = source.readString();
		transferId = source.readString();
		jobNumber = source.readString();
	}
	public FilesCard(){}
}
