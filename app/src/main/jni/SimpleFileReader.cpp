#include <android/log.h>
#include "SimpleFileReader.h"

WAV_HDR* pWavHeader;
CHUNK_HDR* pChunkHeader;

unsigned int stat;
char outBuffer[80];

unsigned short nChannels;
ULONG nSamplesPerSec;
unsigned short nBitsPerSample;

unsigned long int length = 0;
unsigned long int read_count = 0;


SimpleFileReader::SimpleFileReader(void)
{
}

SimpleFileReader::~SimpleFileReader(void)
{
	if(fp)
	{
		fclose(fp);
		fp = 0;
	}
}

HRESULT SimpleFileReader::Open(const TCHAR *pFileName)
{
	fp = fopen(pFileName, ("rb"));

	return (fp) ? S_OK : E_FAIL;
}

HRESULT SimpleFileReader::Close(void)
{
	if(fp)
	{
		fclose(fp);
		fp = 0;
	}

	return S_OK;
}

HRESULT SimpleFileReader::SetPos(unsigned long pos)
{
	int result = fseek(fp, pos, SEEK_SET);

	return (result == 0) ? S_OK : E_FAIL;
}

HRESULT SimpleFileReader::GetPos(unsigned long &pos)
{
	pos = ftell(fp);

	return S_OK;
}

long  SimpleFileReader::GetFileLength()
{
	fseek(fp, 0, SEEK_END); // seek to end of file
	long int size = ftell(fp); // get current file pointer               bv
	fseek(fp, 0, SEEK_SET);

	return size;
}
void SimpleFileReader::setSplitPoints(unsigned long start_value,unsigned long end_value){
	start=start_value;
	end=end_value;
}
HRESULT SimpleFileReader::Read(char *pBuf, ULONG &size)
{
	if(!pBuf || size == 0)
		return E_FAIL;

	size_t readLen = fread(pBuf, 1, size, fp);
	if(size != readLen)
	{
		size = readLen;
	}
	size = readLen;

	return (readLen > 0) ? S_OK : E_FAIL;
}
HRESULT SimpleFileReader::ReadSplitFile(char *pBuf, ULONG &size)
{

	if(start >= end) {
		return 2;
	}

	fseek(fp, start, SEEK_SET);
	::memset(pBuf, 0, size);

	length = end - start;
	if(length > size)
	length = size;

	read_count = fread(pBuf, 1, length, fp);
	if(ferror(fp) != 0) {
		return 3;
	}
	if(read_count != length) {
		return 4;
	}

	start += read_count;
	return (start <= end) ? S_OK : E_FAIL;

}
HRESULT SimpleFileReader::parseHeader()
{
	pWavHeader = new WAV_HDR;
	pChunkHeader = new CHUNK_HDR;


	 /*----------------------
	  *  read the riff/wav header
	  *---------------------*/

	stat = fread((void*) pWavHeader, sizeof(WAV_HDR), (size_t)1, fp);
	if(stat != 1)
	{
	  	//__android_log_print(ANDROID_LOG_VERBOSE, "SimpleFileReader: ", "WAV Header read error! ");
	}

	nChannels=pWavHeader->numChannels;
	nSamplesPerSec=pWavHeader->nSamplesPerSec;
	nBitsPerSample= pWavHeader->numBitsPerSample;

}
void SimpleFileReader::GetPcmFormatInfo(unsigned short &channels, ULONG &samplesPerSec,unsigned short &bitsPerSample)
{
	channels = nChannels;
	samplesPerSec = nSamplesPerSec;
	bitsPerSample = nBitsPerSample;
}

