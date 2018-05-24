#include <time.h>

#include "DssFormatter.h"
void _strtime_s(char *buf,size_t size);
void _strdate_s(char *buf,size_t size);

// time utilities
static void get_time_now_hhmmss(char *szout, int size) {
	if (size < 7) 
		return;
	char tmpbuf[10];
	_strtime_s( tmpbuf, 10 );
	::memcpy(&szout[0], &tmpbuf[0],2);
	::memcpy(&szout[2], &tmpbuf[3],2);
	::memcpy(&szout[4], &tmpbuf[6],2);
	szout[6] = 0;
}

static void get_date_now_yymmdd(char *szout, int size) {
	if (size < 7) 
		return;
    
	char tmpbuf[10];
	_strdate_s( tmpbuf, 10);
	::memcpy(&szout[0], &tmpbuf[6],2);
	::memcpy(&szout[2], &tmpbuf[0],2);
	::memcpy(&szout[4], &tmpbuf[3],2);
	szout[6] = 0;
}

void _strtime_s(char *buf,size_t size){
    time_t rawtime;
    struct tm * timeinfo;
    
    time (&rawtime);
    timeinfo = localtime (&rawtime);
    
    strftime(buf, size, "%H:%M:%S", timeinfo);
}
void _strdate_s(char *buf,size_t size){
    time_t rawtime;
    struct tm * timeinfo;
    
    time (&rawtime);
    timeinfo = localtime (&rawtime);
    
    strftime(buf, size, "%m:%d:%y", timeinfo);
}


void _tzset(){
    tzset();
}
DssFormatter::DssFormatter(void) :
				  version_(VERSION_UNKNOWN)
				, compression_mode_(DssFileFormat::compression_mode_t(0))
				, password_("")
				, user_salt_(false)
				, build_no_headers_(false)
				, fpCallback_(0)
				, total_data_len_(0)
{
}


DssFormatter::~DssFormatter(void)
{
}

DSSRESULT DssFormatter::StartFormatting(void)
{
	// Build the DSS headers if they haven't already been made
	if(!build_no_headers_ && !dssbuilder_.front())
	{
		dssbuilder_.FlushBlocks();
		dssbuilder_.BuildHeaders(version_ > 1 ? 3 : 2, double(version_));
	}

	// Set the compression mode to be used
	dssbuilder_.SetCompressionMode(compression_mode_);

	if(dssbuilder_.GetCommonHeader())
	{
		if(password_.length() > 0 && version_ > 1)
		{
			dssbuilder_.SetPassword(password_.c_str());
			dssbuilder_.GetCommonHeader()->InitEncryption(0, user_salt_ ? encryption_salt_ : 0);
		}
		else if(version_ > 1)
		{
			dssbuilder_.GetCommonHeader()->set_SelfIdentifier(std::string("ds2"));
		} 
		else
		{
			dssbuilder_.SetPassword(0);		//no password
			dssbuilder_.GetCommonHeader()->set_SelfIdentifier(std::string("dss"));
		}
	}

	char refbuf[10];
	_tzset();

	// Display operating system-style date and time.
	get_date_now_yymmdd(write_start_date_,10);
	get_time_now_hhmmss(refbuf, 10);
	write_start_time_ = DssFileFormat::convert_hhmmss_to_reftime(refbuf);

	total_data_len_ = 0;

	return DssResult_Success;
}

