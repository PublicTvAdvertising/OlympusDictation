#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include <stdlib.h>
#include "DSSConverter/DssConverter.h"
#include <string.h>
#include "SimpleFileWriter.h"
#include "SimpleFileReader.h"
#include "64bit.h"



const char *m_InputFile;
const char *m_OutputFile;
const char *m_Password;
const char *m_AuthorID;
const char *m_WorktypeID;
const char *m_JobNumber;
const char *m_Comment;
const char *m_RecStartDate;
const char *m_RecStartTime;
const char *m_RecEndDate;
const char *m_RecEndTime;

int m_DssFormat;
int m_Priority;
int m_EncryptionVersion;
bool m_EncryptionEnable;

SimpleFileWriter *writer;

bool split_enabled=false;
unsigned long int start=0;
unsigned long int end=0;

FILE *fin;

enum {
	DSSFORMAT_DSS_SP = 0,
	DSSFORMAT_DS2_SP = 1,
	DSSFORMAT_DS2_QP = 2,
};

enum {
	DSS_ENCRYPTION_STANDARD = 0,
	DSS_ENCRYPTION_HIGH = 1,
};

enum {
	DSS_PRIORITY_NORMAL = 0,
	DSS_PRIORITY_HIGH = 1,
};


extern "C"
{
    JNIEXPORT void JNICALL Java_com_olympus_dmmobile_DSSConverter_setSplitPoints(JNIEnv * env, jobject obj,jlong start,jlong end);
    JNIEXPORT jint JNICALL Java_com_olympus_dmmobile_DSSConverter_doConvert(JNIEnv * env, jobject obj,jint jPriority,jint jEncryptionVersion,jboolean jEncryptionEnable,jstring jPassword,jstring jAuthorID,jstring jWorktypeID,jstring jJobNumber,jstring jComment,jstring jRecStartDate,jstring jRecStartTime,jstring jRecEndDate,jstring jRecEndTime,jstring sourceFile,jstring destFile,jint jDssFormat,jboolean split);
};

extern "C"
{
// Receive encoded data
	void outputCallback(char *buf, unsigned long size, unsigned long offset)
	{
		if(writer)
		{
			// write the encoded data to dss file
			writer->Write(buf, size, offset);
		}
	}
}

// set the start and end points for splitting the audio file
JNIEXPORT void JNICALL Java_com_olympus_dmmobile_DSSConverter_setSplitPoints(JNIEnv * env, jobject obj,jlong from,jlong to)
{
	start=from;
	::end=to;
}

