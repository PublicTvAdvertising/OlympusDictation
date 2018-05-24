#include "stdafx_ff.h"
#include "DssCryptor.h"
#include "../DSSAES/DSSAES.h"
#include <string>

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

#pragma warning(disable : 4312)		// warning on cast of DWORD hMod to larger size HMODULE -- not a problem

#ifndef min
#define min(a,b)			(((a) < (b)) ? (a) : (b))
#endif
namespace DssFileFormat {

	DssCryptor DssCryptor::operator= (const DssCryptor& o)
	{
//		this->crypt_state_ = o.crypt_state_;
		::memcpy(this->password_, o.password_, sizeof(password_));
		::memcpy(this->salt_, o.salt_, sizeof(salt_));
		::memcpy(this->verification_key_, o.verification_key_, sizeof(verification_key_));
		this->is_salt_set_ = o.is_salt_set_;
		this->crypt_version_ = o.crypt_version_;
//		AESCopyState(o.aes_state_, &aes_state_);
		return *this;
	}
	DssCryptor::DssCryptor() 
		: //aes_present_(true)
//		, aes_handle_(0)
		//, aes_GenerateSalt(0)
		//, aes_InitEncrypt(0)
		//, aes_InitDecrypt(0)
		//, aes_Encrypt(0)
		//, aes_Decrypt(0)
		/*, */
		  crypt_state_(WAITING)
		, aes_state_(0)
		, is_salt_set_(false)
		, crypt_version_(1)
	{
		::memset(password_,0,sizeof(password_));
		::memset(salt_,0xff,sizeof(salt_));
		::memset(verification_key_,0xff,sizeof(verification_key_));

		//MEMORY_BASIC_INFORMATION mbi;
		//static int dummyVariable;
		//VirtualQuery( &dummyVariable, &mbi, sizeof(mbi) );
		//DWORD hMod = (DWORD)(mbi.AllocationBase);
		//char szModule[MAX_PATH];
		//GetModuleFileNameA( reinterpret_cast<HMODULE>(hMod), szModule, sizeof(szModule) ); 
		//std::string str_path(szModule);
		//szModule[str_path.find_last_of("\\")+1] = 0;
	}

	DssCryptor::~DssCryptor() {
		if (aes_state_) {
			AESFree(aes_state_);
			aes_state_ = 0;
		}
	}

	void DssCryptor::init_decryption(const unsigned char *salt, const unsigned char *verification_key, const char *password, const unsigned int crypt_version) {

		if (crypt_version == 1 || crypt_version == 2) {
			crypt_version_ = crypt_version;
		}

		if (password) {
			::memset(password_,0,sizeof(password_));
			::memcpy(password_,password,min(sizeof(password_)-1,strlen(password)));
		}
		::memcpy(verification_key_,verification_key,4);
		::memcpy(salt_,salt,16);
		crypt_state_ = WAITING;
	}

	/**
	* Initialize the encryption
	* \param password - password to encrypt with (if blank, use already set password)
	* \param salt - salt to use for encryption. if blank, generate the salt.
	*/
	void DssCryptor::init_encryption(const char *password = 0, const char *salt = 0, const unsigned int crypt_version = 0) {
		
		if (crypt_version == 1 || crypt_version == 2) {
			crypt_version_ = crypt_version;
		}

		if (password) {
			::memset(password_,0,sizeof(password_));
			::memcpy(password_,password,min(sizeof(password_)-1,strlen(password)));
		}
		bool bSuccess = true;
		if (salt) {
			::memcpy(salt_, salt, 16);
			is_salt_set_ = true;
		}
		else if (!is_salt_set_) {
			bSuccess = AESGenerateSalt(password_, salt_);
		}
		if (bSuccess && crypt_state_ != ENCRYPTING) {
			is_salt_set_ = true;
			if (aes_state_) {
				AESFree(aes_state_);
				aes_state_ = 0;
			}
			if (AESInitEncrypt(password_, salt_, crypt_version_, verification_key_, &aes_state_)){
				crypt_state_ = ENCRYPTING;
			}
		}
	}
	void DssCryptor::reset() 
	{
		if (aes_state_) {
			AESFree(aes_state_);
			aes_state_ = 0;
		}
		::memset(password_,0,sizeof(password_));
		::memset(salt_,0,16);
		::memset(verification_key_,0,4);
		is_salt_set_ = false;
		crypt_state_ = WAITING;
	}
	void DssCryptor::get_salt(unsigned char *psalt, int salt_len) 
	{
		::memcpy(psalt, salt_, min(salt_len, 16));
	}
	void DssCryptor::get_verification_key(unsigned char *pkey, int key_len) 
	{
		::memcpy(pkey, verification_key_, min(key_len, 4));
	}
	void DssCryptor::get_crypt_version(unsigned int* version)
	{
		*version = crypt_version_;
	}
	void DssCryptor::set_password(const char *password, size_t len) {
			::memset(password_,0,sizeof(password_));
			::memcpy(password_,password,min(sizeof(password_)-1,len));
	};
	void DssCryptor::set_crypt_version(const unsigned int version) 
	{
		crypt_version_ = version;
	}

	bool DssCryptor::encrypt_block(byte_t *block, int nbytes) {
		if (crypt_state_ != ENCRYPTING) {
			init_encryption();
		}
		if (ENCRYPTING == crypt_state_) {
			byte_t *data = &block[SIZE_FRAMEBLOCK_HEADER];
			const int crypt_bytes = (SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER - 10);
			// as per documentation, leave last 10 bytes alone so that we process a whole number of words
			swap_words(data,crypt_bytes);
			if (AESEncrypt(aes_state_, (unsigned char*)data, crypt_bytes, crypt_version_)) {
				swap_words(data,crypt_bytes);
				return true;
			}
			return false;
		}
		else {
			return false;

		}
	}
	bool DssCryptor::decrypt_block(byte_t *block, int nbytes) {
		if (crypt_state_ != DECRYPTING) {
			unsigned char	verify_chk[4];
			if (aes_state_) {
				AESFree(aes_state_);
			}

			if (AESInitDecrypt(password_, salt_, crypt_version_, verify_chk, &aes_state_)){
				if (is_verified(verify_chk,verification_key_)) {
					crypt_state_ = DECRYPTING;
				}
			}
		}
		if (DECRYPTING == crypt_state_) {
			byte_t *data = &block[SIZE_FRAMEBLOCK_HEADER];
			const int crypt_bytes = (SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER - 10);
			// as per documentation, leave last 10 bytes alone so that we process a whole number of words
			swap_words(data,crypt_bytes);
			if (AESDecrypt(aes_state_, (unsigned char*)data,crypt_bytes, crypt_version_)) {
				swap_words(data,crypt_bytes);
				return true;
			}
			return false;
		}
		else {
			return false;

		}
	}

};