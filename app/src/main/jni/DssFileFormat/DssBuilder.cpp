#include "stdafx_ff.h"

#include "DssElement.h"
#include "DssCryptor.h"


#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


namespace DssFileFormat {

// -- buffer scanning ----------------------

DssBuilder::DssBuilder()
: cumulative_time_(0)
, m_fbOverflow(0)
, blocks_sent_(0)
, compression_mode_(3)
, frames_received_(0)
{
}

const REFERENCE_TIME
DssBuilder::GetCumulativeTime() {
	return this->cumulative_time_;
}

const int 
DssBuilder::GetCompressionMode() {
	return this->compression_mode_;
}
void 
DssBuilder::SetCompressionMode(compression_mode_t v) {
	this->compression_mode_ = v;
}

static const int mode_qualities[] = {3,2,1,0,1,0,5,4};
// copies incoming data to the parse buffer.
// modified for 64 bit
long
DssBuilder::Fill(const BYTE* pData, LONG cBytes, int mode, LONGLONG start_time, bool is_eos)
{
	if (mode <= 7 && mode_qualities[mode] > mode_qualities[compression_mode_]) {
		compression_mode_ = mode;
	}

	LONG inbytes;		// modified for 64 bit
	for (inbytes = cBytes; inbytes > 0; inbytes -= SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER) {
		LONG tofill = min(inbytes, SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER);
		frame_stream_.Fill((byte_t *)pData, (word_t)tofill, 0, mode, tofill, 0, start_time, 0, false);
		pData += tofill;
	}

	while (frame_stream_.GetCumulativeBytes() >= SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER || (is_eos && frame_stream_.GetCumulativeBytes() > 0)) {
		frame_stream_.Compact(is_eos);
		DssBlock *pb = new DssBlock(m_Cryptor, (int)blocks_sent_, cumulative_time_);
		pb->ForceFillPayload(frame_stream_.GetPayload());
		pb->set_FirstBlockFramePointer(48+8*m_fbOverflow);
		pb->set_CompressionMode((compression_mode_t)frame_stream_.GetPayloadCompressionMode());
		pb->AnalyzeBlockFrames(m_fbOverflow, 0, SIZE_FRAMEBLOCK_HEADER + frame_stream_.GetPayloadLength());
		m_fbOverflow = m_fbOverflow+pb->get_BlockFrameBytes() - (SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER);
		cumulative_time_ += pb->GetDurationRefTime();
		frames_received_ += pb->GetActualNumberOfBlockFrames();		//sanity check
		pb->UpdateRaw();
		if (this->password_.length() > 0) {
			pb->Encrypt();
		}
#ifdef _DEBUG
//		{
//			wchar_t tz[512];
//			short lastword = *((short *)(&pb->Payload()[510]));
//			wsprintf(tz, L"DssBuilder::Fill() new block first word = %X last word = %X.\n", *((short *)(&pb->Payload()[6])),  lastword);
////			OutputDebugString(tz);
//			if (!(lastword >= 0 && lastword < 0xc)) {
//				bool debug_stop = true;
//			}
//		}
#endif
		frame_stream_.ReleasePayload();
		blocks_.push_back(pb);
	}
	if (is_eos && this->password_.length()) {
		unsigned char salt[17];
		unsigned char verification_key[5];
		unsigned int version = 1;
		m_Cryptor.get_salt(salt,16);
		m_Cryptor.get_verification_key(verification_key,4);
		m_Cryptor.get_crypt_version(&version);
		GetCommonHeader()->set_SaltValue(salt);
		GetCommonHeader()->set_VerificationValue(verification_key);
		GetCommonHeader()->set_EncryptionVersion(version);
	}
	return cBytes;
}

bool
DssBuilder::LoadHeadersFromRaw(const DssFileFormat::byte_t *pData, LONG cBytes) {		// modified for 64 bit
	int num_headers = cBytes / SIZE_DSS_SECTOR;
	if (cBytes < SIZE_DSS_SECTOR) {
		return false;
	}
	DssCommonHeader *pch = new DssCommonHeader(m_Cryptor);
	if (!pch->Parse(&pData[0], cBytes))
		return false;
	headers_.push_back(pch);
	cBytes -= pch->Length();
	if (pch->get_HeaderBlockNum() != num_headers) {
		return false;
	}
	if (cBytes < SIZE_DSS_SECTOR) {
		return true;
	}
	pData += SIZE_DSS_SECTOR;
	DssHeader *pco = 0;
	if (pch->get_VersionId() < 2) {		//regular DSS
		pco = new DssOptionalHeader(m_Cryptor);
	}
	else {								//DSS PRO
		pco = new DssProOptionalHeader(m_Cryptor);
	}
	if (!pco->Parse(&pData[0], cBytes)) {
		return false;
	}
	headers_.push_back(pco);
	cBytes -= pch->Length();
	pData += SIZE_DSS_SECTOR;

	DssHeader *pco2;
	if (cBytes > SIZE_DSS_SECTOR && pch->get_HeaderBlockNum() > 2 ) {
		pco2 = new DssProOptionalHeader(m_Cryptor);
		if (!pco2->Parse(&pData[0], cBytes)) {
			delete pco2;
		}
		else {
			headers_.push_back(pco2);
			cBytes -= pco2->Length();
			pData += pco2->Length();
		}
	}

	while (cBytes > SIZE_DSS_SECTOR) {
		DssHeader *pcbh = new DssHeader(m_Cryptor);
		if (!pcbh->Parse(&pData[0], cBytes)) {
			return false;
		}
		headers_.push_back(pcbh);
		pData += SIZE_DSS_SECTOR;
	}
	return true;
}

void 
DssBuilder::BuildHeaders(int num_headers, double version) {
	if (num_headers-- > 0) {
		DssCommonHeader *pch = new DssCommonHeader(m_Cryptor);
		pch->set_HeaderBlockNum(num_headers+1);
		if (password_.length() > 0) {
			pch->set_SelfIdentifier("enc");
		}
		pch->set_VersionId(word_t(version));
		pch->UpdateRaw();
		headers_.push_back(pch);
	}
	if (num_headers-- > 0) {
		DssHeader *ph = 0;
		if (version < 2.)
			ph = static_cast<DssHeader *>(new DssOptionalHeader(m_Cryptor));
		else {
			ph = static_cast<DssHeader *>(new DssProOptionalHeader(m_Cryptor));
            
		}
		headers_.push_back(ph);
	}
	while (num_headers-- > 0) {
		DssHeader *ph = 0;
		if (version < 2.)
			ph = static_cast<DssHeader *>(new DssHeader(m_Cryptor));
		else {
			ph = static_cast<DssHeader *>(new DssProOptionalHeader2(m_Cryptor));
		}
		headers_.push_back(ph);
	}
	
}
    void
    DssBuilder::BuildHeadersNew(int num_headers, double version,int mode, std::string workType) {
        if (num_headers-- > 0) {
            DssCommonHeader *pch = new DssCommonHeader(m_Cryptor);
            pch->set_HeaderBlockNum(num_headers+1);
            if (password_.length() > 0) {
                pch->set_SelfIdentifier("enc");
            }
            pch->set_VersionId(word_t(version));
            pch->UpdateRaw();
            headers_.push_back(pch);
        }
        if (num_headers-- > 0) {
            DssHeader *ph = 0;
            if (version < 2.)
                ph = static_cast<DssHeader *>(new DssOptionalHeader(m_Cryptor));
            else {
//                ph = static_cast<DssHeader *>(new DssProOptionalHeader(m_Cryptor));
                DssProOptionalHeader*   ph1= (new DssProOptionalHeader(m_Cryptor));
                
                
                ph1->set_QualityAndWorkType(mode, workType);
                ph = static_cast<DssHeader *>(ph1);
                
            }
            headers_.push_back(ph);
        }
        while (num_headers-- > 0) {
            DssHeader *ph = 0;
            if (version < 2.)
                ph = static_cast<DssHeader *>(new DssHeader(m_Cryptor));
            else {
                ph = static_cast<DssHeader *>(new DssProOptionalHeader2(m_Cryptor));
            }
            headers_.push_back(ph);
        }
        
    }
    
    
DssElement* 
DssBuilder::front() {
	if (blocks_sent_ < headers_.size() ) {
		return headers_[blocks_sent_];
	}
	if (blocks_.size() > 0) {
		return blocks_.front();
	}
	return 0;
}
void 
DssBuilder::pop_front() {
	if (blocks_sent_ >= headers_.size() && blocks_.size() > 0) 
	{
		delete blocks_.front();
		blocks_.pop_front();
	}
	blocks_sent_++;
}

DssCommonHeader* 
DssBuilder::GetCommonHeader() {
	if (headers_.size() > 0) {
		return static_cast<DssCommonHeader *>(headers_[0]);
	}
	return 0;
}
DssOptionalHeader* 
DssBuilder::GetOptionalHeader() {
	if (headers_.size() > 1) {
		return static_cast<DssOptionalHeader *>(headers_[1]);
	}
	return 0;
}
DssHeader* 
DssBuilder::GetHeaderByIndex(int n) {
	if (headers_.size() > (size_t)n) {
		return static_cast<DssHeader *>(headers_[n]);
	}
	return 0;
}

int
DssBuilder::GetFileHeaderBytes() {
	int bytes = 0;
	std::vector<DssHeader *>::iterator ih = headers_.begin();
	while (ih != headers_.end()) {
		bytes += (*ih)->Length();
		++ih;
	}
	return bytes;
}

DssBuilder::~DssBuilder() {
	Empty();
}

void
DssBuilder::FlushBlocks()
{
	std::list<DssElement *>::iterator ib = blocks_.begin();
	while (ib != blocks_.end()) {
		delete *ib;
		++ib;
	}
	blocks_.clear();
	frame_stream_.Clear();
	current_time_ = 0;
	cumulative_time_ = 0;
	frames_received_ = 0;
}

// discards all data
void 
DssBuilder::Empty()
{
	std::vector<DssHeader *>::iterator ih = headers_.begin();
	while (ih != headers_.end()) {
		delete *ih;
		++ih;
	}
	headers_.clear();
	std::list<DssElement *>::iterator ib = blocks_.begin();
	while (ib != blocks_.end()) {
		delete *ib;
		++ib;
	}
	blocks_.clear();
	frame_stream_.Clear();
	m_Cryptor.reset();
	current_time_ = 0;
	cumulative_time_ = 0;
}

void 
DssBuilder::SetPassword(const char *szpassword) {
	char temp[17];
	if (0 == szpassword || strlen(szpassword) == 0) {
		password_ = std::string("");
		m_Cryptor.reset();		
	}
	else if (strlen(szpassword)) {
		::memset(temp,0,sizeof(temp));
		::memcpy(temp,szpassword,min(strlen(szpassword),sizeof(temp)-1));
		password_ = std::string(temp);
		m_Cryptor.set_password(password_.c_str(), password_.length());
		if (GetCommonHeader()) {
			GetCommonHeader()->set_SelfIdentifier("enc");
		}
	}
}

void 
DssBuilder::SetEncryptionVersion(const WORD version)
{
	m_Cryptor.set_crypt_version(version);
}
};
