#pragma once

#include "DssDefinitions.h"
#include "64bit.h"				// modified for 64 bit
class DSSENCODERLIB_API DssConverter
{
public:
	DssConverter(void);
	virtual ~DssConverter(void);

	DSSRESULT StartConverting(void);
	DSSRESULT EndConverting(void);
	DSSRESULT Convert(char *buf, ULONG length);

	DSSRESULT SetInputPCMFormat(PcmFormat inputPCMFormat);
	
	DSSRESULT SetCompressionMode(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode);
    DSSRESULT SetCompressionModeAndWorkType(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode,char* workType);  /* Method to set compression mode and worktype   */
	DSSRESULT SetOutputCallback(FPCALLBACK_DSSWRITE pFunc);

	DSSRESULT SetDSSHeaderInfo(const DssHeaderInfo *pHeader);

	DSSRESULT SetEncryption(const char *password, DSS_ENCRYPTION_VERSION version);

private:
	class	Impl;
	Impl	*pImpl_;

	// non-copyable
	DssConverter(const DssConverter&);
	void operator=(const DssConverter&);

};

