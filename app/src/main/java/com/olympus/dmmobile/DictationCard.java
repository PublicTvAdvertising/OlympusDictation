package com.olympus.dmmobile;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * DictationCard class is used to keep the dictation details together.
 * A DictationCard object holds each and every details of a dictation recorded.
 * @version 1.0.1
 */
public class DictationCard implements Parcelable{
	
	private int mDictationId = -1;
	private String mDictationName="";	
	private String mWorktype="";
	private String mAuthor="";
	private int mPriority = 0 ;
	private long mDuration = 0;
	private String mComments = "";
	private int mStatus = -1;
	private String mCreatedAt = "";
	private String createdAtTime = "";
	private int isThumbnailAvailable = 0;
	private String thumbnailType = "";
	private String mRecStartDate = "";
	private String mRecStartTime = "";
	private String mRecEndDate = "";
	private String mRecEndTime = "";
	private int mJobNumber = 0;
	private int mSequenceNumber = 0;
	private int isConverted = 0;
	private int mDeliveryMethod = 1;
	private int mDssCompressionMode = -1;
	private String mDssEncryptionPassword = "";
	private int mDssVersion = -1;
	private int isActive = 0;
	private int isFlashAir = 0;
	private String mSentDate = "";
	private String mSentTime = "";
	private boolean mDictChecked = false;
	private ArrayList<FilesCard> mFilesList=null;
	private int groupId = 0;
	private int mainStatus = -1;
	private int queryPriority=-1;
	private int isResend = 0;

	private int splitInternalStatus = 0;

	private int EncryptionVersion = -1;
	private int Encryption = 0 ;
	private int isFileSplittable = 0;
	private int dssFormat = -1;
	private String mDictFileName="";
	
