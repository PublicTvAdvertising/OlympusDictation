#include "DssConverterImpl.h"
#ifndef max
#define max(a,b)            (((a) > (b)) ? (a) : (b))
#endif

typedef union {
	long l;
	struct {
		unsigned char b1;
		unsigned char b2;
		unsigned char b3;
		unsigned char b4;
	};
} long_pack;


DssConverter::Impl::Impl(void)
	: encoder_(0)
	, resamplingBuffer_(0)
	, resamplingBufferSize_(0)
	, encodeOutBuffer_(0)
	, encodeOutBufferSize_(0)
	, inputedPcmFormat_(0)
	, outputedDssFormat_(0)
{
}


DssConverter::Impl::~Impl(void)
{
	if(encoder_)
	{
		encoder_->finish();
		delete encoder_;
		encoder_ = 0;
	}

	if(resamplingBuffer_)
	{
		delete [] resamplingBuffer_;
		resamplingBuffer_ = 0;
	}

	if(encodeOutBuffer_)
	{
		delete [] encodeOutBuffer_;
		encodeOutBuffer_ = 0;
	}

	if(inputedPcmFormat_)
	{
		delete inputedPcmFormat_;
		inputedPcmFormat_ = 0;
	}

	if(outputedDssFormat_)
	{
		delete outputedDssFormat_;
		outputedDssFormat_ = 0;
	}
}



DSSRESULT DssConverter::Impl::SetCompressionMode(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode)
{
	// restriction on Smartphone App
	if(ver == VERSION_DSS)
	{
		if(compressionMode != MODE_SP)
			return DssResult_Unsupported_Format;
	}
	else if(ver == VERSION_DSSPRO)
	{
		if((compressionMode != MODE_SP) && (compressionMode != MODE_QP))
			return DssResult_Unsupported_Format;
	}
	else
	{
		return DssResult_Unsupported_Format;
	}

	if(!outputedDssFormat_)
	{
		outputedDssFormat_ = new DssFormat;
	}

	outputedDssFormat_->version = ver;
	outputedDssFormat_->mode = compressionMode;
	int fileFormatMode = GetDssFileFormatCompressionMode(compressionMode);
	outputedDssFormat_->bitrate = DssFileFormat::get_bitrate_for_mode(fileFormatMode);
	outputedDssFormat_->samplesPerSec = DssFileFormat::get_samplerate_for_mode(fileFormatMode);

	formatter_.SetCompressionMode(ver, DssFileFormat::compression_mode_t(fileFormatMode));
	if(encoder_)
	{
		delete encoder_;
	}

	if(outputedDssFormat_->mode == MODE_LP)
	{
		encoder_ = createDssEncoder(mode_lp);
	}
	else if(outputedDssFormat_->mode == MODE_SP)
	{
		encoder_ = createDssEncoder(mode_sp);
	}
	else if(outputedDssFormat_->mode == MODE_QP)
	{
		encoder_ = createDssEncoder(mode_qp);
	}
	else
	{
		return DssResult_Unsupported_Format;
	}

	// SP & QP share some variables, safest to make sure everything is flushed before we run...
	encoder_->finish();
	encoder_->init();

	return DssResult_Success;
}
/* Method to set compression mode and worktype   */
DSSRESULT DssConverter::Impl::SetCompressionModeAndWorkType(DSS_VERSION ver, DSS_COMPRESSION_MODE compressionMode,char* workType)
{
	// restriction on Smartphone App
	if(ver == VERSION_DSS)
	{
		if(compressionMode != MODE_SP)
			return DssResult_Unsupported_Format;
	}
	else if(ver == VERSION_DSSPRO)
	{
		if((compressionMode != MODE_SP) && (compressionMode != MODE_QP))
			return DssResult_Unsupported_Format;
	}
	else
	{
		return DssResult_Unsupported_Format;
	}
    
	if(!outputedDssFormat_)
	{
		outputedDssFormat_ = new DssFormat;
	}
    
	outputedDssFormat_->version = ver;
	outputedDssFormat_->mode = compressionMode;
	int fileFormatMode = GetDssFileFormatCompressionMode(compressionMode);
	outputedDssFormat_->bitrate = DssFileFormat::get_bitrate_for_mode(fileFormatMode);
	outputedDssFormat_->samplesPerSec = DssFileFormat::get_samplerate_for_mode(fileFormatMode);
    
    //	formatter_.SetCompressionMode(ver, DssFileFormat::compression_mode_t(fileFormatMode));
    formatter_.SetCompressionModeAndWorkType(ver, DssFileFormat::compression_mode_t(fileFormatMode), workType);//shaheen
	if(encoder_)
	{
		delete encoder_;
	}
    
	if(outputedDssFormat_->mode == MODE_LP)
	{
		encoder_ = createDssEncoder(mode_lp);
	}
	else if(outputedDssFormat_->mode == MODE_SP)
	{
		encoder_ = createDssEncoder(mode_sp);
	}
	else if(outputedDssFormat_->mode == MODE_QP)
	{
		encoder_ = createDssEncoder(mode_qp);
	}
	else
	{
		return DssResult_Unsupported_Format;
	}
    
	// SP & QP share some variables, safest to make sure everything is flushed before we run...
	encoder_->finish();
	encoder_->init();
    
	return DssResult_Success;
}

