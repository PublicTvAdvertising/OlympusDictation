#include "stdafx_ff.h"

#include "DssElement.h"
#include "DssCryptor.h"
#include "64bit.h"

#include <cmath>


#undef DSS_PARSING_LOG_DEBUGGING

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

#ifndef min
#define min(a,b)			(((a) < (b)) ? (a) : (b))
#endif
namespace DssFileFormat {

DssBlock::DssBlock(DssCryptor &cryptor, int serial_num, LONGLONG time_) 			
	: DssElement(cryptor)
	, serial_num_(serial_num)
	, time_offset_(time_)
	, block_frame_bytes_(0)
	, partial_offset_(506)
	, failed_decryption_(false)
{
	m_cBytes = SIZE_DSS_SECTOR;
	m_elementId = DssFileFormat::DSS_BLOCK_HEADER;
	block_header_.compression_id = LP_NO_SCVA;
	block_header_.first_block_frame_pointer = 0xFFF;
	block_header_.fw_first_block_frame_pointer = 0xFFF;
	block_header_.number_of_block_frames = 0xFF;
	block_header_.fw_number_of_block_frames = 0xFF;
}
DssBlock::~DssBlock() {
}

//================ Meta Data Accessors
const word_t					
DssBlock::get_FirstBlockFramePointer() 
{
	return block_header_.first_block_frame_pointer;
}
void							
DssBlock::set_FirstBlockFramePointer(word_t v)
{
	block_header_.first_block_frame_pointer = v;
}
const word_t					
DssBlock::get_FirstBlockFramePointerFW()
{
	return block_header_.fw_first_block_frame_pointer;
}
void							
DssBlock::set_FirstBlockFramePointerFW(word_t v)
{
	block_header_.fw_first_block_frame_pointer = v;
}
const byte_t					
DssBlock::get_NumberOfBlockFrames()
{
	return block_header_.number_of_block_frames;
}
void							
DssBlock::set_NumberOfBlockFrames(byte_t v)
{
	block_header_.number_of_block_frames = v;
}
const byte_t					
DssBlock::get_NumberOfBlockFramesFW()
{
	return block_header_.fw_number_of_block_frames;
}
void							
DssBlock::set_NumberOfBlockFramesFW(byte_t v)
{
	block_header_.fw_number_of_block_frames = v;
}
const compression_mode_t		
DssBlock::get_CompressionMode()
{
	return block_header_.compression_id;
}
void							
DssBlock::set_CompressionMode(compression_mode_t v)
{
	block_header_.compression_id = v;
}

//================ Block Parser
bool 
DssBlock::Parse(const byte_t* pData, LONG cBytes, bool is_encrypted, bool enable_decryption) {
	if (!DssElement::Parse(pData, cBytes)) 
		return false;

	int bytes_used = 0;
	::memcpy_s(raw_header_,sizeof(raw_header_),&pData[bytes_used], min(sizeof(raw_header_), cBytes-bytes_used));
	bytes_used += min(sizeof(raw_header_), cBytes-bytes_used);

	block_header_.first_block_frame_pointer = (raw_header_[0]&0xFFF0)>>4;
	block_header_.fw_first_block_frame_pointer = ((raw_header_[0]&0x000F)<<8) | ((raw_header_[1]&0xFF00)>>8);
	block_header_.number_of_block_frames = byte_t(raw_header_[1]&0xFF);
	block_header_.fw_number_of_block_frames = byte_t((raw_header_[2]&0xFF00)>>8);
	block_header_.compression_id = compression_mode_t(raw_header_[2]&0xFF);

	//if ((block_header_.compression_id & 0x1) != 0) {
#ifdef _DEBUG
	//WCHAR wzDebug[512];
	//wsprintf(wzDebug, L"Parsed block with compression ID = %d.\n",block_header_.compression_id);
	//DbgParseLog(wzDebug);
#endif
//	}
	m_elementId = DSS_BLOCK_HEADER;
	m_cHdr = DssFileFormat::SIZE_FRAMEBLOCK_HEADER;

	if (is_encrypted && enable_decryption) {
		failed_decryption_ = (m_cBytes != SIZE_DSS_SECTOR) || !m_Cryptor.decrypt_block(this->m_pItem, m_cBytes);
	}
	return true;
}
void 
DssBlock::UpdateRaw() {
	raw_header_[0] = (block_header_.first_block_frame_pointer << 4) | ((block_header_.fw_first_block_frame_pointer>>8)&0xF);
	raw_header_[1] = ((block_header_.fw_first_block_frame_pointer&0x00FF) << 8) | (block_header_.number_of_block_frames&0xFF);
	raw_header_[2] = (word_t(block_header_.fw_number_of_block_frames) << 8) | (block_header_.compression_id);

	::memcpy(&m_pItem[0],raw_header_,sizeof(raw_header_));

}
bool
DssBlock::Encrypt(DssCryptor *pCryptor) {
	if (pCryptor) {
		return pCryptor->encrypt_block(m_pItem, m_cBytes);
	} else {
		return m_Cryptor.encrypt_block(m_pItem, m_cBytes);
	}
}
//================ Block analysis methods

bool DssBlock::FailedDecryption() {
	return failed_decryption_;
}

const int frame_bits[QP_WITH_SCVA+1][2] = {	{328,	328},
											{72,	328},
											{192,	192},
											{32,	192},
											{192,	192},
											{32,	192},
											{448,	448},
											{96,	448}
};

const LONGLONG DssBlock::GetTimeOffset() {
	return time_offset_;
}

word_t 
DssBlock::GetBitsForFrame(byte_t* pdata, BitReader& breader) 
{
	int is_voiced = 0;
	int vad = 0;
	switch (get_CompressionMode()) {
		case LP_WITH_SCVA:
		case MLP_WITH_SCVA:
			// the BitReader doesn't work as expected for LPS mode - might as well play it safe
			// and use the source from the decoder module (accessed by lps_mode_get_framelength_bytes)
			return lps_mode_get_framelength_bytes((char *)pdata)*8;
			break;
		case LP_NO_SCVA:
		case MLP_NO_SCVA:
			is_voiced = true;
			break;
		default:
			{
				is_voiced = breader.getbits(1);
				breader.getbits(1);
			}
			break;
	}
	if (get_CompressionMode() > 7) {
		bool debug_stop = true;
	}
	return frame_bits[get_CompressionMode() <= QP_WITH_SCVA ? get_CompressionMode() : 0][is_voiced];
}

word_t 
DssBlock::GetActualBlockFramePointer() {
	word_t bfp;
	bfp = get_FirstBlockFramePointer();
	if (bfp == 0) {
		return get_FirstBlockFramePointerFW();
	}
	return bfp;
}
byte_t 
DssBlock::GetActualNumberOfBlockFrames() {
	if (0 == get_NumberOfBlockFrames() && 255 != get_NumberOfBlockFramesFW()) {
		return get_NumberOfBlockFramesFW();
	}
	return get_NumberOfBlockFrames();
}

void 
DssBlock::ForceFillPayload(const byte_t *pData, int cBytes) {
	::memcpy(&m_pItem[SIZE_FRAMEBLOCK_HEADER], pData, min(SIZE_DSS_SECTOR-SIZE_FRAMEBLOCK_HEADER, cBytes));
}

void
DssBlock::OffsetStartByTime(double fTimeInSeconds)
{
	double fFrameLengthInSeconds = double(get_framelength_for_mode(get_CompressionMode())) / 
								   double(GetSampleRate()) ;
	long lframe_offset = long(floor(0.5 + fTimeInSeconds / fFrameLengthInSeconds));
	if (lframe_offset > GetActualNumberOfBlockFrames())
	{
		lframe_offset = GetActualNumberOfBlockFrames();
	}


	int nf = 0, dest_offset = 0, end = 0, skip_frame_offset = 0, bytes_for_frame = 0;
	int skip_bytes = 0;
	int actual_length = Length();

	int actual_block_frames = GetActualNumberOfBlockFrames();
	int offset				= GetActualBlockFramePointer() / 8; // in bytes (must be byte aligned)

	int overflowed = offset - (int)sizeof(raw_header_);

	skip_frame_offset = offset;	// skip_bytes indicates a byte offset we want in to the stream; skip_frame_offset will be frame-aligned
	end	= offset;
	BitReader breader(m_pItem, offset*8, SIZE_DSS_SECTOR, false);
	int prev_end = offset;
	for (nf = 0; nf <  actual_block_frames && end < actual_length; ++nf) 
	{
		if (nf <= lframe_offset ) { //&& ((end&0x1) == 0)
			skip_frame_offset = end;
		}
		bytes_for_frame = GetBitsForFrame(&m_pItem[end], breader)/8;
		breader.ffwd(bytes_for_frame*8-2);	//subtract the two bits that were used for vad detection
		prev_end = end;
		end += bytes_for_frame;
	}
	set_NumberOfBlockFrames(nf - (byte_t)lframe_offset);
	set_FirstBlockFramePointer(skip_frame_offset*8);
	UpdateRaw();
	// make sure that all unwritten data is set to 0xff
	if (0 == nf && end < SIZE_DSS_SECTOR) {
		::memset(&m_pItem[end],0xff,SIZE_DSS_SECTOR-end);
	}
	offset = skip_frame_offset;
	block_frame_bytes_ = end - offset;
		
}
void
DssBlock::AnalyzeBlockFrames(int overflowed, int skip_bytes, int actual_length) {
	int nf = 0, dest_offset = 0, end = 0, skip_frame_offset = 0, bytes_for_frame = 0;

	int actual_block_frames = GetActualNumberOfBlockFrames();
	int offset				= GetActualBlockFramePointer() / 8; // in bytes (must be byte aligned)

	overflowed = offset - (int)sizeof(raw_header_);

	skip_frame_offset = offset;	// skip_bytes indicates a byte offset we want in to the stream; skip_frame_offset will be frame-aligned
	end	= offset;
	BitReader breader(m_pItem, offset*8, SIZE_DSS_SECTOR, false);
	int prev_end = offset;
	for (nf = 0; nf <  actual_block_frames && end < actual_length; ++nf) 
	{
		if (end < skip_bytes && ((end&0x1) == 0)) {
			skip_frame_offset = end;
		}
		bytes_for_frame = GetBitsForFrame(&m_pItem[end], breader)/8;
		breader.ffwd(bytes_for_frame*8-2);	//subtract the two bits that were used for vad detection
		prev_end = end;
		end += bytes_for_frame;
	}
	set_NumberOfBlockFrames(nf);
	// make sure that all unwritten data is set to 0xff
	if (0 == nf && end < SIZE_DSS_SECTOR) {
		::memset(&m_pItem[end],0xff,SIZE_DSS_SECTOR-end);
	}
	offset = skip_frame_offset;
	block_frame_bytes_ = end - offset;
}
word_t 
DssBlock::ReadBlockFrames(byte_t* pDest, int dest_length, int& copied, int& overflowed, int skip_bytes)
/*
  0    ================
	   | block header |
	   |			  |
	   ----------------
	   |			  |		"overflowed" bytes of the data before the "first block frame pointer" is copied 
	   |  overflow /  | 	from immediately following the block_header, provided everything adds up and it
	   |  meta data   |		doesn't overlap with the beginning of blockframe 1
	   |			  |
	   ----------------
	   | blockframe 1 |
	   |			  |
	  ...			 ...
	   |			  |
	   ----------------
	   | blockframe 2 |
	   |			  |
	  ...			 ...
	   |			  |
	   ----------------
	   | blockframe N |		is truncated, overflows into next frame by "overflowed" bytes (= (block header + sum(frame_lengths))) - 512)
 512   ================

*/
{

	if (!pDest || !dest_length) {
		return false;
	}
	bool bProvidesNewData = false;
	bool bOverflowMismatch = false;
	int nf = 0, dest_offset = 0, end = 0, skip_frame_offset = 0, bytes_for_frame = 0;

	int actual_block_frames = GetActualNumberOfBlockFrames();
	int offset				= GetActualBlockFramePointer() / 8; // in bytes (must be byte aligned)
	int prev_overflowed = overflowed;

	// for some reason we don't count block frames/frame sizes properly in LP_WITH_SCVA mode, so just assume
	// all stuff in the block that comes after the header and before the frame_start is all speech data
	//if (get_CompressionMode() == LP_WITH_SCVA && 0 == skip_bytes) {
	//	overflowed = offset - (int)sizeof(raw_header_);
	//}
	if (overflowed != offset - 6) {
		bOverflowMismatch = true;
		overflowed = 0;
		prev_overflowed	=0;
	}
	if (overflowed > 0 && offset-(int)sizeof(raw_header_) >= overflowed) {
		DssFileFormat::WordMover(pDest, &m_pItem[sizeof(raw_header_)], overflowed);
//		::memcpy(pDest,&m_pItem[sizeof(raw_header_)],overflowed);
		dest_offset += overflowed;
		bProvidesNewData = true;
	}

	skip_frame_offset = offset;	// skip_bytes indicates a byte offset we want in to the stream; skip_frame_offset will be frame-aligned
	end	= offset;
	BitReader breader(m_pItem, offset*8, SIZE_DSS_SECTOR, false);
	int prev_end = offset;
	for (nf = 0; nf <  actual_block_frames && end < SIZE_DSS_SECTOR; ++nf) 
	{
		if (end < skip_bytes && ((end&0x1) == 0)) {
			skip_frame_offset = end;
		}
		bytes_for_frame = GetBitsForFrame(&m_pItem[end], breader)/8;
		breader.ffwd(bytes_for_frame*8-2);	//subtract the two bits that were used for vad detection
		prev_end = end;
		end += bytes_for_frame;
	}
	offset = skip_frame_offset;
	block_frame_bytes_ = end - offset;
	word_t available = min(SIZE_DSS_SECTOR,end) - offset;
	copied = available + overflowed;
	if ((available&0x1) && end < SIZE_DSS_SECTOR) {
		bool test_stop = true;
	}

	if (end > SIZE_DSS_SECTOR) {
		overflowed = end - SIZE_DSS_SECTOR;
	} else {
		overflowed = 0;
	}

	if (overflowed > 0) {
		partial_offset_ = prev_end-offset;
	}

#ifdef _DEBUG
	//TCHAR wzDebug[512];
	//wsprintf(wzDebug,L"Block %d: block_frames = %d [%d], length = %d bytes, overflow = %d bytes\n", get_SerialNum(), actual_block_frames, nf, end-offset,overflowed);
	//DbgParseLog(wzDebug);
#endif

	int nFrameBytes = available;
	if (nFrameBytes > 0 && nFrameBytes < dest_length) {
		bProvidesNewData = true;
		DssFileFormat::WordMover(&pDest[dest_offset], &m_pItem[offset], nFrameBytes);
	}
	return (bProvidesNewData ? BLOCK_HAS_DATA : 0) | (bOverflowMismatch ? BLOCK_IS_DISCONTINUOUS : 0 );
}

int DssBlock::GetSampleRate() {
	switch (this->get_CompressionMode()) 
	{
		case LP_WITH_SCVA:
		case LP_NO_SCVA:
		case MLP_WITH_SCVA:
		case MLP_NO_SCVA:
			return LP_FS;
			break;
		case QP_WITH_SCVA:
		case QP_NO_SCVA:
			return QP_FS;
			break;
		default:
		case SP_WITH_SCVA:
		case SP_NO_SCVA:
			return SP_FS;
			break;
	}
}

void DssBlock::ReduceDurationBy(int samples)
{
	int reduction_in_frames = (int)floor(0.5 + double(samples) / double(DssFileFormat::get_framelength_for_mode(int(get_CompressionMode()))));
	int block_frames = GetActualNumberOfBlockFrames();
	set_NumberOfBlockFrames((byte_t)(max(0,block_frames - reduction_in_frames)));
	set_NumberOfBlockFramesFW(0xFF); // make sure the regular value is read
	UpdateRaw();
}
void DssBlock::ReduceDurationBy(LONGLONG llRefTime)
{
	int reduction_in_samples = int((llRefTime * LONGLONG(GetSampleRate())) / UNITS);
	ReduceDurationBy(reduction_in_samples);
}

LONGLONG DssBlock::GetDurationRefTime() {
	switch (this->get_CompressionMode()) 
	{
		case LP_WITH_SCVA:
		case LP_NO_SCVA:
		case MLP_WITH_SCVA:
		case MLP_NO_SCVA:
			return (LONGLONG(this->GetActualNumberOfBlockFrames())*(LONGLONG)LP_PCM_SAMPLES*UNITS)/LP_FS;
			break;
		case QP_WITH_SCVA:
		case QP_NO_SCVA:
			return (LONGLONG(this->GetActualNumberOfBlockFrames())*(LONGLONG)QP_PCM_SAMPLES*UNITS)/QP_FS;
			break;
		default:
		case SP_WITH_SCVA:
		case SP_NO_SCVA:
			return (LONGLONG(this->GetActualNumberOfBlockFrames())*(LONGLONG)SP_PCM_SAMPLES*UNITS)/SP_FS;
			break;
	}
}

int DssBlock::GetPartialFrameOffset() 
 {
	return partial_offset_; 
}

const block_header_t& DssBlock::GetBlockHeader() 
{
			return block_header_;
}
};
