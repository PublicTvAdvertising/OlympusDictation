
// ˆÈ‰º‚Ì ifdef ƒuƒ�ƒbƒN‚Í DLL ‚©‚çŠÈ’P‚ÉƒGƒNƒXƒ|�[ƒg‚³‚¹‚éƒ}ƒNƒ�‚ð�ì�¬‚·‚é•W�€“I‚È•û–@‚Å‚·�B 
// ‚±‚Ì DLL “à‚Ì‚·‚×‚Ä‚Ìƒtƒ@ƒCƒ‹‚ÍƒRƒ}ƒ“ƒhƒ‰ƒCƒ“‚Å’è‹`‚³‚ê‚½ DSSAES_EXPORTS ƒVƒ“ƒ{ƒ‹
// ‚ÅƒRƒ“ƒpƒCƒ‹‚³‚ê‚Ü‚·�B‚±‚ÌƒVƒ“ƒ{ƒ‹‚Í‚±‚Ì DLL ‚ªŽg—p‚·‚é‚Ç‚Ìƒvƒ�ƒWƒFƒNƒg�ã‚Å‚à–¢’è‹`‚Å‚È‚¯
// ‚ê‚Î‚È‚è‚Ü‚¹‚ñ�B‚±‚Ì•û–@‚Å‚Íƒ\�[ƒXƒtƒ@ƒCƒ‹‚É‚±‚Ìƒtƒ@ƒCƒ‹‚ðŠÜ‚Þ‚·‚×‚Ä‚Ìƒvƒ�ƒWƒFƒNƒg‚ª DLL 
// ‚©‚çƒCƒ“ƒ|�[ƒg‚³‚ê‚½‚à‚Ì‚Æ‚µ‚Ä DSSAES_API ŠÖ�”‚ðŽQ�Æ‚µ�A‚»‚Ì‚½‚ß‚±‚Ì DLL ‚Í‚±‚Ìƒ}ƒN 
// ƒ�‚Å’è‹`‚³‚ê‚½ƒVƒ“ƒ{ƒ‹‚ðƒGƒNƒXƒ|�[ƒg‚³‚ê‚½‚à‚Ì‚Æ‚µ‚ÄŽQ�Æ‚µ‚Ü‚·�B

//#define WINAPI __stdcall
#ifdef WINAPI
#undef WINAPI
#endif

#define WINAPI

#ifdef __cplusplus
extern "C"
{
#endif

bool WINAPI AESGenerateSalt( char *password, unsigned char *salt );
bool WINAPI AESInitEncrypt( char *password, unsigned char *salt, const unsigned int crypt_ver, unsigned char *pwd_ver, void **ppState );
bool WINAPI AESInitDecrypt( char *password, unsigned char *salt, const unsigned int crypt_ver, unsigned char *pwd_ver, void **ppState );
bool WINAPI AESEncrypt( void *pState, unsigned char *buf, int len, const unsigned int crypt_ver );
bool WINAPI AESDecrypt( void *pState, unsigned char *buf, int len, const unsigned int crypt_ver );
bool WINAPI AESFree( void *pState );
bool WINAPI AESCopyState( void *pFirstState, void **ppNewState );

#ifdef __cplusplus
}
#endif
