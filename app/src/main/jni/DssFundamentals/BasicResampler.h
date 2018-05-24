#ifndef CHEAP_RESAMPLER_H__
#define CHEAP_RESAMPLER_H__

#include <stdexcept>
#include <cmath>
#include "HalfBandFilter.h"

//#define UPSAMPLE_INTERPOLATION 

#ifndef min
#define min(a, b)  (((a) < (b)) ? (a) : (b))
#endif
#ifndef max
#define max(a, b)  (((a) > (b)) ? (a) : (b))
#endif

struct resampling_error : public std::runtime_error
{
	resampling_error(const std::string& message = std::string("unspecified"), long reqbufsize = -1)
		: std::runtime_error(message)
		, requiredBufferSize_(reqbufsize)
	{
	}
	long requiredBufferSize() const {
		return requiredBufferSize_;
	}
private:
	long requiredBufferSize_;
};

template <typename sample_t, typename float_t>
class CBasicResampler
{
protected:
	enum {BUFFER_LENGTH = 1024*32,
		  TEMPBUFFER_LENGTH = 16384	
	};
public:
	CBasicResampler() 
		: write_idx_(0)
		, read_idx_(0.)
		, input_rate_(1)
		, output_rate_(1)
		, conversion_(1.)
		, tempbuffer_(0)
		, num_halfers_(0)
		, halfers_(0)
		, tempbuffer_len_(TEMPBUFFER_LENGTH)
		, head_distance_(0)
		, leftover_len_(0)
	{
		::memset(cbuf_,0,sizeof(cbuf_));
		tempbuffer_ = new sample_t[tempbuffer_len_];
		::memset(tempbuffer_, 0, sizeof(sample_t)*tempbuffer_len_);
	}
	~CBasicResampler() {
		delete[] halfers_;
		delete[] tempbuffer_;
	}


	void reset(void) {
		::memset(cbuf_,0,sizeof(cbuf_));
		if (tempbuffer_) {
			::memset(tempbuffer_, 0, sizeof(sample_t)*tempbuffer_len_);
		}
		write_idx_ = 4;
		read_idx_ = 0;
		head_distance_ = float_t(write_idx_) - float_t(read_idx_);
		input_rate_ = 1;
		output_rate_ = 1;
		conversion_ = 1.;
		num_halfers_ = 0;
		leftover_len_ = 0;

	}
	LONG get_required_buffer_length(long insize) {
		return (LONG)ceil(float_t(insize)/(conversion_*(1<<num_halfers_)));
	}
	LONG multiply_by_conversion(long insize) {
		return (LONG)ceil(float_t(insize)*conversion_*(1<<num_halfers_));
	}
	inline float_t clip(float_t val) {
		if (val > (float_t)0x7fff) {
			val = (float_t)0x7fff;
		}
		else if (val < -(float_t)0x7fff) {
			val = -(float_t)0x7fff;
		}
		return val;
	}
	int process(sample_t *InOut, int incount, int inmaxsize, bool canThrow = false) {
		// if there is no sample rate conversion, do nothing
		if (1 == conversion_ && num_halfers_ == 0) {
			return incount;
		}
		// we need our temp buffer to be as big as the output (if upsampling, bigger than the input) 
		// or the input buffer prior to downsampling
		LONG req_buf_size = 2*(max(incount,(int)ceilf(float_t(incount)/conversion_)) + (1<<(num_halfers_+1)));
		if ( req_buf_size > tempbuffer_len_) {
			tempbuffer_len_ = req_buf_size;
			delete[] tempbuffer_;
			tempbuffer_ = new sample_t[tempbuffer_len_];
			::memset(tempbuffer_, 0, sizeof(sample_t)*tempbuffer_len_);
		}
		// if we are doubling the sample rate, do it the best (and most efficient) way
		if (float_t(0.5) == conversion_ && 
			inmaxsize >= incount*2 && 
			incount*2 < tempbuffer_len_) 
		{
			::memcpy(tempbuffer_,InOut,sizeof(sample_t)*incount);
			int m=0;
			for (int n=0; n < incount; ++n) {
				InOut[m++] = sample_t(clip(2.f*halfband_.tick(tempbuffer_[n])));
				InOut[m++] = sample_t(clip(2.f*halfband_.tick(0)));
			}
			return incount*2;
		}
		// if we are halving the sample rate, we don't need to use a temporary buffer and we only need the halfband filter
		// requires that the input length is a multiple of 2**num_halfers_ samples long
		else if (float_t(1.0) == conversion_ && num_halfers_ > 0 && (incount%(1<<num_halfers_) == 0))
		{
			for (int k=0; k < num_halfers_; ++k) {
				for (int n=0; n < incount; n++) {
					float_t prev = halfers_[k].tick(InOut[n]);
					if ((n%2) == 0) {
						InOut[n/2] = sample_t(clip(prev));
					}
				}
				incount /= 2;
			}
			return incount;
		}
		else if (inmaxsize >= float_t(incount)/conversion_ || conversion_ == 1)
		{
			::memcpy(&tempbuffer_[leftover_len_],InOut,sizeof(sample_t)*incount);
			incount += leftover_len_;
			int halfchunksize = 1<<(num_halfers_);
			leftover_len_ = incount % halfchunksize;
			incount -= leftover_len_;

			int m=0,n=0;
			int topush,topull,k;
			while (n < incount) 
			{
				topush = min(BUFFER_LENGTH/2-16,(incount-n));
				k = 0;
				while (k < topush) 
				{
					if (num_halfers_ > 0) 
					{
						for (int h = 0; h < num_halfers_; ++h) {
							for (int r = 0; r < 1<<(num_halfers_-h); ++r) {
								if (h == 0) {
									halfers_[h].tick(tempbuffer_[n++]);
									k++;
								}
								else {
									halfers_[h].tick(halfers_[h-1].getLastOut());
								}
							}
						}
						push_sample(halfers_[num_halfers_-1].getLastOut());
					} 
					else {
						push_sample(tempbuffer_[n++]);
						k++;
					}
				}	
				topull = int(ceil(float_t(k-1)/conversion_ / (float_t(1<<num_halfers_)) ));
				do 
				{
					InOut[m++] = (sample_t)(clip(read_sample()));
				} while (m < inmaxsize && fabsf(head_distance_) > 3);
			}
			if (num_halfers_ > 0) {
				::memmove(tempbuffer_,&tempbuffer_[incount],leftover_len_*sizeof(sample_t));
			}
			return m;
		}
		else {
			if (canThrow) {
				throw resampling_error("Output buffer is insufficient", req_buf_size);
			}
		}
		return incount;
	}

