#include "stdafx_ff.h"
#include "DssElement.h"
//#include "../Include/DssLogger.h"

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

DssOptionalHeader::DssOptionalHeader(DssCryptor &cryptor) 
 : DssHeader(cryptor, DSS_OPTIONAL_HEADER)
{
	memset(&optional_header_,0xff,sizeof(optional_header_));
	optional_header_.ex_rec_length = 0;
	optional_header_.priority_status = 0;
	optional_header_.quality = 0x17; // 0�ł����������H
    
    
#warning Edit by bilal
    std::string blank("");
    
	set_OptionItemId1(blank);
	set_OptionItemId2(blank);
	set_OptionItemId3(blank);
	set_OptionItemName1(blank);
	set_OptionItemName2(blank);
	set_OptionItemName3(blank);
	set_WorkTypeId(blank);
    
	memset(optional_header_.notes, 0, sizeof(optional_header_.notes) );
	optional_header_.dss_status = 0xFB;
	optional_header_.playback_position = 0;
	UpdateRaw();
}
//========================== Accessors for optional header meta data
const dword_t			
DssOptionalHeader::get_ExRecLength(){
	return optional_header_.ex_rec_length;
}
void					
DssOptionalHeader::set_ExRecLength(dword_t v){
	optional_header_.ex_rec_length = v;
	UpdateRaw();
}
extended_index_mark_t&	
DssOptionalHeader::get_ExtendedIndexMark(int idx){
	if (idx > 15) {
		idx = 15;
	} else if (idx < 0) {
		idx = 0;
	}
	return optional_header_.ex_index_mark[idx];
}
void
DssOptionalHeader::set_ExtendedIndexMark(int idx, extended_index_mark_t &mark)
{
	if (idx < 16) {
		::memcpy(&optional_header_.ex_index_mark[idx], &mark, sizeof(extended_index_mark_t));
	}
	UpdateRaw();
}
udword_t 
DssOptionalHeader::get_WMAIndexMark(int idx) {
	if (idx > 35) {
		idx = 35;
	} else if (idx < 0) {
		idx = 0;
	}
	return ((udword_t *)&optional_header_.ex_index_mark)[idx];
}
const byte_t			
DssOptionalHeader::get_Quality(){
	return optional_header_.quality;
}
void					
DssOptionalHeader::set_Quality(byte_t v){
	optional_header_.quality = v;
	UpdateRaw();
}
const std::string&		
DssOptionalHeader::get_WorkTypeId(){
	RETURN_STRING_REF(work_type_id, work_type_id_, 16)
}
void					
DssOptionalHeader::set_WorkTypeId(const std::string &v) {
	SET_FROM_STRING_REF(work_type_id, 16);
}
const std::string&		
DssOptionalHeader::get_OptionItemName1(){
	RETURN_STRING_REF(option_item_name1, option_item_name1_,8)
}
void					
DssOptionalHeader::set_OptionItemName1(const std::string &v){
	SET_FROM_STRING_REF(option_item_name1, 8);
}
const std::string&		
DssOptionalHeader::get_OptionItemId1(){
	RETURN_STRING_REF(option_item_id1, option_item_id1_,20)
}
void					
DssOptionalHeader::set_OptionItemId1(const std::string &v){
	SET_FROM_STRING_REF(option_item_id1, 20);
}
const std::string&		
DssOptionalHeader::get_OptionItemName2(){
	RETURN_STRING_REF(option_item_name2, option_item_name2_,8)
}
void					
DssOptionalHeader::set_OptionItemName2(const std::string &v){
	SET_FROM_STRING_REF(option_item_name2, 8);
}
const std::string&		
DssOptionalHeader::get_OptionItemId2(){
	RETURN_STRING_REF(option_item_id2, option_item_id2_,20)
}
void					
DssOptionalHeader::set_OptionItemId2(const std::string &v){
	SET_FROM_STRING_REF(option_item_id2, 20);
}
const std::string&		
DssOptionalHeader::get_OptionItemName3(){
	RETURN_STRING_REF(option_item_name3, option_item_name3_,8)
}
void					
DssOptionalHeader::set_OptionItemName3(const std::string &v){
	SET_FROM_STRING_REF(option_item_name3, 8);
}
const std::string&		
DssOptionalHeader::get_OptionItemId3(){
	RETURN_STRING_REF(option_item_id3, option_item_id3_,20)
}
void					
DssOptionalHeader::set_OptionItemId3(const std::string &v){
	SET_FROM_STRING_REF(option_item_id3, 20);
}
const byte_t			
DssOptionalHeader::get_DssStatus(){
	return optional_header_.dss_status;
}
void					
DssOptionalHeader::set_DssStatus(byte_t v){
	optional_header_.dss_status = v;
	UpdateRaw();
}
const byte_t			
DssOptionalHeader::get_PriorityStatus(){
	return optional_header_.priority_status;
}
void					
DssOptionalHeader::set_PriorityStatus(byte_t v){
	optional_header_.priority_status = v;
	UpdateRaw();
}
const dword_t			
DssOptionalHeader::get_PlaybackPosition(){
	return optional_header_.playback_position;
}
void					
DssOptionalHeader::set_PlaybackPosition(dword_t v){
	optional_header_.playback_position = v;
	UpdateRaw();
}

void 
DssOptionalHeader::set_Notes(const std::string& v) {
	SET_FROM_STRING_REF(notes, 100);
}
const std::string&
DssOptionalHeader::get_Notes() {
	RETURN_STRING_REF(notes, notes_, 100)
}

//============================== Parser for optional header
bool
DssOptionalHeader::Parse(const byte_t* pData, LONG cBytes, bool is_encrypted)
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

// 2009/02/24 Motoyama wave��dss header��ʂ����߂ɉ��L�̏������O�����B
//	// Bit of an arbitrary test to discriminate optional headers from other (unknown) non-standard headers. 
//	// Imperfect
//	if (!(get_PriorityStatus() == 0 || get_PriorityStatus() == 1) ||
//		optional_header_.quality > 0x20 )
//	{
//		return false;
//	}
	m_cHdr = bytes_used;
	m_elementId = DSS_FILE_HEADER;

	get_WorkTypeId();
	get_OptionItemName1();
	get_OptionItemId1();
	get_OptionItemName2();
	get_OptionItemId2();
	get_OptionItemName3();
	get_OptionItemId3();
	return true;
}

void 
DssOptionalHeader::UpdateRaw() {
	// 2009/02/24 Motoyama
	// file_header_t �\���̂̃A���C�����g��8�ׁ̈A
	// file_header_t �̐��m�ȃT�C�Y�𓾂邱�Ƃ��o���Ȃ��B
	// sizeof(file_header_t)�� 136�Bfile_header_t�̗v�f�����v�����134�ɂȂ�B
	// sizeof(optional_header_t)�� 388 optional_header_t�̗v�f�����v�����386�ɂȂ�B
	// �����ł�reserved���󂳂Ȃ����ߒ��ڒl���w�肷��B
	::memcpy(m_pItem, &optional_header_,/*sizeof(optional_header_)*/386);
}

};
