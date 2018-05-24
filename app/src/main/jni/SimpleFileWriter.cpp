#include "SimpleFileWriter.h"


SimpleFileWriter::SimpleFileWriter(void)
{
}


SimpleFileWriter::~SimpleFileWriter(void)
{
	ofs.close();
}

HRESULT SimpleFileWriter::Open(const TCHAR *pFileName)
{
	ofs.open(pFileName, std::ios::out | std::ios::trunc | std::ios::binary);

	return S_OK;
}

HRESULT SimpleFileWriter::Close(void)
{
	ofs.close();

	return S_OK;
}

HRESULT SimpleFileWriter::Write(char *pBuf, unsigned long size, unsigned long offset)
{
	if(size > 0)
	{
		ofs.seekp(offset, std::ios::beg);
		ofs.write(pBuf, size);
	}

	return S_OK;
}



