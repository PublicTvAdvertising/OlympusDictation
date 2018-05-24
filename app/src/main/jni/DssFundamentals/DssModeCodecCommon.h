#pragma once

#include "dsswork.h"
//#include "typedef.h"
#include "cst2.h"

typedef unsigned char byte_t;
typedef unsigned short word_t;

enum dss_mode_t {
	mode_lp = 0,
	mode_sp = 1,
	mode_qp = 2
};

#ifndef min
# define min(a,b) (((a)<(b)) ? (a) : (b) )
#endif

#ifdef _DEBUG
# define DSS_CODEC_LOG_DEBUG 1
#endif

class BitReader{
public: 
	BitReader(word_t* start, int start_bit_offset, int num_bytes) 
		: source_(start)
		, total_bytes_(num_bytes)
		, offset_(0)
		, intemp_(0)
		, inbitmask_(0)
	{
		getbits(start_bit_offset);
	}
	long getbits(int nbits) 
	{
		long           mask;
		long           result;
		int           n;

		int total_words = total_bytes_/sizeof(word_t);
		for (result=0, mask=1L<<(nbits-1), n=nbits; n>0; n--, mask>>=1)
	    {
			if ((inbitmask_>>=1) == 0 && offset_ < total_words)
			{
				inbitmask_ = 0x8000;
				intemp_ = source_[offset_++];
			}
			if (intemp_ & inbitmask_) 
				result |= mask;
		}
		return result;
	}
	void ffwd(int nbits) {
		while (nbits > 0) {
			getbits(min(nbits,sizeof(word_t)*8));
			nbits -= min(nbits,sizeof(word_t)*8);
		}
	}
protected:
	word_t*			source_;
	int				total_bytes_;;
	int				offset_;
	word_t			intemp_;
	word_t			inbitmask_;
};

#if 0
class BitReader{
public: 
	BitReader(word_t* start, int start_bit_offset, int num_bytes) 
		: source_(start)
		, total_bytes_(num_bytes)
		, offset_(0)
		, intemp_(0)
		, inbitmask_(0)
	{
		getbits(start_bit_offset);
	}
	long getbits(int nbits) 
	{
		long           mask;
		long           result;
		int           n;

		int total_words = total_bytes_/sizeof(word_t);
		for (result=0, mask=1L<<(nbits-1), n=nbits; n>0; n--, mask>>=1)
	    {
			if ((inbitmask_>>=1) == 0 && offset_ < total_words)
			{
				inbitmask_ = 0x8000;
				intemp_ = source_[offset_++];
			}
			if (intemp_ & inbitmask_) 
				result |= mask;
		}
		return result;
	}
	void ffwd(int nbits) {
		while (nbits > 0) {
			getbits(min(nbits,sizeof(word_t)*8));
			nbits -= min(nbits,sizeof(word_t)*8);
		}
	}
protected:
	word_t*			source_;
	int				total_bytes_;;
	int				offset_;
	word_t			intemp_;
	word_t			inbitmask_;
};

#endif