	void set_rates(int input_rate, int output_rate) {
		input_rate_ = input_rate;
		output_rate_ = output_rate;

		num_halfers_ = 0;
		leftover_len_ = 0;
		while (input_rate /2 >= output_rate) {
			num_halfers_++;
			input_rate /= 2;
		}
		delete[] halfers_;
		halfers_ = 0;
		if (num_halfers_ > 0) {
			halfers_ = new CHalfBandFilter<12,float_t,true>[num_halfers_];
		}

		conversion_ = float_t(double(input_rate)/double(output_rate));
		write_idx_ = 0;
		read_idx_ = (double)write_idx_;
	}
protected:
	void push_sample(float_t in) {
		cbuf_[write_idx_++] = in;
		write_idx_ &= (BUFFER_LENGTH-1);
		update_head_distance();
	}
	float_t read_sample() {
		float_t ret = get_interpolated(read_idx_);
		read_idx_ += double(conversion_);
		if (read_idx_ >= float_t(BUFFER_LENGTH)) {
			read_idx_ -= float_t(BUFFER_LENGTH);
		}
		update_head_distance();
		return ret;
	}
	void update_head_distance() {
		head_distance_ = (float_t)write_idx_ - float_t(read_idx_);
		if (head_distance_ < -float_t(BUFFER_LENGTH-6)) {
			head_distance_ += float_t(BUFFER_LENGTH);
		}
	}
	// interpolator from musicdsp should do fine
	float_t get_interpolated(double location)
	{
		/* 5-point spline*/

		long nearest_sample = (int) floor(location);
		double x = (double) location - (double) nearest_sample;

		double p0=cbuf_[(nearest_sample-2)&(BUFFER_LENGTH-1)];
		double p1=cbuf_[(nearest_sample-1)&(BUFFER_LENGTH-1)];
		double p2=cbuf_[(nearest_sample+0)&(BUFFER_LENGTH-1)];
		double p3=cbuf_[(nearest_sample+1)&(BUFFER_LENGTH-1)];
		double p4=cbuf_[(nearest_sample+2)&(BUFFER_LENGTH-1)];
		double p5=cbuf_[(nearest_sample+3)&(BUFFER_LENGTH-1)];

		return float_t(p2 + 0.04166666666*x*((p3-p1)*16.0+(p0-p4)*2.0
				+ x *((p3+p1)*16.0-p0-p2*30.0- p4
				+ x *(p3*66.0-p2*70.0-p4*33.0+p1*39.0+ p5*7.0- p0*9.0
				+ x *( p2*126.0-p3*124.0+p4*61.0-p1*64.0- p5*12.0+p0*13.0
				+ x *((p3-p2)*50.0+(p1-p4)*25.0+(p5-p0)*5.0))))));
	};
protected:
	sample_t*						tempbuffer_;
	int								tempbuffer_len_;
	int								leftover_len_; //for using halfers in downsampling, length must be divisible by 2**D

	int								num_halfers_;
	CHalfBandFilter<12,float_t,true> *halfers_;
	CHalfBandFilter<12,float_t,true> halfband_;
	int								input_rate_;
	int								output_rate_;
	float_t							conversion_;
	double							read_idx_;
	size_t							write_idx_;
	float_t							head_distance_;
	float_t							cbuf_[BUFFER_LENGTH];

};
#endif
