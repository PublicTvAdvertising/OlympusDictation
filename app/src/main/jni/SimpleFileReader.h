#pragma once

#include "platform.linux.h"
#include "64bit.h"

typedef struct
{
    char rID[4];      			// 'RIFF'
    long int rLen;
    char wID[4];      			// 'WAVE'
    char fId[4];     			// 'fmt'
    long int pcmHeaderLength;
    short int wFormatTag;
    short int numChannels;
    long int nSamplesPerSec;
    long int nAvgBytesPerSec;
    short int numBlockAlingn;
    short int numBitsPerSample;
} WAV_HDR;

typedef struct
{
    char dId[4];  // 'data' or 'fact'
    long int dLen;
} CHUNK_HDR;


class SimpleFileReader
{
public:
	SimpleFileReader(void);
	~SimpleFileReader(void);

	HRESULT Open(const TCHAR *pFileName);
	HRESULT Close(void);
	HRESULT SetPos(unsigned long pos);
	HRESULT GetPos(unsigned long &pos);
	HRESULT Read(char *pBuf, ULONG &size);
	HRESULT ReadSplitFile(char *pBuf, ULONG &size);
	HRESULT parseHeader();

	void setSplitPoints(unsigned long start,unsigned long end);
	long  GetFileLength();
	void GetPcmFormatInfo(unsigned short &channels, ULONG &samplesPerSec, unsigned short &bitsPerSample);

private:

	FILE *fp;
	unsigned long int start = 0;
	unsigned long int end = 0;

};

