/*
 ---------------------------------------------------------------------------
 Copyright (c) 2002, Dr Brian Gladman <                 >, Worcester, UK.
 All rights reserved.

 LICENSE TERMS

 The free distribution and use of this software in both source and binary
 form is allowed (with or without changes) provided that:

   1. distributions of this source code include the above copyright
      notice, this list of conditions and the following disclaimer;

   2. distributions in binary form include the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other associated materials;

   3. the copyright holder's name is not used to endorse products
      built using this software without specific written permission.

 ALTERNATIVELY, provided that this notice is retained in full, this product
 may be distributed under the terms of the GNU General Public License (GPL),
 in which case the provisions of the GPL apply INSTEAD OF those given above.

 DISCLAIMER

 This software is provided 'as is' with no explicit or implied warranties
 in respect of its properties, including, but not limited to, correctness
 and/or fitness for purpose.
 -------------------------------------------------------------------------
 Issue Date: 24/01/2003

 This file implements password based file encryption and authentication 
 using AES in CTR mode, HMAC-SHA1 authentication and RFC2898 password 
 based key derivation.

*/

#include <memory.h>

#include "fileenc.h"
#include "aesopt.h"

#if defined(__cplusplus)
extern "C"
{
#endif

/* subroutine for data encryption/decryption    */
/* this could be speeded up a lot by aligning   */
/* buffers and using 32 bit operations          */

static void encr_data(unsigned char data[], unsigned long d_len, fcrypt_ctx cx[1])
{
	//modified for 64 bit
	unsigned long i = 0;
	ULONG pos = cx->encr_pos;

	// 2007/04/16 Added.
	unsigned char kbuf[MAX_KEY_LENGTH];
    const aes_32t  *kp = cx->encr_ctx->k_sch + nc * cx->encr_ctx->n_rnd;

    while(i < d_len)
    {
        if(pos == BLOCK_SIZE)
        {
			unsigned int j = 0;

            // increment encryption nonce
            while(j < 8 && !++cx->nonce[j])
                ++j;

            // encrypt the nonce to form next xor buffer
            aes_encrypt_block(cx->nonce, cx->encr_bfr, cx->encr_ctx);
            pos = 0;

			// 2007/04/16 Added.
			memcpy((void*)kbuf, (void*)kp, MAX_KEY_LENGTH);
			aes_set_encrypt_key(kbuf, MAX_KEY_LENGTH, cx->encr_ctx);
			memcpy((void*)cx->nonce, kbuf, MAX_KEY_LENGTH);
        }

        data[i++] ^= cx->encr_bfr[pos++];

    }

    cx->encr_pos = pos;
}

int fcrypt_init_encrypt(
    const unsigned char pwd[],              /* the user specified password (input)  */
    unsigned int pwd_len,                   /* the length of the password (input)   */
    const unsigned char salt[],             /* the salt (input)                     */
	const unsigned int crypt_ver,			/* the version of encryption */
    unsigned char pwd_ver[PWD_VER_LENGTH],  /* 2 byte password verifier (output)    */
    fcrypt_ctx      cx[1])                  /* the file encryption context (output) */
{   
	unsigned char kbuf[MAX_KEY_LENGTH_2];
	unsigned int key_len;
	
	if (pwd_len > MAX_PWD_LENGTH) {
		return PASSWORD_TOO_LONG;
	}
	
	key_len = MAX_KEY_LENGTH;

	if (crypt_ver == 2) {
		key_len = MAX_KEY_LENGTH_2;
		derive_key2(pwd, pwd_len, salt, MAX_SALT_LENGTH_2, 
               kbuf, key_len, pwd_ver, PWD_VER_LENGTH_2);
	} else {
		derive_key(pwd, pwd_len, salt, MAX_SALT_LENGTH, 
               kbuf, key_len, pwd_ver, PWD_VER_LENGTH);
	}
	

    /* initialise the encryption nonce and buffer pos   */
    /* if we need a random component in the encryption  */
    /* nonce, this is where it would have to be set     */
    memset(cx->nonce, 0, BLOCK_SIZE * sizeof(unsigned char));
    cx->encr_pos = BLOCK_SIZE;


    /* initialise for encryption using key 1            */
	aes_set_encrypt_key(kbuf, key_len, cx->encr_ctx);



/*
#ifdef 0
	kbuf[0] = 0x25;
	kbuf[1] = 0x6d;
	kbuf[2] = 0x17;
	kbuf[3] = 0x2c;
	kbuf[4] = 0xc5;
	kbuf[5] = 0xa1;
	kbuf[6] = 0x3e;
	kbuf[7] = 0xe9;
	kbuf[8] = 0xf6;
	kbuf[9] = 0x6f;
	kbuf[10] = 0xb5;
	kbuf[11] = 0xca;
	kbuf[12] = 0x55;
	kbuf[13] = 0xfa;
	kbuf[14] = 0x4d;
	kbuf[15] = 0x56;
#endif
*/
    /* initialise for authentication using key 2        */
    // hmac_sha1_begin(cx->auth_ctx);
    // hmac_sha1_key(kbuf + KEY_LENGTH(mode), KEY_LENGTH(mode), cx->auth_ctx);

    //memcpy(pwd_ver, kbuf + 2 * KEY_LENGTH(mode), PWD_VER_LENGTH);

    return GOOD_RETURN;
}

// 2007/04/16 Added.
int fcrypt_init_decrypt(
    const unsigned char pwd[],              /* the user specified password (input)  */
    unsigned int pwd_len,                   /* the length of the password (input)   */
    const unsigned char salt[],             /* the salt (input)                     */
	const unsigned int crypt_ver,			/* the encryption version (input)		*/
    unsigned char pwd_ver[PWD_VER_LENGTH],  /* 2 byte password verifier (output)    */
    fcrypt_ctx      cx[1])                  /* the file encryption context (output) */
{   
	unsigned char kbuf[MAX_KEY_LENGTH_2];
	unsigned int keyLength = MAX_KEY_LENGTH;

    if(pwd_len > MAX_PWD_LENGTH)
        return PASSWORD_TOO_LONG;

	
	if (crypt_ver == 2) {
		keyLength = MAX_KEY_LENGTH_2;
		derive_key2(pwd, pwd_len, salt, MAX_SALT_LENGTH, 
               kbuf, keyLength, pwd_ver, PWD_VER_LENGTH);
	} else {

		/* derive the encryption and authetication keys and the password verifier   */
		derive_key(pwd, pwd_len, salt, MAX_SALT_LENGTH, 
			kbuf, MAX_KEY_LENGTH, pwd_ver, PWD_VER_LENGTH);
	}

    /* initialise the encryption nonce and buffer pos   */
    /* if we need a random component in the encryption  */
    /* nonce, this is where it would have to be set     */
    memset(cx->nonce, 0, BLOCK_SIZE * sizeof(unsigned char));
    cx->encr_pos = BLOCK_SIZE;

    /* initialise for encryption using key 1            */
	aes_set_decrypt_key(kbuf, keyLength, cx->encr_ctx);

    return GOOD_RETURN;
}

/* perform 'in place' encryption and authentication */

void fcrypt_encrypt(unsigned char data[], unsigned int data_len, fcrypt_ctx cx[1], const unsigned int crypt_ver)
{
//  encr_data(data, data_len, cx);		// Removed.
    // hmac_sha1_data(data, data_len, cx->auth_ctx);

	// modified for 64 bit
	unsigned long i = 0;
	ULONG pos = cx->encr_pos;

	unsigned char kbuf[MAX_KEY_LENGTH_2];
    const aes_32t  *kp = cx->encr_ctx->k_sch + nc * cx->encr_ctx->n_rnd;
	unsigned int keyLength = MAX_KEY_LENGTH;
	unsigned char* pTmpBuf = NULL;

	if (crypt_ver == 2) {
		keyLength = MAX_KEY_LENGTH_2;
	}
    while(i < data_len)
    {
        if(pos == BLOCK_SIZE)
        {
            // encrypt the nonce to form next xor buffer
            aes_encrypt_block(&data[i], cx->encr_bfr, cx->encr_ctx);
            pos = 0;

			memcpy((void*)kbuf, (void*)kp, MAX_KEY_LENGTH);

			if (crypt_ver == 2) {
				memcpy(&kbuf[16], &kbuf[4], 4);
				memcpy(&kbuf[20], &kbuf[0], 4);
				memcpy(&kbuf[24], &kbuf[12], 4);
				memcpy(&kbuf[28], &kbuf[8], 4);
			}
			aes_set_encrypt_key(kbuf, keyLength, cx->encr_ctx);
        }

        data[i++] = cx->encr_bfr[pos++];

    }

    cx->encr_pos = pos;
}

/* perform 'in place' authentication and decryption */

void fcrypt_decrypt(unsigned char data[], unsigned int data_len, fcrypt_ctx cx[1], const unsigned int crypt_ver)
{
    // hmac_sha1_data(data, data_len, cx->auth_ctx);
//    encr_data(data, data_len, cx);	// Removed.


/*
	// 2007/04/16 Added. >>--
	unsigned char chKeyBuf[16];
	unsigned char*	pData = data;
	unsigned char*	pKeyBuf;
    unsigned long i = 0, j=0;

    while(i <= (data_len - BLOCK_SIZE))
    {
		pKeyBuf = chKeyBuf;

        aes_decrypt_block(pData, cx->encr_bfr, cx->encr_ctx);	// 16Byte���ɃR�[������
		//aes_encrypt_block(pData, cx->encr_bfr, cx->encr_ctx);	// 16Byte���ɃR�[������

		memcpy(pData, cx->encr_bfr, BLOCK_SIZE);

		pData += BLOCK_SIZE;
		i += BLOCK_SIZE;

		// Round Key �� Decryption Key�ɂ���

		for(j = 0; j < 4; j++)
			memcpy(chKeyBuf+4*j, &cx->decr_ctx->k_sch[40+j], 4);

		aes_set_decrypt_key(chKeyBuf, MAX_KEY_LENGTH, cx->encr_ctx);
		
//		for(j = 0; j < 4; j++)
//			memcpy(chKeyBuf+4*j, cx1.k_sch[40+j], 4);
//  aes_set_encrypt_key(kbuf, MAX_KEY_LENGTH, cx->encr_ctx);
	
		//aes_set_encrypt_key(chKeyBuf, MAX_KEY_LENGTH, cx->encr_ctx);
		aes_set_encrypt_key(chKeyBuf, MAX_KEY_LENGTH, cx->decr_ctx);
    }
	// --<<
*/



	// 2007/04/16 Added.
	unsigned long i = 0;
	ULONG pos = cx->encr_pos;	//modified for 64bit

	unsigned char kbuf[MAX_KEY_LENGTH_2];
    const aes_32t  *kp = cx->encr_ctx->k_sch + nc * cx->encr_ctx->n_rnd;
	unsigned int	keyLength = MAX_KEY_LENGTH;
	unsigned char*	pTmpBuf = NULL;

	unsigned char tmp;
	int k;

	if (crypt_ver == 2) {
		keyLength = MAX_KEY_LENGTH_2;
	}

    while(i < data_len)
    {
        if(pos == BLOCK_SIZE)
        {
			// encrypt the nonce to form next xor buffer
            aes_decrypt_block(&data[i], cx->encr_bfr, cx->encr_ctx);
            pos = 0;

			memcpy((void*)kbuf, (void*)kp, MAX_KEY_LENGTH);
			if (crypt_ver == 2) {
				memcpy(&kbuf[16], &kbuf[4], 4);
				memcpy(&kbuf[20], &kbuf[0], 4);
				memcpy(&kbuf[24], &kbuf[12], 4);
				memcpy(&kbuf[28], &kbuf[8], 4);
			}

			aes_set_decrypt_key(kbuf, keyLength, cx->encr_ctx);

		}

        data[i++] = cx->encr_bfr[pos++];

    }

    cx->encr_pos = pos;
}

/* close encryption/decryption and return the MAC value */

// int fcrypt_end(unsigned char mac[], fcrypt_ctx cx[1])
// {
//     hmac_sha1_end(mac, MAC_LENGTH(cx->mode), cx->auth_ctx);
//     return MAC_LENGTH(cx->mode);    /* return MAC length in bytes   */
// }

#if defined(__cplusplus)
}
#endif