// called from DSSConverter.java
// set the dss format, dss header , encryption and starts dss conversion
JNIEXPORT jint JNICALL Java_com_olympus_dmmobile_DSSConverter_doConvert(JNIEnv * env, jobject obj,jint jPriority,
		jint jEncryptionVersion,jboolean jEncryptionEnable,jstring jPassword,
		jstring jAuthorID,jstring jWorktypeID,jstring jJobNumber,jstring jComment,jstring jRecStartDate,
		jstring jRecStartTime,jstring jRecEndDate,jstring jRecEndTime,jstring sourceFile,
		jstring destFile,jint jDssFormat,jboolean split)
{
	split_enabled=split;

	m_Priority=jPriority;
	m_EncryptionVersion=jEncryptionVersion;
	m_EncryptionEnable=jEncryptionEnable;

	// java strings are converted to UTF-8 characters
	m_Password=env->GetStringUTFChars(jPassword,0);
	m_AuthorID=env->GetStringUTFChars(jAuthorID,0);
	m_WorktypeID=env->GetStringUTFChars(jWorktypeID,0);
	m_JobNumber=env->GetStringUTFChars(jJobNumber,0);
	m_Comment=env->GetStringUTFChars(jComment,0);
	m_RecStartDate=env->GetStringUTFChars(jRecStartDate,0);
	m_RecStartTime=env->GetStringUTFChars(jRecStartTime,0);
	m_RecEndDate=env->GetStringUTFChars(jRecEndDate,0);
	m_RecEndTime=env->GetStringUTFChars(jRecEndTime,0);

	m_InputFile=env->GetStringUTFChars(sourceFile,0);
	m_OutputFile=env->GetStringUTFChars(destFile,0);
	m_DssFormat=jDssFormat;


	SimpleFileReader *reader;
	reader=new SimpleFileReader();
	if(reader->Open((LPCTSTR)m_InputFile)==S_OK){
		// parse the header of wav file
		reader->parseHeader();
	}else{
		//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter", "cannot open input pcm file");
		return S_FALSE;
	}
	writer = new SimpleFileWriter();
	writer->Open((LPCTSTR)m_OutputFile);

	DssConverter *converter = new DssConverter();
	if(converter->SetOutputCallback(outputCallback)!=DssResult_Success){
		//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter", "cannot set output callback method");
		return S_FALSE;
	}

	// DSS Format
	{
		DSS_VERSION dss_version = VERSION_UNKNOWN;
		DSS_COMPRESSION_MODE mode = MODE_UNKNOWN;
		if(m_DssFormat == DSSFORMAT_DSS_SP)
		{
			dss_version = VERSION_DSS;
			mode = MODE_SP;
		}
		else if(m_DssFormat == DSSFORMAT_DS2_SP)
		{
			dss_version = VERSION_DSSPRO;
			mode = MODE_SP;
		}
		else if(m_DssFormat == DSSFORMAT_DS2_QP)
		{
			dss_version = VERSION_DSSPRO;
			mode = MODE_QP;
		}

		if(converter->SetCompressionModeAndWorkType(dss_version, mode,(char*)m_WorktypeID)!=DssResult_Success){
			//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter", "cannot set encryption method");
			return S_FALSE;
		}
	}
	// PCM Format
	{
		PcmFormat pcm;

		reader->GetPcmFormatInfo(pcm.channels, pcm.samplesPerSec, pcm.bitsPerSample);
		if(converter->SetInputPCMFormat(pcm)!=DssResult_Success){
			//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter", "cannot set input pcm format ");
			return S_FALSE;
		}
	}

	// Encryption
	{
		if((m_DssFormat != DSSFORMAT_DSS_SP) && m_EncryptionEnable && (strlen(m_Password) > 0)) //edited
		{
			DSS_ENCRYPTION_VERSION version = ENCRYPTION_NONE;
			if(m_EncryptionVersion == ENCRYPTION_STANDARD){
				version = ENCRYPTION_STANDARD;
				//__android_log_print(ANDROID_LOG_VERBOSE, "ENCRYPTION", "DSS STANDARD");
			}
			else{
				version = ENCRYPTION_HIGH;
				//__android_log_print(ANDROID_LOG_VERBOSE, "ENCRYPTION", "DSS HIGH");
			}

			if(converter->SetEncryption((const char*)m_Password, version)!=DssResult_Success){
				//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter", "cannot set encryption method");
				return S_FALSE;
			}

		}
	}

	// DSS Header
	{
		DssHeaderInfo header;
		::memset(&header, 0, sizeof(DssHeaderInfo));

		// AuthorID
		if(strlen(m_AuthorID) > 0)
		{
			strncpy(header.authorID, (const char*)m_AuthorID, AUTHORID_LENGTH);
		}
		// Priority
		if(m_Priority == DSS_PRIORITY_NORMAL)
		{
			header.priorityStatus = PRIORITY_NORMAL;
		}
		else
		{
			header.priorityStatus = PRIORITY_HIGH;
		}

		// WorktypeID
		if(strlen(m_WorktypeID) > 0)
		{
			strncpy(header.worktypeID, (const char*)m_WorktypeID, WORKTYPEID_LENGTH);
		}

		// Job Number
		if(strlen(m_JobNumber) > 0)
		{
			unsigned int jobNumber =(long)(m_JobNumber);
			header.jobNumber = jobNumber;
		}

		// Comment
		if(strlen(m_Comment) > 0)
		{
			strncpy(header.comment, (const char*)m_Comment, COMMENT_LENGTH);
		}

		// Rec Start Date/Time
		if(strlen(m_RecStartDate)> 0)
		{
			strncpy(header.recStart.recDate, (const char*)m_RecStartDate, DATETIME_LENGTH);
		}
		if(strlen(m_RecStartTime) > 0)
		{
			strncpy(header.recStart.recTime, (const char*)m_RecStartTime, DATETIME_LENGTH);
		}

		// Rec End Date/Time
		if(strlen(m_RecEndDate) > 0)
		{
			strncpy(header.recEnd.recDate, (const char*)m_RecEndDate, DATETIME_LENGTH);
		}
		if(strlen(m_RecEndTime) > 0)
		{
			strncpy(header.recEnd.recTime, (const char*)m_RecEndTime, DATETIME_LENGTH);
		}

		if(converter->SetDSSHeaderInfo(&header)!=DssResult_Success){
			//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter ", "setting dss headerinfo failed ");
			return S_FALSE;
		}
	}

	// 	Convert to DSS
	//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter ", "Dss convertion started ");
	if(converter->StartConverting()!=DssResult_Success){
		//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter ", "Dss convertion cannot be started ");
		return S_FALSE;
	}

	ULONG size = 1024 * 1024;				// buffer size
	char *buf = new char[size];

	reader->SetPos(44);
	if(split_enabled)
	{
		// set the split points if the file needs to split into multiple files
		reader->setSplitPoints(start,::end);
		//read the pcm data from start position to end position and gives to dss encoder
		while(reader->ReadSplitFile(buf,size)==S_OK)
		{
			converter->Convert(buf,size);
			//size = sizeof(buf);
			//::memset(buf, 0, size);
		}
	}
	else
	{
		// read the pcm data and gives to dss encoder
		while(reader->Read(buf,size)==S_OK)
		{
			converter->Convert(buf,size);

			size = sizeof(buf);
			::memset(buf, 0, size);
		}
	}


	converter->EndConverting();
	//__android_log_print(ANDROID_LOG_VERBOSE, "DSS Converter ", "Dss convertion ended ");

	delete converter;

	reader->Close();
	delete reader;
	reader = NULL;

	writer->Close();
	delete writer;
	writer = NULL;

	// release the pointers

	env->ReleaseStringUTFChars(sourceFile,m_InputFile);
	env->ReleaseStringUTFChars(destFile,m_OutputFile);
	
	env->ReleaseStringUTFChars(jPassword,m_Password);
	env->ReleaseStringUTFChars(jAuthorID,m_AuthorID);
	env->ReleaseStringUTFChars(jWorktypeID,m_WorktypeID);
	env->ReleaseStringUTFChars(jJobNumber,m_JobNumber);
	env->ReleaseStringUTFChars(jComment,m_Comment);
	env->ReleaseStringUTFChars(jRecStartDate,m_RecStartDate);
	env->ReleaseStringUTFChars(jRecStartTime,m_RecStartTime);
	env->ReleaseStringUTFChars(jRecEndDate,m_RecEndDate);
	env->ReleaseStringUTFChars(jRecEndTime,m_RecEndTime);


	return S_OK;
}
