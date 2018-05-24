#include "DssConverter.h"
#include "DssConverterImpl.h"


DssConverter::DssConverter(void)
	: pImpl_(new Impl)
{
}


DssConverter::~DssConverter(void)
{
	delete pImpl_;
}


DSSRESULT DssConverter::SetInputPCMFormat(PcmFormat inputPCMFormat)
{
	return pImpl_->SetInputPCMFormat(inputPCMFormat);
}


DSSRESULT DssConverter::SetCompressionMode(DSS_VERSION ver,DSS_COMPRESSION_MODE compressionMode)
{
	return pImpl_->SetCompressionMode(ver, compressionMode);
}
/* Method to set compression mode and worktype   */
DSSRESULT DssConverter::SetCompressionModeAndWorkType(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode,char* workType)
{

	return pImpl_->SetCompressionModeAndWorkType(ver,compressionMode,workType);
}

DSSRESULT DssConverter::SetOutputCallback(FPCALLBACK_DSSWRITE pFunc)
{
	return pImpl_->SetOutputCallback(pFunc);
}


DSSRESULT DssConverter::SetDSSHeaderInfo(const DssHeaderInfo *pHeader)
{
	return pImpl_->SetDSSHeaderInfo(pHeader);
}


DSSRESULT DssConverter::SetEncryption(const char *password, DSS_ENCRYPTION_VERSION version)
{
	return pImpl_->SetEncryption(password, version);
}


DSSRESULT DssConverter::StartConverting()
{
	return pImpl_->StartConverting();
}


DSSRESULT DssConverter::EndConverting()
{
	return pImpl_->EndConverting();
}

// modified for 64 bit
DSSRESULT DssConverter::Convert(char *buf, ULONG length)
{
	return pImpl_->Convert(buf, length);
}