DSSRESULT DssFormatter::EndFormatting(void)
{
	dssbuilder_.Fill(0, 0, compression_mode_, (LONGLONG)dssbuilder_.GetCumulativeTime(), true);
	while(dssbuilder_.front())
	{
		long outputLen = dssbuilder_.front()->Length();
		unsigned long offset = total_data_len_;
		if(fpCallback_)
		{
			(*fpCallback_)((char*)dssbuilder_.front()->Payload(), dssbuilder_.front()->Length(), offset);
		}
		
		total_data_len_ += outputLen;
		dssbuilder_.pop_front();
	}

	// update DSS Header

	unsigned long jobNumber = dssbuilder_.GetCommonHeader()->get_JobNumber();
	if(jobNumber == 0xffffffff)
	{
		dssbuilder_.GetCommonHeader()->set_JobNumber(1);
	}

	char refbuf[100];
	REFERENCE_TIME runtime = dssbuilder_.GetCumulativeTime();
	DssFileFormat::convert_reftime_to_hhmmss(refbuf, 8, runtime);
	dssbuilder_.GetCommonHeader()->set_Length(std::string(refbuf));

	UpdateRecDateTime();

	dssbuilder_.GetCommonHeader()->UpdateRaw();

	if(dssbuilder_.GetOptionalHeader())
	{
		dssbuilder_.GetOptionalHeader()->set_ExRecLength((DssFileFormat::dword_t)(runtime*1000L/UNITS));
		dssbuilder_.GetOptionalHeader()->set_Quality(dssbuilder_.GetCompressionMode());
		dssbuilder_.GetOptionalHeader()->UpdateRaw();
	}

	unsigned long offset = 0;
	long commonHeaderLen = dssbuilder_.GetCommonHeader()->Length();
	if(fpCallback_)
	{
		(*fpCallback_)((char*)dssbuilder_.GetCommonHeader()->Payload(), commonHeaderLen, offset);
	}
	offset += commonHeaderLen;

	if(dssbuilder_.GetOptionalHeader())
	{
		if(fpCallback_)
		{
			(*fpCallback_)((char*)dssbuilder_.GetOptionalHeader()->Payload(), dssbuilder_.GetOptionalHeader()->Length(), offset);
		}
	}


	return DssResult_Success;
}


void DssFormatter::UpdateRecDateTime()
{
	const unsigned char invalidData[7] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00};
	char refbuf[100];

	std::string startDate = dssbuilder_.GetCommonHeader()->get_RecordingStartDate();
	if(strncmp(startDate.c_str(), (const char *)invalidData, 6) == 0)
	{
		dssbuilder_.GetCommonHeader()->set_RecordingStartDate(write_start_date_);
		DssFileFormat::convert_reftime_to_hhmmss(refbuf, 8, write_start_time_);
		dssbuilder_.GetCommonHeader()->set_RecordingStartTime(std::string(refbuf));
	}

	std::string endDate = dssbuilder_.GetCommonHeader()->get_RecordingEndDate();
	if(strncmp(endDate.c_str(), (const char *)invalidData, 6) == 0)
	{
		get_date_now_yymmdd(refbuf,10);
		dssbuilder_.GetCommonHeader()->set_RecordingEndDate(refbuf);
		get_time_now_hhmmss(refbuf, 10);
		dssbuilder_.GetCommonHeader()->set_RecordingEndTime(refbuf);
	}
}


DSSRESULT DssFormatter::AddFrameData(unsigned char *pData, size_t len)
{
	if(!pData)
	{
		return DssResult_Error;
	}

	int mode = ((short *)pData)[0];
	dssbuilder_.Fill(&pData[2], (LONG)(len - 2), mode, (LONGLONG)dssbuilder_.GetCumulativeTime(), false);	// modified for 64 bit

	while(dssbuilder_.front())
	{
		long outputLen = dssbuilder_.front()->Length();
		unsigned long offset = total_data_len_;
		if(fpCallback_)
		{
			(*fpCallback_)((char*)dssbuilder_.front()->Payload(), dssbuilder_.front()->Length(), offset);
		}
		
		total_data_len_ += outputLen;
		dssbuilder_.pop_front();
	}

	return DssResult_Success;
}


DSSRESULT DssFormatter::SetCompressionMode(DSS_VERSION ver, DssFileFormat::compression_mode_t mode)
{
	version_ = ver;

	dssbuilder_.BuildHeaders(version_ > 1 ? 3 : 2, double(version_));
	SetMode(mode);

	return DssResult_Success;
}

DSSRESULT DssFormatter::SetCompressionModeAndWorkType(DSS_VERSION ver, DssFileFormat::compression_mode_t mode,std::string workType)
{
	version_ = ver;
    
    //	dssbuilder_.BuildHeaders(version_ > 1 ? 3 : 2, double(version_));
    dssbuilder_.BuildHeadersNew(version_ > 1 ? 3 : 2, double(version_), mode,workType);
	SetMode(mode);
    
	return DssResult_Success;
}


void DssFormatter::SetOutputCallback(FPCALLBACK_DSSWRITE fp)
{
	fpCallback_ = fp;
}


bool DssFormatter::SetPassword(const char *pszPassword)
{
	if (pszPassword == NULL)
	{
		dssbuilder_.SetPassword(0);
	}
	else if (DssFileFormat::DssCryptor::check_password(pszPassword))
	{
		password_ = std::string(pszPassword);
		dssbuilder_.SetPassword(pszPassword);
	}
	else
	{
		return false;
	}
	return true;
}

