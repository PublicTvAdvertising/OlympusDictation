#include "DssFundamentals/BasicResampler.h"
#include "DSSConverter/DssDefinitions.h"
#include <jni.h>

class Resampler
{
public:
	int startResampling(JNIEnv * env,jobject obj,jmethodID mid);
	Resampler(void);
	~Resampler(void);

	unsigned long downmix_channels(short *pOut, unsigned long dwOutSize, short *pIn, unsigned long dwInSize) ;
	float scale(unsigned char* pbBuf, unsigned int uiCurrent);


private:

	PcmFormat						*inputedPcmFormat_;
	CBasicResampler<short,float>	resampler_;
	short							*resamplingBuffer_;
	unsigned long					resamplingBufferSize_;
	unsigned int					inBytePerSample_;

};
