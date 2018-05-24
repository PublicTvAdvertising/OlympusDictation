#ifndef DSS_ENCRYPTOR_H__
#define DSS_ENCRYPTOR_H__

#include "DssFileFormat.h"

namespace DssFileFormat {

	class DssCryptor 
	{

	private:
#if 0
		typedef BOOL (WINAPI * FPAESGenerateSalt)(char *, unsigned char *);
		typedef BOOL (WINAPI * FPAESInitEncrypt)(char *, unsigned char *, unsigned char *);
		typedef BOOL (WINAPI * FPAESInitDecrypt)(char *, unsigned char *, unsigned char *);
		typedef BOOL (WINAPI * FPAESEncrypt)(unsigned char *, int );
		typedef BOOL (WINAPI * FPAESDecrypt)(unsigned char *, int );
#endif
		typedef enum {
			WAITING,
			DECRYPTING,
			ENCRYPTING
		} state_t;

	public:
		DssCryptor();
		~DssCryptor();
//		DssCryptor(const DssCryptor& o);
		DssCryptor operator= (const DssCryptor& o);

#if 0
		static DssCryptor& get_instance() {
			static DssCryptor self_;
			return self_;
		}
#endif
//		bool is_aes_present();
		void set_password(const char *password, size_t len = 0);
		void set_crypt_version(const unsigned int version = 1) ;
		void init_decryption(const unsigned char *salt, const unsigned char *verification_key, const char *password = 0, const unsigned int crypt_version = 0);
		void init_encryption(const char *password, const char *salt, const unsigned int crypt_version);
		void reset();
		void get_salt(unsigned char *psalt, int salt_len);
		void get_verification_key(unsigned char *pkey, int key_len);
		void get_crypt_version(unsigned int* version);
		bool decrypt_block(byte_t *data, int bytes);
		bool encrypt_block(byte_t *data, int bytes);

		static bool check_password(const char *pPass) {
			if (0 == pPass) {
				return false;
			} 
			if (strlen(pPass) < 4) {
				return false;
			}
			if (strlen(pPass) > 16)  {
				return false;
			}
			return true;
		}
		

	protected:
		bool is_verified(byte_t* ver1, byte_t *ver2) {
			if (*((word_t*)ver1) == *((word_t*)ver2)) {
				return true;
			}
			return false;
		}
		void swap_words(byte_t* buf, unsigned int len)
		{
			byte_t tmp;
			unsigned int i = 0;
			while (i < len) {
				tmp			= buf[i];
				buf[i]		= buf[i+1];
				buf[i+1]	= tmp;
				i += 2 ;
			}
		}


	protected:
		char				password_[17];
		unsigned char		salt_[16];
		bool				is_salt_set_;
		unsigned char		verification_key_[5];

		unsigned int		crypt_version_;

		state_t				crypt_state_;
		void				*aes_state_;
//		HMODULE				aes_handle_;
//		bool				aes_present_;
//		FPAESGenerateSalt	aes_GenerateSalt;
//		FPAESInitEncrypt	aes_InitEncrypt;
//		FPAESInitDecrypt	aes_InitDecrypt;
//		FPAESEncrypt		aes_Encrypt;
//		FPAESDecrypt		aes_Decrypt;
	};

};

#endif