bool DssFormatter::SetSalt(const char *pszSalt)
{
	if(0 == pszSalt)
	{
		user_salt_ = false;
	}
	else
	{
		user_salt_ = true;
		::memcpy(encryption_salt_, pszSalt, 16);
	}
	return true;
}

bool DssFormatter::SetEncryptionVersion(const WORD version)
{
	if(version != 1 && version != 2)
	{
		 return false;
	}
	
	encrypt_version_ = version;
	dssbuilder_.SetEncryptionVersion(encrypt_version_);
	return true;
}

bool DssFormatter::SetMode(unsigned int uiMode)
{
	compression_mode_ = DssFileFormat::compression_mode_t(uiMode);
	if(compression_mode_ == DssFileFormat::MLP_NO_SCVA ||
		compression_mode_ == DssFileFormat::MLP_WITH_SCVA )
	{
		return false;	// we don't support MLP, so don't allow it to be set!
	}
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.SetCompressionMode(compression_mode_);
		return true;
	}
	return false;
}

bool DssFormatter::SetNumberOfHeaders(unsigned int uiNumberOfHeaders)
{
	build_no_headers_ = false;
	if(dssbuilder_.GetFileHeaderBytes()/DssFileFormat::SIZE_DSS_SECTOR != uiNumberOfHeaders)
	{
		dssbuilder_.Empty();
		dssbuilder_.BuildHeaders(uiNumberOfHeaders);
		return true;
	}
	else if(dssbuilder_.GetCommonHeader() && dssbuilder_.GetCommonHeader()->get_HeaderBlockNum() != uiNumberOfHeaders)
	{
		dssbuilder_.Empty();
		dssbuilder_.BuildHeaders(uiNumberOfHeaders);
		return true;
	}
	else if(0 == uiNumberOfHeaders)
	{
		dssbuilder_.Empty();
		build_no_headers_ = true;
		return true;
	}
	else
	{
		dssbuilder_.BuildHeaders(uiNumberOfHeaders);
		return true;
	}
	return false;
}

bool DssFormatter::GetHeaderBytes(unsigned int* puiNumberOfHeaderBytes)
{
	if (0 == puiNumberOfHeaderBytes)
	{
		return false;
	}
	*puiNumberOfHeaderBytes = dssbuilder_.GetFileHeaderBytes();
	return true;
}

bool DssFormatter::GetHeaders(unsigned char *pbBuffer, unsigned int cbSize)
{
	if(0 == pbBuffer)
	{
		return false;
	}
	unsigned int copied = min(cbSize, (unsigned int)dssbuilder_.GetFileHeaderBytes());
	unsigned int to_copy = copied;
	unsigned int n = 0;
	while (to_copy >= DssFileFormat::SIZE_DSS_SECTOR && dssbuilder_.GetHeaderByIndex(n))
	{
		::memcpy(&pbBuffer[n*DssFileFormat::SIZE_DSS_SECTOR], dssbuilder_.GetHeaderByIndex(n)->Data(), DssFileFormat::SIZE_DSS_SECTOR);
		to_copy -= DssFileFormat::SIZE_DSS_SECTOR;
		++n;
	}
	if (to_copy != 0)
	{
		return false;
	}
	return true;
}

bool DssFormatter::SetHeaders(const unsigned char *pbBuffer, unsigned int cbSize)
{
	if(NULL == pbBuffer)
	{
		return false;
	}
	//UNUSED VARIABLE unsigned int avail = min(cbSize, (unsigned int)dssbuilder_.GetFileHeaderBytes());
	unsigned int n = 0;
	while(cbSize >= DssFileFormat::SIZE_DSS_SECTOR && dssbuilder_.GetHeaderByIndex(n))
	{
		dssbuilder_.GetHeaderByIndex(n)->Parse(&pbBuffer[DssFileFormat::SIZE_DSS_SECTOR*n], DssFileFormat::SIZE_DSS_SECTOR, n == 0 ? false : dssbuilder_.GetCommonHeader()->IsEncrypted());
		cbSize -= DssFileFormat::SIZE_DSS_SECTOR;
		++n;
	}
	if (cbSize != 0)
	{
		return false;
	}
	return true;
}

//bool DssFormatter::GetDataBytesWritten(LONGLONG *pllBytesWritten)
//{
//	if(0 == pllBytesWritten)
//	{
//		return false;
//	}
//	*pllBytesWritten = m_cbTransData;
//	return true;
//}