	public int getQueryPriority() {
		return queryPriority;
	}
	public void setQueryPriority(int queryPriority) {
		this.queryPriority = queryPriority;
	}
	public int getMainStatus() {
		return mainStatus;
	}
	public void setMainStatus(int mainStatus) {
		this.mainStatus = mainStatus;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public int getSplitInternalStatus() {
		return splitInternalStatus;
	}
	public void setSplitInternalStatus(int splitInternalStatus) {
		this.splitInternalStatus = splitInternalStatus;
	}
	
	public String getDictFileName() {
		return mDictFileName;
	}
	public void setDictFileName(String mDictFileName) {
		this.mDictFileName = mDictFileName;
	}
	public String getCreatedAtTime() {
		return createdAtTime;
	}
	public void setCreatedAtTime(String createdAtTime) {
		this.createdAtTime = createdAtTime;
	}
	public String getThumbnailType() {
		return thumbnailType;
	}
	public void setThumbnailType(String thumbnailType) {
		this.thumbnailType = thumbnailType;
	}
	public String getRecStartTime() {
		return mRecStartTime;
	}
	public void setRecStartTime(String mRecStartTime) {
		this.mRecStartTime = mRecStartTime;
	}
	public String getRecEndTime() {
		return mRecEndTime;
	}
	public void setRecEndTime(String mRecEndTime) {
		this.mRecEndTime = mRecEndTime;
	}
	public String getSentTime() {
		return mSentTime;
	}
	public void setSentTime(String mSentTime) {
		this.mSentTime = mSentTime;
	}

	
	
	
	public int getDssFormat() {
		return dssFormat;
	}
	public void setDssFormat(int dssFormat) {
		this.dssFormat = dssFormat;
	}
	public int getEncryptionVersion() {
		return EncryptionVersion;
	}
	public void setEncryptionVersion(int encryptionVersion) {
		EncryptionVersion = encryptionVersion;
	}
	public int isEncryption() {
		return Encryption;
	}
	public void setEncryption(int encryption) {
		Encryption = encryption;
	}
	
	/*public String getSourceFile() {
		return sourceFile;
	}
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}
	public String getDestFile() {
		return destFile;
	}
	public void setDestFile(String destFile) {
		this.destFile = destFile;
	}*/
	public int isFileSplittable() {
		return isFileSplittable;
	}
	public void setFileSplittable(int isFileSplittable) {
		this.isFileSplittable = isFileSplittable;
	}
	public ArrayList<FilesCard> getFilesList() {
		return mFilesList;
	}
	public void setFilesList(ArrayList<FilesCard> mFilesList) {
		this.mFilesList = mFilesList;
	}
	public int getDictationId() {
		return mDictationId;
	}
	public void setDictationId(int mDictationId) {
		this.mDictationId = mDictationId;
	}
	public String getAuthor() {
		return mAuthor;
	}
	public void setAuthor(String mAuthor) {
		this.mAuthor = mAuthor;
	}
	public String getCreatedAt() {
		return mCreatedAt;
	}
	public void setCreatedAt(String mCreatedAt) {
		this.mCreatedAt = mCreatedAt;
	}
	public int getIsThumbnailAvailable() {
		return isThumbnailAvailable;
	}
	public void setIsThumbnailAvailable(int isThumbnailAvailable) {
		this.isThumbnailAvailable = isThumbnailAvailable;
	}
	public String getRecStartDate() {
		return mRecStartDate;
	}
	public void setRecStartDate(String mRecStartDate) {
		this.mRecStartDate = mRecStartDate;
	}
	public String getRecEndDate() {
		return mRecEndDate;
	}
	public void setRecEndDate(String mRecEndDate) {
		this.mRecEndDate = mRecEndDate;
	}
	public int getJobNumber() {
		return mJobNumber;
	}
	public void setJobNumber(int mJobNumber) {
		this.mJobNumber = mJobNumber;
	}
	public int getSequenceNumber() {
		return mSequenceNumber;
	}
	public void setSequenceNumber(int mSequenceNumber) {
		this.mSequenceNumber = mSequenceNumber;
	}
	public int getIsConverted() {
		return isConverted;
	}
	public void setIsConverted(int isConverted) {
		this.isConverted = isConverted;
	}
	public int getDeliveryMethod() {
		return mDeliveryMethod;
	}
	public void setDeliveryMethod(int mDeliveryMethod) {
		this.mDeliveryMethod = mDeliveryMethod;
	}
	public int getDssCompressionMode() {
		return mDssCompressionMode;
	}
	public void setDssCompressionMode(int mDssCompressionMode) {
		this.mDssCompressionMode = mDssCompressionMode;
	}
	public String getDssEncryptionPassword() {
		return mDssEncryptionPassword;
	}
	public void setDssEncryptionPassword(String mDssEncryptionPassword) {
		this.mDssEncryptionPassword = mDssEncryptionPassword;
	}
	public int getDssVersion() {
		return mDssVersion;
	}
	public void setDssVersion(int mDssVersion) {
		this.mDssVersion = mDssVersion;
	}
	public int getIsActive() {
		return isActive;
	}
	public void setIsActive(int isActive) {
		this.isActive = isActive;
	}
	public int getIsFlashAir() {
		return isFlashAir;
	}
	public void setIsFlashAir(int isFlashAir) {
		this.isFlashAir = isFlashAir;
	}
	public String getSentDate() {
		return mSentDate;
	}
	public void setSentDate(String mSentDate) {
		this.mSentDate = mSentDate;
	}
	public long getDuration() {
		return mDuration;
	}
	public void setDuration(long mDuration) {
		this.mDuration = mDuration;
	}
	public String getComments() {
		return mComments;
	}
	public void setComments(String mComments) {
		this.mComments = mComments;
	}
	public int getStatus() {
		return mStatus;
	}
	public void setStatus(int mStatus) {
		this.mStatus = mStatus;
	}
	public int getPriority() {
		return mPriority;
	}
	public boolean isDictChecked() {
		return mDictChecked;
	}
	public void setDictChecked(boolean mDictChecked) {
		this.mDictChecked = mDictChecked;
	}
	public void setPriority(int mPriority) {
		this.mPriority = mPriority;
	}
	public String getDictationName(){
		return mDictationName;
	}
	public void setDictationName(String mDictationName){
		this.mDictationName = mDictationName;
	}
	public String getWorktype(){
		return mWorktype;
	}
	public void setWorktype(String mWorktype){
		this.mWorktype = mWorktype;
	}
	
	public int isResend() {
		return isResend;
	}
	public void setIsResend(int isResend) {
		this.isResend = isResend;
	}
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mDictationId);
		dest.writeString(mDictationName);
		dest.writeString(mDictFileName);
		dest.writeString(mWorktype);
		dest.writeInt(mPriority);
		dest.writeLong(mDuration);
		dest.writeString(mComments);
		dest.writeInt(mStatus);
		dest.writeString(mAuthor);
		dest.writeString(mCreatedAt);
		dest.writeString(createdAtTime);
		dest.writeString(mRecStartDate);
		dest.writeString(mRecStartTime);
		dest.writeString(mRecEndDate);
		dest.writeString(mRecEndTime);
		dest.writeString(mSentDate);
		dest.writeString(mSentTime);
		dest.writeString(mDssEncryptionPassword);
		dest.writeInt(isThumbnailAvailable);
		dest.writeString(thumbnailType);
		dest.writeInt(mJobNumber);
		dest.writeInt(mSequenceNumber);
		dest.writeInt(isConverted);
		dest.writeInt(mDeliveryMethod);
		dest.writeInt(mDssCompressionMode);
		dest.writeInt(mDssVersion);
		dest.writeInt(isActive);
		dest.writeInt(isFlashAir);
		dest.writeInt(EncryptionVersion);
		dest.writeInt(isFileSplittable);
		dest.writeInt(groupId);
		dest.writeInt(splitInternalStatus);
		dest.writeInt(Encryption);
		dest.writeInt(mainStatus);
		dest.writeInt(queryPriority);
		dest.writeTypedList(mFilesList);
	}
	
	public static final Parcelable.Creator<DictationCard> CREATOR 
			=new Parcelable.Creator<DictationCard>() {

				@Override
				public DictationCard createFromParcel(Parcel source) {
					
					return new DictationCard(source);
				}

				@Override
				public DictationCard[] newArray(int size) {
					return new DictationCard[size];
				}
			};
	
	@SuppressWarnings("unchecked")
	public DictationCard (Parcel source){
		mDictationId = source.readInt();
		mDictationName = source.readString();
		mDictFileName = source.readString();
		mWorktype = source.readString();
		mPriority = source.readInt();
		mDuration = source.readLong();
		mComments = source.readString();
		mStatus = source.readInt();
		mAuthor = source.readString();
		mCreatedAt = source.readString();
		createdAtTime = source.readString();
		mRecStartDate = source.readString();
		mRecStartTime = source.readString();
		mRecEndDate = source.readString();
		mRecEndTime = source.readString();
		mSentDate = source.readString();
		mSentTime = source.readString();
		mDssEncryptionPassword = source.readString();
		isThumbnailAvailable = source.readInt();
		thumbnailType = source.readString();
		mJobNumber = source.readInt();
		mSequenceNumber = source.readInt();
		isConverted = source.readInt();
		mDeliveryMethod = source.readInt();
		mDssCompressionMode = source.readInt();
		mDssVersion = source.readInt();
		isActive = source.readInt();
		isFlashAir = source.readInt();
		EncryptionVersion = source.readInt();
		isFileSplittable = source.readInt();
		groupId = source.readInt();
		splitInternalStatus = source.readInt();
		Encryption = source.readInt();
		mainStatus = source.readInt();
		queryPriority = source.readInt();
		mFilesList = source.readArrayList(FilesCard.class.getClassLoader());
	}
	public DictationCard(){}
	
}
