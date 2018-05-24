// stdafx.h : �W���̃V�X�e�� �C���N���[�h �t�@�C���̃C���N���[�h �t�@�C���A�܂���
// �Q�Ɖ񐔂������A�����܂�ύX����Ȃ��A�v���W�F�N�g��p�̃C���N���[�h �t�@�C��
// ���L�q���܂��B
//
#include "64bit.h"
#pragma once

#pragma warning(disable : 4311)
#pragma warning(disable : 4995)
#pragma warning(disable : 4819)
#pragma warning(disable : 4291)

typedef long long LONGLONG;
typedef LONGLONG REFERENCE_TIME;

typedef unsigned short WORD;
typedef ULONG DWORD;
typedef int BOOL;
typedef unsigned char BYTE;

#define WINAPI __stdcall

#ifndef max
#define max(a,b)			(((a) > (b)) ? (a) : (b))
#endif

#ifndef min
#define min(a,b)			(((a) < (b)) ? (a) : (b))
#endif
#ifndef NOMINMAX


#endif	/* NOMINMAX */


const LONGLONG MILLISECONDS = (1000);			 // 10 ^ 3
const LONGLONG NANOSECONDS = (1000000000);		 // 10 ^ 9
const LONGLONG UNITS = (NANOSECONDS / 100); 	 // 10 ^ 7


#ifdef _DEBUG
# define DShowLog(str)				((void)0) //OutputDebugString(str)
# define DbgParseLog(str)			((void)0) //OutputDebugString(str)
# define DbgFramestreamLog(str)		((void)0) //OutputDebugString(str)
#else
# define DSDebugStr(str)			((void)0)
# define DbgParseLog(str)			((void)0)
# define DbgFramestreamLog(str)		((void)0)
#endif

#if defined(_DEBUG) && !defined(_WIN64)
# define _CRTDBG_MAP_ALLOC
# include <stdlib.h>
# include <crtdbg.h>

	static void* operator new(size_t nSize, const char * lpszFileName, int nLine)
	{
		return ::operator new(nSize, 1, lpszFileName, nLine);
	}
	#define DEBUG_NEW new(THIS_FILE, __LINE__)

	#define MALLOC_DBG(x) _malloc_dbg(x, 1, THIS_FILE, __LINE__);
	#define malloc(x) MALLOC_DBG(x)

#endif // _DEBUG



#ifndef memcpy_s_replacement //bilal
#define memcpy_s_replacement

#define memcpy_s(dest,num,src,count) memcpy(dest,src,num)

#endif



// TODO: �v���O�����ɕK�v�Ȓǉ��w�b�_�[�������ŎQ�Ƃ��Ă��������B