int DssConverter::Impl::GetDssFileFormatCompressionMode(DSS_COMPRESSION_MODE mode)
{
	if(mode == MODE_LP)
	{
		return DssFileFormat::LP_NO_SCVA;
	}
	else if(mode == MODE_QP)
	{
		return DssFileFormat::QP_NO_SCVA;
	}
	else//else if(mode == MODE_SP)
	{
		return DssFileFormat::SP_NO_SCVA;
	}
}


DSSRESULT DssConverter::Impl::SetInputPCMFormat(PcmFormat inputPCMFormat)
{
	// BitsPerSample‚Í16bitŒÅ’è
	// Channel�”‚Í1(mono)‚Ü‚½‚Í2(stereo)‚Ì‚Ç‚¿‚ç‚©‚Ì‚Ý ‚Æ‚·‚é
	if((inputPCMFormat.bitsPerSample != 16) ||
		(inputPCMFormat.channels != 1 && inputPCMFormat.channels != 2))
	{
		return DssResult_Unsupported_Format;
	}

	if(!inputedPcmFormat_)
	{
		inputedPcmFormat_ = new PcmFormat;
	}

	*inputedPcmFormat_ = inputPCMFormat;

	inBytePerSample_ = (inputedPcmFormat_->bitsPerSample + 7)/8;
	return DssResult_Success;
}


DSSRESULT DssConverter::Impl::SetOutputCallback(FPCALLBACK_DSSWRITE pFunc)
{
	formatter_.SetOutputCallback(pFunc);

	return DssResult_Success;
}


DSSRESULT DssConverter::Impl::SetEncryption(const char *password, DSS_ENCRYPTION_VERSION version)
{
	formatter_.SetPassword(password);
	formatter_.SetEncryptionVersion((unsigned short)version);

	return DssResult_Success;
}


DSSRESULT DssConverter::Impl::SetDSSHeaderInfo(const DssHeaderInfo *pHeader)
{
	if(!pHeader)
	{
		return DssResult_Invalid_Param;
	}

	if(strlen(pHeader->authorID))
	{
		formatter_.set_AuthorId(pHeader->authorID);
	}
	if(strlen(pHeader->worktypeID))
	{
		formatter_.set_WorktypeId(pHeader->worktypeID);
	}
	if(strlen(pHeader->comment))
	{
		formatter_.set_Notes(pHeader->comment);
	}
	if(strlen(pHeader->recStart.recDate))
	{
		formatter_.set_RecordingStartDate(pHeader->recStart.recDate);
	}
	if(strlen(pHeader->recStart.recTime))
	{
		formatter_.set_RecordingStartTime(pHeader->recStart.recTime);
	}
	if(strlen(pHeader->recEnd.recDate))
	{
		formatter_.set_RecordingEndDate(pHeader->recEnd.recDate);
	}
	if(strlen(pHeader->recEnd.recTime))
	{
		formatter_.set_RecordingEndTime(pHeader->recEnd.recTime);
	}

	formatter_.set_JobNumber((pHeader->jobNumber > 0) ? pHeader->jobNumber : 1);
	formatter_.set_PriorityStatus((unsigned char)pHeader->priorityStatus);

	return DssResult_Success;
}



