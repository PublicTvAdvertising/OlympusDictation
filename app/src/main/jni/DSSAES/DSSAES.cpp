// DSSAES.cpp : DLL アプリケーション用のエントリ ポイントを定義します。
//

#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <string.h>
//#include <windows.h>
#include <time.h>

#include "DSSAES.h"

#include "fileenc.h"
#include "prng.h"

// グローバル変数
//fcrypt_ctx  zcx_bak[1];




/* simple entropy collection function that uses the fast timer      */
/* since we are not using the random pool for generating secret     */
/* keys we don't need to be too worried about the entropy quality   */

int entropy_fun(unsigned char buf[], unsigned int len)
{
#if 0
	//unsigned __int64    pentium_tsc[1];
 //   unsigned int        i;
	//
 //   QueryPerformanceCounter((LARGE_INTEGER *)pentium_tsc);
 //   for(i = 0; i < 8 && i < len; ++i)
 //       buf[i] = ((unsigned char*)pentium_tsc)[i];
 //   return i;
#else
	static bool isInit = false;
	if(!isInit)
	{
		::srand((unsigned int)time(0));
		isInit = true;
	}

	unsigned char random[8];
	for(int j = 0; j < 4; ++j)
	{
		((short*)random)[j] = ::rand();
	}

	unsigned int i;
	for(i = 0; i < 8 && i < len; ++i)
        buf[i] = random[i];
    return i;
#endif
}


bool WINAPI AESGenerateSalt( char *password,			// [in]
									   unsigned char *salt )	// [out]
{
    int len, err = 0;
    prng_ctx rng[1];    /* the context for the random number pool   */

    len = (int)strlen(password);
	/* password is too short    */	// 2007/04/12 Modified.
    if(len < MIN_PWD_LENGTH || len > MAX_PWD_LENGTH) // 8
    {
        return false;
    }

    /* set the key length based on password length assuming that there  */
    /* are about 4 bits of entropy per password character (the key      */
    /* length and other mode dependent parameter values are set using   */
    /* macros defined in fileenc.h)                                     */
    // mode = (len < 32 ? 1 : len < 48 ? 2 : 3);

    prng_init(entropy_fun, rng);                /* initialise RNG   */
    prng_rand(salt, MAX_SALT_LENGTH, rng);    /* and the salt     */
    prng_end(rng);

	return true;
}



bool WINAPI AESInitEncrypt( char *password,		// [in]
							   unsigned char *salt,			// [in]
							   const unsigned int crypt_ver,// [in]		// version2 対応
							   unsigned char *pwd_ver, 		// [out]
							   void **pState)
{
    int len, err = 0;
    fcrypt_ctx  zcx[1];

    len = (int)strlen(password);
	/* password is too short    */	// 2007/04/12 Modified.
    if(len < MIN_PWD_LENGTH || len > MAX_PWD_LENGTH) // 8
    {
		*pState = 0;
        return false;
    }

    /* set the key length based on password length assuming that there  */
    /* are about 4 bits of entropy per password character (the key      */
    /* length and other mode dependent parameter values are set using   */
    /* macros defined in fileenc.h)                                     */
    //mode = (len < 32 ? 1 : len < 48 ? 2 : 3);

    /* initialise encryption and authentication */
    fcrypt_init_encrypt((unsigned char*)password, (unsigned int)strlen(password), salt, crypt_ver, pwd_ver, zcx );

	// 暗号化コンテキストをバックアップ
    fcrypt_ctx*  rzcx = new fcrypt_ctx;
    memcpy( rzcx, zcx, sizeof(fcrypt_ctx) );
	*pState = reinterpret_cast<void *>(rzcx);

	return true;
}

bool WINAPI AESInitDecrypt( char *password,		// [in]
							   unsigned char *salt,			// [in]
							   const unsigned int crypt_ver,// [in]		// version2 対応
							   unsigned char *pwd_ver, 		// [out]
							   void **pState )
{
    int len, err = 0;
    fcrypt_ctx  zcx[1];

    len = (int)strlen(password);
	/* password is too short    */	// 2007/04/12 Modified.
    if(len < MIN_PWD_LENGTH || len > MAX_PWD_LENGTH) // 8
    {
		*pState = 0;
        return false;
    }

    /* initialise encryption and authentication */
    fcrypt_init_decrypt((unsigned char*)password, (unsigned int)strlen(password), salt, crypt_ver, pwd_ver, zcx );


    fcrypt_ctx*  rzcx = new fcrypt_ctx;
	// 暗号化コンテキストをバックアップ
    memcpy( rzcx, zcx, sizeof(fcrypt_ctx) );
	*pState = reinterpret_cast<void *>(rzcx);
	return true;
}

bool WINAPI AESEncrypt( void *pState, unsigned char *buf, int len, const unsigned int crypt_ver )
{
    fcrypt_ctx  zcx[1];

	// 暗号化コンテキストをリセット
    memcpy( zcx, pState, sizeof(fcrypt_ctx) );
    // 暗号化
	fcrypt_encrypt(buf, len, zcx, crypt_ver);

	return true;
}



bool WINAPI AESDecrypt( void *pState, unsigned char *buf, int len, const unsigned int crypt_ver )
{
    fcrypt_ctx  zcx[1];

	// 暗号化コンテキストをリセット
	memcpy( zcx, pState, sizeof(fcrypt_ctx) );
    // 復号化
    fcrypt_decrypt(buf, len, zcx, crypt_ver);

	return true;
}

bool WINAPI AESFree( void *pState )
{
	fcrypt_ctx *zcx= reinterpret_cast<fcrypt_ctx *>(pState);
	delete zcx;
	return true;
}

bool WINAPI AESCopyState( void *pFirstState, void **ppNewState )
{
	fcrypt_ctx *zcx= reinterpret_cast<fcrypt_ctx *>(pFirstState);
	fcrypt_ctx *ncx = new fcrypt_ctx;
	::memcpy(ncx,zcx,sizeof(fcrypt_ctx));
	*ppNewState = reinterpret_cast<void *>(ncx);
	return true;
}

