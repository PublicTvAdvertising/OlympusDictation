#include"SimpleFileWriter.h"

#include"Resampler.h"
#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include "SimpleFileReader.h"

int pos=44;

const char *m_InFile;

PcmFormat pcm;
FILE *pcmFile;


typedef union {
	long l;
	struct {
		unsigned char b1;
		unsigned char b2;
		unsigned char b3;
		unsigned char b4;
	};
} long_pack;

extern "C"
{
    JNIEXPORT jint JNICALL Java_com_olympus_dmmobile_AMRConverter_resample(JNIEnv * env, jobject obj,jstring src);

};

// this method is called from AMRConverter which needs to downsample the PCM data from 16 kHz to 8 kHz before converting to AMR
JNIEXPORT jint JNICALL Java_com_olympus_dmmobile_AMRConverter_resample(JNIEnv * env, jobject obj,jstring src)
{
	jclass cls;
	jmethodID mid;
	m_InFile=env->GetStringUTFChars(src,0);

	// get the class from the obj

	cls = env->GetObjectClass(obj);

	// get the ID of method "encode"


	mid =env->GetMethodID(cls, "encode", "([S)V");
	if (mid == NULL) {

		return 0; // method not found
	}

	Resampler *resampler=new Resampler();
	int result=resampler->startResampling(env,obj,mid);
//__android_log_write(ANDROID_LOG_INFO, "nativelog", "returnmethod"+result);
	return result;

}
Resampler::Resampler(void)
	: resamplingBuffer_(0)
	, resamplingBufferSize_(0)
	, inputedPcmFormat_(0)
{
}
Resampler::~Resampler(void)
{
	if(resamplingBuffer_)
	{
		delete [] resamplingBuffer_;
		resamplingBuffer_ = 0;
	}

	if(inputedPcmFormat_)
	{
		delete inputedPcmFormat_;
		inputedPcmFormat_ = 0;
	}

}
// this function re-sample the PCM data and call the java method 'encode' in class 'AMRConverter' with the re-sampled data
int Resampler::startResampling(JNIEnv * env,jobject obj,jmethodID mid)
{

	jshortArray sArray;
	int SAMPLE_RATE=8000;

	ULONG size = 10 * 1024;	//10 kb
	char *buf = new char[size];

	SimpleFileReader *pcmReader=new SimpleFileReader();
	if(pcmReader->Open((LPCTSTR)m_InFile)==S_OK){
		pcmReader->parseHeader();
	}else{
		return S_FALSE;
	}

	pcmReader->GetPcmFormatInfo(pcm.channels, pcm.samplesPerSec, pcm.bitsPerSample);
	inBytePerSample_ = (pcm.bitsPerSample + 7)/8;

	resampler_.reset();
	resampler_.set_rates(pcm.samplesPerSec,SAMPLE_RATE);

	while(pcmReader->Read(buf,size)==S_OK)
	{
	//__android_log_print(ANDROID_LOG_INFO, "nativelog", "test int = %d", size);


		unsigned long neededBufferLen = max(size/inBytePerSample_/pcm.channels, resampler_.get_required_buffer_length(size)/inBytePerSample_/pcm.channels);

		if((resamplingBuffer_ == 0) || neededBufferLen > resamplingBufferSize_)
		{
			if(resamplingBuffer_){ delete [] resamplingBuffer_; }
			resamplingBufferSize_ = 4 + neededBufferLen;
			resamplingBuffer_ = new short[resamplingBufferSize_];
		}

		if(resamplingBuffer_)
		{

			downmix_channels(resamplingBuffer_, resamplingBufferSize_, (short *)buf, size/inBytePerSample_);
			int resamples = (int)resampler_.process((short *)resamplingBuffer_, size/inBytePerSample_/pcm.channels, resamplingBufferSize_);

			sArray=env->NewShortArray(resamples);
			//__android_log_print(ANDROID_LOG_INFO, "nativelog", "line after sArray");
			env->SetShortArrayRegion(sArray,0,resamples,resamplingBuffer_);

			// call the java method 'encode' with that re-sampled data in sArray
			__android_log_write(ANDROID_LOG_INFO, "nativelog", "call the java method 'encode' with that re-sampled data in sArray");
			env->CallVoidMethod(obj, mid,sArray);
__android_log_print(ANDROID_LOG_INFO, "nativelog", "line after env->sCallVoidMethod");
			int input_samples = resamples;
			env->DeleteLocalRef(sArray);
		}

	}

	pcmReader->Close();
	delete pcmReader;
	pcmReader = NULL;

	return S_OK;
}

unsigned long Resampler::downmix_channels(short *pOut, unsigned long dwOutSize, short *pIn, unsigned long dwInSize)
{

	int nChannels = pcm.channels;
	if (1 == nChannels && sizeof(short) == inBytePerSample_)
	{
		::memcpy(pOut, pIn, dwInSize*sizeof(pIn[0]));
	}
	else
	{
		float fChannelDiv = 1.f/((float)nChannels);  // was 1/sqrt(channels), but clipped signals really encode terribly, so play it safe
		BYTE *pInByte = (BYTE *)pIn;
		for (unsigned int n=0; n + nChannels <= dwInSize; n += nChannels) {
			int sum = 0;
			for (unsigned int c=0; c < nChannels; ++c) {
				sum += int(32767.f*scale(pInByte, n+c));
			}
			sum = (int)(float(sum)*fChannelDiv);
			if (sum < - 0x8000){
				sum = -0x8000;
			} else if (sum > 0x7fff) {
				sum = 0x7fff;
			}
			pOut[n/nChannels] = (short)sum;
		}
	}
	return dwInSize/nChannels;
}
inline float Resampler::scale(unsigned char* pbBuf, unsigned int uiCurrent)
{
	static const float m_fcK = 1.f / 127.f;
	static const float m_flK = 1.f / (float)(1<<23);
	static const float m_fsK = 1.f / (float)(1<<15);
	unsigned char* puc = (unsigned char*)pbBuf;
	short* ps = (short*)pbBuf;
	float* pf = (float*)pbBuf;
	long_pack value;
	switch( inBytePerSample_ )
	{
	case sizeof(char): // 8bit
		return ( (float)puc[uiCurrent] - 128.f ) * m_fcK;
		break;
	case sizeof(short): // 16bit
		return m_fsK * (float)ps[uiCurrent];
		break;
	case sizeof(char) * 3: // 24bit
		value.b2 = puc[uiCurrent*3];
		value.b3 = puc[(uiCurrent*3)+1];
		value.b4 = puc[(uiCurrent*3)+2];
		value.l >>= 8; // signed flag
		return m_flK*(float)value.l;
	default: // 32bit
		return pf[uiCurrent];
		break;
	}

}