DSSRESULT DssConverter::Impl::StartConverting(void)
{
	if(!inputedPcmFormat_ || !outputedDssFormat_)
	{
		return DssResult_Unsupported_Format;
	}

	resampler_.reset();
	resampler_.set_rates(inputedPcmFormat_->samplesPerSec, outputedDssFormat_->samplesPerSec);

	formatter_.StartFormatting();

	return DssResult_Success;
}


DSSRESULT DssConverter::Impl::EndConverting(void)
{
	PushThroughRemaining();

	formatter_.EndFormatting();

	return DssResult_Success;
}


DSSRESULT DssConverter::Impl::Convert(char *buf, ULONG length)
{
    
	ULONG neededBufferLen = max(length/inBytePerSample_/inputedPcmFormat_->channels, resampler_.get_required_buffer_length(length)/inBytePerSample_/inputedPcmFormat_->channels);
	if((resamplingBuffer_ == 0) || neededBufferLen > resamplingBufferSize_)
	{
		if(resamplingBuffer_){ delete [] resamplingBuffer_; }
		resamplingBufferSize_ = 4 + neededBufferLen;
		resamplingBuffer_ = new short[resamplingBufferSize_];
	}

	ULONG outBufSize = max(512, resamplingBufferSize_) + 4;
	if((encodeOutBuffer_ == 0) || encodeOutBufferSize_ < outBufSize)
	{
		if(encodeOutBuffer_){ delete [] encodeOutBuffer_;}
		encodeOutBufferSize_ = outBufSize;
		encodeOutBuffer_ = new char[encodeOutBufferSize_];
	}

	if(resamplingBuffer_)
	{
		downmix_channels(resamplingBuffer_, resamplingBufferSize_, (short *)buf, length/inBytePerSample_);

		DWORD resamples = (DWORD)resampler_.process((short *)resamplingBuffer_, length/inBytePerSample_/inputedPcmFormat_->channels, resamplingBufferSize_);
		int input_samples = resamples;

		int outSize = encodeOutBufferSize_;
		encoder_->encode_frames((short *)resamplingBuffer_, input_samples, (byte_t *)encodeOutBuffer_, outSize,  GetDssFileFormatCompressionMode(outputedDssFormat_->mode));
		if(outSize > 0)
		{
			formatter_.AddFrameData((unsigned char *)encodeOutBuffer_, outSize);
		}
	}

	return DssResult_Success;
}


DSSRESULT DssConverter::Impl::PushThroughRemaining()
{
	long remainSize = encoder_->get_remaining();
	if(remainSize > 0)
	{
		int nSizeZeroBuf = min(resamplingBufferSize_, resampler_.multiply_by_conversion(max(320, 512 - encoder_->get_remaining())));
		::memset(resamplingBuffer_, 0, resamplingBufferSize_*sizeof(short));

		DWORD resamples = (DWORD)resampler_.process((short *)resamplingBuffer_, nSizeZeroBuf, resamplingBufferSize_);
		int input_samples = resamples;
		
		int outSize = encodeOutBufferSize_;
		encoder_->encode_frames((short *)resamplingBuffer_, input_samples, (byte_t *)encodeOutBuffer_, outSize,  GetDssFileFormatCompressionMode(outputedDssFormat_->mode));
		if(outSize > 0)
		{
			formatter_.AddFrameData((unsigned char *)encodeOutBuffer_, outSize);
		}
	}

	return DssResult_Success;
}


unsigned long DssConverter::Impl::downmix_channels(short *pOut, unsigned long dwOutSize, short *pIn, unsigned long dwInSize) 
{
	int nChannels = inputedPcmFormat_->channels;
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


inline float DssConverter::Impl::scale(unsigned char* pbBuf, unsigned int uiCurrent)
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
		value.l >>= 8; // signed flag ‚ðˆÛŽ�‚µ‚½‚Ü‚Ü�¶shift
		return m_flK*(float)value.l;
	default: // 32bit
		return pf[uiCurrent];
		break;
	}
}

