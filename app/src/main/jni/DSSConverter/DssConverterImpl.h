#pragma once

#include "DssDefinitions.h"
#include "DssConverter.h"
#include "DssFormatter.h"
#include "DssFundamentals/IDssModeEncoder.h"
#include "DssFundamentals/BasicResampler.h"

typedef struct tDSSFORMAT
{
	DSS_VERSION				version;
	DSS_COMPRESSION_MODE	mode;
	int						samplesPerSec;
	int						bitrate;
} DssFormat;


class DssConverter::Impl
{
public:
	Impl(void);
	virtual ~Impl(void);

	DSSRESULT StartConverting(void);
	DSSRESULT EndConverting(void);
	DSSRESULT Convert(char *buf, ULONG length);			// modified for 64 bit

	DSSRESULT SetInputPCMFormat(PcmFormat inputPCMFormat);
	
	DSSRESULT SetCompressionMode(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode);
    DSSRESULT SetCompressionModeAndWorkType(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode,char* workType); /* Function to set compression mode and worktype   */

	DSSRESULT SetOutputCallback(FPCALLBACK_DSSWRITE pFunc);

	DSSRESULT SetDSSHeaderInfo(const DssHeaderInfo *pHeader);

	DSSRESULT SetEncryption(const char *password, DSS_ENCRYPTION_VERSION version);

private:
	DSSRESULT PushThroughRemaining();
	unsigned long downmix_channels(short *pOut, unsigned long dwOutSize, short *pIn, unsigned long dwInSize) ;
	float scale(unsigned char* pbBuf, unsigned int uiCurrent);
	int GetDssFileFormatCompressionMode(DSS_COMPRESSION_MODE mode);

private:

	PcmFormat						*inputedPcmFormat_;
	DssFormat						*outputedDssFormat_;

	IDssModeEncoder					*encoder_;
	char							*encodeOutBuffer_;
	ULONG					encodeOutBufferSize_;			// modified for 64 bit

	CBasicResampler<short,float>	resampler_;
	short							*resamplingBuffer_;
	ULONG					resamplingBufferSize_;			// modified for 64 bit
	unsigned int					inBytePerSample_;

	DssFormatter					formatter_;

};

