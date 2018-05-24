#include "stdafx_ff.h"
#include "DssElement.h"

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

// helper macros for accessing strings in the header structure, which are not null terminated
#define RETURN_STRING_REF(member,store,len) 	ascii_t id[len+1]; \
												memset(id,0,sizeof(id));\
												::memcpy(id,optional_header_.member,len);\
												store = std::string(id);\
												return store;

#define SET_FROM_STRING_REF(member, len)		memset(&optional_header_.member,0,len); \
												::memcpy(&optional_header_.member, v.c_str(), std::min<size_t>(v.length(),len)); \
												UpdateRaw();



namespace DssFileFormat {

DssProOptionalHeader::DssProOptionalHeader(DssCryptor &cryptor) 
 : DssHeader(cryptor, DSS_PRO_OPTIONAL_HEADER)
{
	memset(&optional_header_,0xff,sizeof(optional_header_));
	optional_header_.ex_rec_length = 0;
	optional_header_.priority_status = 0;
	optional_header_.quality = 0x7;
	optional_header_.playback_position = 0;
	memset(&optional_header_.notes, 0, sizeof(optional_header_.notes));
	UpdateRaw();
}
    /* Accessor to set Quality  and worktype   */
void
DssProOptionalHeader::set_QualityAndWorkType(int mode,std::string workType){
    
    if (mode == 0 || mode == 1)
        optional_header_.quality = 0x0;//SP
    else
        optional_header_.quality = 0x7;//QP
    
    strcpy(optional_header_.work_type_id, workType.c_str());
    
    
    UpdateRaw();
}
//========================== Accessors for optional header meta data
const dword_t			
DssProOptionalHeader::get_ExRecLength(){
	return optional_header_.ex_rec_length;
}
void					
DssProOptionalHeader::set_ExRecLength(dword_t v){
	optional_header_.ex_rec_length = v;
	UpdateRaw();
}
const byte_t			
DssProOptionalHeader::get_Quality(){
	return optional_header_.quality;
}
void					
DssProOptionalHeader::set_Quality(byte_t v){
	/*optional_header_.quality = v;
	UpdateRaw();*/
}
const std::string&		
DssProOptionalHeader::get_WorkTypeId(){
	RETURN_STRING_REF(work_type_id, work_type_id_, 16)
}
void					
DssProOptionalHeader::set_WorkTypeId(const std::string &v) {
	SET_FROM_STRING_REF(work_type_id, 16);
}

const std::string&		
DssProOptionalHeader::get_OptionItemName(size_t idx)
{
	if (idx > 10 || idx < 1) {
		idx = 1;
	}
	idx--;						//items are numbered from 1, but the array is indexed from zero

	ascii_t name[16+1];
	memset(name,0,sizeof(name));
	::memcpy(name,optional_header_.optional_items[idx].option_item_name, 16);
	option_item_name_[idx] = std::string(name);
	return option_item_name_[idx];
}

const std::string&
DssProOptionalHeader::get_OptionItemId(size_t idx)
{
	if (idx > 10 || idx < 1) {
		idx = 1;
	}
	idx--;						//items are numbered from 1, but the array is indexed from zero

	ascii_t id[20+1];
	memset(id,0,sizeof(id));
	::memcpy(id,optional_header_.optional_items[idx].option_item_id, 20);
	option_item_id_[idx] = std::string(id);
	return option_item_id_[idx];
}

const byte_t			
DssProOptionalHeader::get_PriorityStatus(){
	return optional_header_.priority_status;
}
void					
DssProOptionalHeader::set_PriorityStatus(byte_t v){
	optional_header_.priority_status = v;
	UpdateRaw();
}
const dword_t			
DssProOptionalHeader::get_PlaybackPosition(){
	return optional_header_.playback_position;
}
void					
DssProOptionalHeader::set_PlaybackPosition(dword_t v){
	optional_header_.playback_position = v;
	UpdateRaw();
}

const std::string&			
DssProOptionalHeader::get_Notes(){
	return this->notes_;
}
void					
DssProOptionalHeader::set_Notes(const std::string& v){
	this->notes_ = v;
	SET_FROM_STRING_REF(notes, 100);
	UpdateRaw();
}

//============================== Parser for optional header
bool
DssProOptionalHeader::Parse(const byte_t* pData, LONG cBytes, bool is_encrypted)
{
	// if we have less than a full sector, assume this is an irregular header and don't use the element parser
	if (cBytes < SIZE_DSS_SECTOR) {
		::memcpy((void *)(&m_pItem[0]),pData,cBytes);
		m_cBytes = cBytes;
	} else {
		if (!DssElement::Parse(pData, cBytes)) 
			return false;
	}

	if (cBytes < sizeof(optional_header_))
		return false;

	int bytes_used = 0;
	::memcpy_s(&optional_header_, sizeof(optional_header_), pData, min(sizeof(optional_header_),cBytes));
	bytes_used += min(sizeof(optional_header_),cBytes);

	// Bit of an arbitrary test to discriminate optional headers from other (unknown) non-standard headers. 
	// Imperfect
#if 0
	if (!(get_PriorityStatus() == 0 || get_PriorityStatus() == 1) ||
		optional_header_.quality > 0x20 )
	{
		return false;
	}
#endif
	m_cHdr = bytes_used;
	m_elementId = DSS_FILE_HEADER;

	get_WorkTypeId();
	for (size_t n=1; n<=10; ++n) {
		get_OptionItemName(n);
		get_OptionItemId(n);
	}
	return true;
}

void 
DssProOptionalHeader::UpdateRaw() {
	::memcpy(m_pItem, &optional_header_,sizeof(optional_header_));
}

DssProOptionalHeader2::DssProOptionalHeader2(DssCryptor& cryptor) 
 : DssHeader(cryptor, DSS_PRO_OPTIONAL_HEADER2)
{
	::memset(&optional_header_2_,0xff,sizeof(optional_header_2_));
	UpdateRaw();
}

//============================== Parser for optional header
bool
DssProOptionalHeader2::Parse(const byte_t* pData, LONG cBytes, bool is_encrypted)
{
	// if we have less than a full sector, assume this is an irregular header and don't use the element parser
	if (!DssElement::Parse(pData, cBytes)) 
		return false;

	if (cBytes < sizeof(SIZE_DSS_SECTOR))
		return false;

	int bytes_used = 0;
	for (int n=0; n < 32; ++n) 
	{
		word_t	pack[6];
		::memcpy(pack, &m_pItem[n*sizeof(pack)], sizeof(pack));
		optional_header_2_.verbal_comment[n].start_offset.sector = (dword_t(pack[1])<<16) | dword_t(pack[0]);
		optional_header_2_.verbal_comment[n].start_offset.frame = pack[2];
		optional_header_2_.verbal_comment[n].end_offset.sector = (dword_t(pack[4])<<16) | dword_t(pack[3]);
		optional_header_2_.verbal_comment[n].end_offset.frame = pack[5];
	}

	m_cHdr = bytes_used = 12*32;
	m_elementId = DSS_FILE_HEADER;
	return true;
}

void
DssProOptionalHeader2::UpdateRaw()
{
	for (int n=0; n < 32; ++n) 
	{
		word_t	pack[6];
		pack[0] = 0xFFFF & optional_header_2_.verbal_comment[n].start_offset.sector;
		pack[1] = optional_header_2_.verbal_comment[n].start_offset.sector >> 16;
		pack[2] = optional_header_2_.verbal_comment[n].start_offset.frame;
		pack[3] = 0xFFFF & optional_header_2_.verbal_comment[n].end_offset.sector;
		pack[4] = optional_header_2_.verbal_comment[n].end_offset.sector >> 16;
		pack[5] = optional_header_2_.verbal_comment[n].end_offset.frame;
		::memcpy(&m_pItem[n*sizeof(pack)], pack, sizeof(pack));
	}
}
const pro_verbal_comment_t& DssProOptionalHeader2::get_VerbalComment(size_t idx)
{
	if (idx < 1 || idx > 32) {
		idx = 1;
	}
	idx --;
	return optional_header_2_.verbal_comment[idx];
}

void DssProOptionalHeader2::set_VerbalComment(size_t idx, const pro_verbal_comment_t& comment)
{
	if (idx < 1 || idx > 32) {
		return;
	}
	idx--;
	::memcpy(&optional_header_2_.verbal_comment[idx], &comment, sizeof(pro_verbal_comment_t));
	UpdateRaw();
}
void DssProOptionalHeader2::set_VerbalComment(size_t idx, int start_sector, int start_frame, int end_sector, int end_frame)
{
	if (idx < 1 || idx > 32) {
		return;
	}
	idx--;
	pro_verbal_comment_t	range;
	if (-1 == start_sector && -1 == start_frame && -1 == end_sector && -1 == end_frame) {
		memset(&range, 0xff, sizeof(range));
	}
	else {
		range.start_offset.sector = start_sector;
		range.start_offset.frame = start_frame;
		range.end_offset.sector = end_sector;
		range.end_offset.frame = end_frame;
	}
	::memcpy(&optional_header_2_.verbal_comment[idx], &range, sizeof(range));
	UpdateRaw();
}

};
