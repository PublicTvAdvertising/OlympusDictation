#pragma once

#ifdef _WINDOWS
#ifdef DSSENCODERLIB_EXPORTS
#define DSSENCODERLIB_API __declspec(dllexport)
#else
#define DSSENCODERLIB_API __declspec(dllimport)
#endif
#else
#define DSSENCODERLIB_API
#endif

#include "64bit.h"			// modified for 64 bi

typedef enum
{
	DssResult_Success = 0,

	DssResult_Unsupported_Format,
	DssResult_Invalid_Param,
	DssResult_Error

} DSSRESULT;


typedef struct tPCMFORMAT
{
	unsigned short		channels;			/* number of channels (i.e. mono, stereo...) */
	// modified for 64 bit
	ULONG		samplesPerSec;				/* sample rate */
	unsigned short		bitsPerSample;		/* number of bits per sample of mono data */
} PcmFormat;

typedef enum
{
	VERSION_UNKNOWN	= 0,
	VERSION_DSS		= 1,
	VERSION_DSSPRO	= 2
} DSS_VERSION;

typedef enum
{
	MODE_UNKNOWN	= 0,
	MODE_LP			= 1,
	MODE_SP			= 2,
	MODE_QP			= 3
} DSS_COMPRESSION_MODE;

typedef void (*FPCALLBACK_DSSWRITE)(char *buf, unsigned long size, unsigned long offset);


typedef enum
{
	ENCRYPTION_NONE		= 0,
	ENCRYPTION_STANDARD	= 1,
	ENCRYPTION_HIGH		= 2
} DSS_ENCRYPTION_VERSION;


typedef enum
{
	PRIORITY_NORMAL	= 0,
	PRIORITY_HIGH	= 1
} DSS_PRIORITY;

static const int AUTHORID_LENGTH = 16;
static const int WORKTYPEID_LENGTH = 16;
static const int COMMENT_LENGTH = 100;
static const int DATETIME_LENGTH = 6;

typedef struct tRECDATETIME
{
	char recDate[DATETIME_LENGTH + 1];	// yymmdd
	char recTime[DATETIME_LENGTH + 1];	// hhmmss
} RecDateTime;

typedef struct tDSSHEADERINFO
{
	char authorID[AUTHORID_LENGTH + 1];
	char worktypeID[WORKTYPEID_LENGTH + 1];
	char comment[COMMENT_LENGTH + 1];
	unsigned int jobNumber;
	DSS_PRIORITY priorityStatus;
	RecDateTime recStart;
	RecDateTime recEnd;
} DssHeaderInfo;
