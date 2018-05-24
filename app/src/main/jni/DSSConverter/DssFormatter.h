#pragma once

#include "DssDefinitions.h"
#include "DssFileFormat/DssElement.h"			//Modified ~ Android Porting
#include "DssFileFormat/DssFileFormat.h"		//Modified ~ Android Porting


class DssFormatter
{
public:
	DssFormatter(void);
	~DssFormatter(void);

	DSSRESULT StartFormatting(void);
	DSSRESULT EndFormatting(void);

	DSSRESULT AddFrameData(unsigned char *pData, size_t len);	// Encode‚³‚ê‚½Frameƒf�[ƒ^‚ð’Ç‰Á
	DSSRESULT SetCompressionMode(DSS_VERSION ver, DssFileFormat::compression_mode_t mode);
    DSSRESULT SetCompressionModeAndWorkType(DSS_VERSION ver, DssFileFormat::compression_mode_t mode,std::string workType);/* Function to set compression mode and worktype   */


	void SetOutputCallback(FPCALLBACK_DSSWRITE fp);

	bool SetPassword			(const char *pszPassword);
	bool SetSalt				(const char *pszSalt);
	bool SetEncryptionVersion	(const WORD version);
	bool SetMode				(unsigned int uiMode);
	bool SetNumberOfHeaders		(unsigned int uiNumberOfHeaders);
	bool GetHeaderBytes			(unsigned int *puiNumberOfHeaderBytes);
	bool GetHeaders				(unsigned char *pbBuffer, unsigned int cbSize);
	bool SetHeaders				(const unsigned char *pbBuffer, unsigned int cbSize);
	//bool GetDataBytesWritten	(LONGLONG *pllBytesWritten);
	bool GetRecordingTime		(LONGLONG *pllRecordingTime);
	bool set_SelfIdentifier		(const char *szBuf);
	bool set_VersionId			(unsigned short v);
	bool set_ReleaseId			(unsigned short r);
	bool set_AuthorId			(const char *szBuf);
	bool set_JobNumber			(unsigned int jobNumer);
	bool set_ObjectWord			(unsigned short objWord);
	bool set_ProcessWord		(unsigned short processWord);
	bool set_StatusWord			(unsigned short statusWord);
	bool set_RecordingStartDate (const char *recStartDate);
	bool set_RecordingStartTime (const char *recStartTime);
	bool set_RecordingEndDate	(const char *recEndDate);
	bool set_RecordingEndTime	(const char *recEndTime);
	bool set_LengthOfRecording	(const char *length);
	bool set_AttributeFlag		(unsigned char attribute);
	bool set_PriorityLevel		(unsigned char priorityLevel);
	bool set_TargetTranscriptionistId (const char *typistID);
	bool set_InstructionMarks	(const unsigned char *szBuf, unsigned int cbSize);
	bool set_AdditionalInstructionMarks (const unsigned char *szBuf, unsigned int cbSize);

	bool set_Notes				(const char *notes);
	bool set_PriorityStatus		(unsigned char priorityStatus);
	bool set_WorktypeId			(const char *worktype);

private:
	FPCALLBACK_DSSWRITE					fpCallback_;

	DssFileFormat::DssBuilder			dssbuilder_;

	DSS_VERSION							version_;
	DssFileFormat::compression_mode_t	compression_mode_;
	std::string							password_;
	char								encryption_salt_[16];
	WORD								encrypt_version_;
	bool								user_salt_;
	bool								build_no_headers_;
	unsigned long						total_data_len_;

	char                                write_start_date_[100];
	REFERENCE_TIME						write_start_time_;
	REFERENCE_TIME						write_stop_time_;

	void UpdateRecDateTime();
};

