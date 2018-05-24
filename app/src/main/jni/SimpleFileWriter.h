#pragma once
#include <fstream>
#include "platform.linux.h"
class SimpleFileWriter
{
public:
	SimpleFileWriter(void);
	~SimpleFileWriter(void);

	HRESULT Open(const TCHAR *pFileName);
	HRESULT Close(void);
	HRESULT Write(char *pBuf, unsigned long size, unsigned long offset);

private:
	std::ofstream ofs;
};