bool DssFormatter::GetRecordingTime(LONGLONG *pllRecordingTime)
{
	if(0 == pllRecordingTime)
	{
		return false;
	}
	*pllRecordingTime = dssbuilder_.GetCumulativeTime();
	return true;
}

bool DssFormatter::set_SelfIdentifier(const char *szBuf)
{
	if(0 == szBuf)
	{
		return false;
	}
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_SelfIdentifier(std::string(szBuf));
		return true;
	}
	return false;
}

bool DssFormatter::set_VersionId(unsigned short v)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_VersionId(v);
		return true;
	}
	return false;
}

bool DssFormatter::set_ReleaseId(unsigned short r)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_ReleaseId(r);
		return true;
	}
	return false;}

bool DssFormatter::set_AuthorId(const char *szBuf)
{
	if(0 == szBuf)
	{
		return false;
	}
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_AuthorId(std::string(szBuf));
		return true;
	}
	return false;
}

bool DssFormatter::set_JobNumber(unsigned int jobNumer)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_JobNumber(jobNumer);
		return true;
	}
	return false;
}

bool DssFormatter::set_ObjectWord(unsigned short objWord)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_ObjectWord(objWord);
		return true;
	}
	return false;
}

bool DssFormatter::set_ProcessWord(unsigned short processWord)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_ProcessWord(processWord);
		return true;
	}
	return false;
}

bool DssFormatter::set_StatusWord(unsigned short statusWord)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_StatusWord(statusWord);
		return true;
	}
	return false;
}

bool DssFormatter::set_RecordingStartDate(const char *recStartDate)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_RecordingStartDate(std::string(recStartDate));
		return true;
	}
	return false;
}

bool DssFormatter::set_RecordingStartTime(const char *recStartTime)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_RecordingStartTime(std::string(recStartTime));
		return true;
	}
	return false;
}

bool DssFormatter::set_RecordingEndDate(const char *recEndDate)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_RecordingEndDate(std::string(recEndDate));
		return true;
	}
	return false;
}

bool DssFormatter::set_RecordingEndTime(const char *recEndTime)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_RecordingEndTime(std::string(recEndTime));
		return true;
	}
	return false;
}

bool DssFormatter::set_LengthOfRecording(const char *length)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_Length(std::string(length));
		return true;
	}
	return false;
}

bool DssFormatter::set_AttributeFlag(unsigned char attribute)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_AttributeFlag(attribute);
		return true;
	}
	return false;
}

bool DssFormatter::set_PriorityLevel(unsigned char priorityLevel)
{
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_PriorityLevel(priorityLevel);
		return true;
	}
	return false;
}

bool DssFormatter::set_TargetTranscriptionistId(const char *typistID)
{
	if(typistID == 0)
	{
		return false;
	}
	if(dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_TypistId(std::string(typistID));
		return true;
	}
	return false;
}

bool DssFormatter::set_InstructionMarks(const unsigned char *szBuf, unsigned int cbSize)
{
	if(0 == szBuf)
	{
		return false;
	}
	if(cbSize >= DssFileFormat::SIZE_INSTRUCTION_MARK && dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_IMark(szBuf);
		return true;
	}
	return false;
}

bool DssFormatter::set_AdditionalInstructionMarks(const unsigned char *szBuf, unsigned int cbSize)
{
	if(0 == szBuf)
	{
		return false;
	}
	if(cbSize >= DssFileFormat::SIZE_ADDITIONAL_INSTRUCTION_MARK && dssbuilder_.GetCommonHeader())
	{
		dssbuilder_.GetCommonHeader()->set_AdditionalIMark(szBuf);
		return true;
	}
	return false;
}

bool DssFormatter::set_Notes(const char *szBuf)
{
	if(0 == szBuf)
	{
		return false;
	}
	if(dssbuilder_.GetOptionalHeader())
	{
		dssbuilder_.GetOptionalHeader()->set_Notes(std::string(szBuf));
		return true;
	}
	return false;
}

bool DssFormatter::set_PriorityStatus(unsigned char priorityStatus)
{
	if(dssbuilder_.GetOptionalHeader())
	{
		dssbuilder_.GetOptionalHeader()->set_PriorityStatus((DssFileFormat::byte_t)priorityStatus);
		return true;
	}

	return false;
}

bool DssFormatter::set_WorktypeId(const char *worktype)
{
	if(0 == worktype)
	{
		return false;
	}
	if(dssbuilder_.GetOptionalHeader())
	{
		dssbuilder_.GetOptionalHeader()->set_WorkTypeId(std::string(worktype));
		return true;
	}
	return false;
}


