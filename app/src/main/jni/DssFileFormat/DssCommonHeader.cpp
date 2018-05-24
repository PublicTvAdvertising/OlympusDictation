#include "stdafx_ff.h"

#include "DssElement.h"
#include "DssCryptor.h"
//#include "../Include/DssLogger.h"

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


// helper macros for accessing strings in the header structure, which are not null terminated
#define RETURN_STRING_REF(member,store,len) 	ascii_t id[len+1]; \
												memset(id,0,sizeof(id));\
												::memcpy(id,header_.member,len);\
												store = std::string(id);\
												return store;

#define SET_FROM_STRING_REF(member, len)		memset(&header_.member,0 ,len); \
												::memcpy(&header_.member, v.c_str(), v.length());

namespace DssFileFormat {

#ifdef _DEBUG
static int gdbg_num_headers = 0;
static int gdbg_header_serial = 0;
#endif

DssHeader::DssHeader(DssCryptor &cryptor, DssFileFormat::DSS_HEADER_TYPE htype)
 : DssElement(cryptor)
 , header_type_(htype) 
{
#ifdef _DEBUG
	++gdbg_num_headers;
	serial_no = gdbg_header_serial++;
#endif
	m_cBytes = (SIZE_DSS_SECTOR);
}
DssHeader::~DssHeader()
{
#ifdef _DEBUG
//	wchar_t szDebug[1024];
//	::wsprintf(szDebug,L"DssHeader:: Deleting %d th header (serial = %d).\n", gdbg_num_headers, serial_no);
////	::OutputDebugString(szDebug);
//	--gdbg_num_headers;
#endif
}

DssCommonHeader::DssCommonHeader(DssCryptor &cryptor)
	: DssHeader(cryptor, DSS_COMMON_HEADER) 
{
	::memset(&header_, 0xff, sizeof(header_));
	::memset(&header_ds2_, 0xff, sizeof(header_ds2_));
	::memset(&header_enc_, 0xff, sizeof(header_enc_));
	set_TypistId("");
	set_AuthorId(std::string("ODDSHOW"));
	::memcpy(header_.self_identifier, "dss", 3);
	header_.version_id_dss = 0x1;
	header_.release_id_dss = 0x2;
	header_.licensee_id = 0x2;
	header_.object_word = 0xFFFE;
	header_.process_word = 0xFFFE;
	header_.status_word = 0xFFF7;
	header_.priority_level = 0x7;
	UpdateRaw();
}
DssCommonHeader::~DssCommonHeader() {
}

void
DssCommonHeader::InitEncryption(const char *szPassword, const char *szSalt, const word_t encVersion) {
	m_Cryptor.init_encryption(szPassword, szSalt, encVersion);
	m_Cryptor.get_salt((byte_t *)&header_enc_.encryption_salt[0], 16);
	m_Cryptor.get_verification_key((byte_t *)&header_enc_.verification_value[0], 4);
	header_enc_.encryption_version = encVersion;//todo hiramatsu
	set_SelfIdentifier("enc");
};
void 
DssCommonHeader::UpdateRaw() {
	if (get_SelfIdentifier().compare("ds2") == 0 ||
		get_SelfIdentifier().compare("enc") == 0) 
	{
		header_.release_id_dss = 3;
	}
	::memcpy(m_pItem, &header_, sizeof(header_));
	if (get_SelfIdentifier().compare("ds2") == 0 ||
		get_SelfIdentifier().compare("enc") == 0) 
	{

		::memcpy(&m_pItem[SIZE_DSS_HEADER], &header_ds2_, sizeof(header_ds2_));
		if (get_SelfIdentifier().compare("enc") == 0) {
			::memcpy(&m_pItem[SIZE_DSS_HEADER+SIZE_DS2_HEADER], &header_enc_, sizeof(header_enc_));
		}
	}
}

//========================== Accessors

const byte_t		
DssCommonHeader::get_HeaderBlockNum() {
	return int(header_.number_of_header_blocks);
}
void
DssCommonHeader::set_HeaderBlockNum(byte_t val) {
	header_.number_of_header_blocks = val;
}
const std::string&	
DssCommonHeader::get_SelfIdentifier() {
	RETURN_STRING_REF(self_identifier, self_id_,3)
}
void
DssCommonHeader::set_SelfIdentifier(const std::string &v) {
	if (0 == v.compare("dss"))
	{
		SET_FROM_STRING_REF(self_identifier, 3);
		this->set_VersionId(1);
		this->set_ReleaseId(2);
	}
	else if (0 == v.compare("ds2")) 
	{
		SET_FROM_STRING_REF(self_identifier, 3);
		this->set_VersionId(2);
		this->set_ReleaseId(3);
	} 
	else if (0 == v.compare("enc")) {
		SET_FROM_STRING_REF(self_identifier, 3);
		this->set_VersionId(2);
		this->set_ReleaseId(3);
	}
	UpdateRaw();
}
const word_t		
DssCommonHeader::get_VersionId(){
	return header_.version_id_dss;
}
void 
DssCommonHeader::set_VersionId(word_t v) {
	header_.version_id_dss = v;
	UpdateRaw();
}
const word_t		
DssCommonHeader::get_ReleaseId(){
	return header_.release_id_dss;
}
void
DssCommonHeader::set_ReleaseId(word_t v) {
	header_.release_id_dss = v;
	UpdateRaw();
}
const dword_t		
DssCommonHeader::get_LicenseeId(){
	return header_.licensee_id;
}
const std::string&	
DssCommonHeader::get_AuthorId(){
	RETURN_STRING_REF(author_id, author_id_,16)
}
void				
DssCommonHeader::set_AuthorId(const std::string &v) {
	SET_FROM_STRING_REF(author_id, 16);
	UpdateRaw();
}
const dword_t		
DssCommonHeader::get_JobNumber() {
	return header_.job_number;
}
void				
DssCommonHeader::set_JobNumber(dword_t v){
	header_.job_number = v;
	UpdateRaw();
}
const word_t		
DssCommonHeader::get_ObjectWord(){
	return header_.object_word;
}
void				
DssCommonHeader::set_ObjectWord(word_t v){
	header_.object_word = v;
	UpdateRaw();
}
const word_t		
DssCommonHeader::get_ProcessWord(){
	return header_.process_word;
}
void				
DssCommonHeader::set_ProcessWord(word_t v){
	header_.process_word = v;
	UpdateRaw();
}
const word_t		
DssCommonHeader::get_StatusWord(){
	return header_.status_word;
}
void				
DssCommonHeader::set_StatusWord(word_t v){
	header_.status_word = v;
	UpdateRaw();
}
const std::string&	
DssCommonHeader::get_RecordingStartDate(){
	RETURN_STRING_REF(record_start_date, rec_start_date_,6)
}
void				
DssCommonHeader::set_RecordingStartDate(const std::string &v){
	SET_FROM_STRING_REF(record_start_date, 6);
	UpdateRaw();
}
const std::string&	
DssCommonHeader::get_RecordingStartTime(){
	RETURN_STRING_REF(record_start_time, rec_start_time_,6)
}
void				
DssCommonHeader::set_RecordingStartTime(const std::string &v){
	SET_FROM_STRING_REF(record_start_time, 6);
	UpdateRaw();
}
const std::string&	
DssCommonHeader::get_RecordingEndDate(){
	RETURN_STRING_REF(record_end_date,rec_end_date_,6)
}
void				
DssCommonHeader::set_RecordingEndDate(const std::string &v){
	SET_FROM_STRING_REF(record_end_date, 6);
	UpdateRaw();
}
const std::string&	
DssCommonHeader::get_RecordingEndTime(){
	RETURN_STRING_REF(record_end_time,rec_end_time_,6)
}
void				
DssCommonHeader::set_RecordingEndTime(const std::string &v){
	SET_FROM_STRING_REF(record_end_time, 6);
	UpdateRaw();
}
const std::string&	
DssCommonHeader::get_LengthOfRecording(){
	RETURN_STRING_REF(length_of_recording, rec_length_, 6);
}
void				
DssCommonHeader::set_Length(const std::string &v){
	SET_FROM_STRING_REF(length_of_recording, 6);
	UpdateRaw();
}
const byte_t		
DssCommonHeader::get_AttributeFlag(){
	return header_.attribute_flag;
}
void				
DssCommonHeader::set_AttributeFlag(byte_t v){
	header_.attribute_flag = v;
	UpdateRaw();
}
const byte_t		
DssCommonHeader::get_PriorityLevel(){
	return header_.priority_level;
}
void				
DssCommonHeader::set_PriorityLevel(byte_t v){
	header_.priority_level = v;
	UpdateRaw();
}
const std::string&	
DssCommonHeader::get_TypistId(){
	RETURN_STRING_REF(transcriptionist_id, typist_id_, 16);
}
void				
DssCommonHeader::set_TypistId(const std::string &v){
	SET_FROM_STRING_REF(transcriptionist_id, 16);
	UpdateRaw();
}
const byte_t*			
DssCommonHeader::get_IMark(){
	return header_.instruction_mark;
}
void				
DssCommonHeader::set_IMark(const byte_t* pv){
	::memcpy(header_.instruction_mark,pv,sizeof(header_.instruction_mark));
	UpdateRaw();
}
const byte_t*			
DssCommonHeader::get_AdditionalIMark(){
	return header_ds2_.additional_instruction_mark;
}
void				
DssCommonHeader::set_AdditionalIMark(const byte_t* pv){
	size_t srcsize = sizeof(header_ds2_.additional_instruction_mark);
	::memcpy(header_ds2_.additional_instruction_mark,pv,srcsize);
	UpdateRaw();
}
const word_t
DssCommonHeader::get_EncryptionVersion() {
	if (IsEncrypted()) {
		return header_enc_.encryption_version;
	}
	return 0;
}
void
DssCommonHeader::set_EncryptionVersion(word_t ver) {
	header_enc_.encryption_version = ver;
	UpdateRaw();
}
const byte_t*
DssCommonHeader::get_SaltValue() {
	return header_enc_.encryption_salt;
}
void
DssCommonHeader::set_SaltValue(byte_t* val) {
	::memcpy(header_enc_.encryption_salt,val,sizeof(header_enc_.encryption_salt));
	UpdateRaw();
}
const byte_t*
DssCommonHeader::get_VerificationValue() {
	return header_enc_.verification_value;
}
void
DssCommonHeader::set_VerificationValue(byte_t *val) {
	::memcpy(header_enc_.verification_value, val, sizeof(header_enc_.verification_value));
	UpdateRaw();
}

const LONGLONG 
DssCommonHeader::GetLengthReferenceTime() 
{
	return convert_hhmmss_to_reftime(get_LengthOfRecording().c_str());
}

//========================== Parse the common file header
bool
DssCommonHeader::Parse(const byte_t* pData, LONG cBytes, bool is_encrypted)
{
	if (SIZE_DSS_WMA_COMMON_HEADER == cBytes) {
		::memcpy((void *)(&m_pItem[0]),pData,SIZE_DSS_WMA_COMMON_HEADER);
		m_cBytes = SIZE_DSS_WMA_COMMON_HEADER;
	}
	else if (!DssElement::Parse(pData, cBytes)) {
		return false;
	}

	if (cBytes < sizeof(header_))
		return false;

	int bytes_used = 0;
	::memcpy(&header_, pData, min(SIZE_DSS_HEADER,cBytes));
	bytes_used += min(SIZE_DSS_HEADER,cBytes);
	
	if (!get_SelfIdentifier().compare("dss"))
	{
	}
	else if (!get_SelfIdentifier().compare("ds2") || !get_SelfIdentifier().compare("enc"))
	{
		::memcpy(&header_ds2_,&pData[bytes_used], min(SIZE_DS2_HEADER, cBytes-bytes_used));
		bytes_used += min(SIZE_DS2_HEADER, cBytes-bytes_used);
		if (!get_SelfIdentifier().compare("enc"))
		{
			::memcpy(&header_enc_,&pData[bytes_used], min(SIZE_ENC_HEADER, cBytes-bytes_used));
			bytes_used += min(SIZE_ENC_HEADER, cBytes-bytes_used);
			m_Cryptor.init_decryption(header_enc_.encryption_salt, header_enc_.verification_value, 0, header_enc_.encryption_version);
		}
	}
	else {
		return false;
	}

	m_cHdr = bytes_used;
	m_elementId = DSS_FILE_HEADER;

	get_AuthorId();
	get_RecordingStartDate();
	get_RecordingStartTime();
	get_RecordingEndDate();
	get_RecordingEndTime();
	get_LengthOfRecording();
	return true;
}

bool DssCommonHeader::IsEncrypted() 
{
	return !get_SelfIdentifier().compare("enc");
}

const file_header_t& DssCommonHeader::GetFileHeader() {
			return header_;
}
};
