#ifndef HALF_BAND_FILTER_H__
#define HALF_BAND_FILTER_H__



template <int ORDER, typename T>
class CAllPassCascade
{
public:
	CAllPassCascade(){ memset(arrAllpass,0,sizeof(arrAllpass)); lastOut = (T)0.;}
	virtual ~CAllPassCascade(){;}

	void	reset (void);
	void	setCoefs(T* ptK) {
		for (int n=0;n<ORDER;++n) 
			arrAllpass[ORDER-n-1].a = ptK[n]; 
	}
	inline T		tick  (T In) {
		lastOut = In; 
		_AllPassStage<ORDER-1, T>::f(&arrAllpass[0], &lastOut); 
		return lastOut;
	}
	void	tickN (T* In, int N) { for (int n=0;n<N;++n) In[n] = tick(In[n]); }
	inline T		getLastOut(void) { return lastOut;	}
protected:
	T		lastOut;
	typedef struct{
			T a;
			T x0;	T x1;	T x2;
			T y0;	T y1;	T y2;
	} stAllPass;
	stAllPass	arrAllpass[ORDER];

	template<int I, typename S>
	class _AllPassStage {
	private:
		enum { go = (I-1) != 0 };
	public:
		static inline void f(stAllPass* apf, S* Input)
		{
			apf[I].x2 = apf[I].x1;
			apf[I].x1 = apf[I].x0;
			apf[I].x0 = *Input;

			apf[I].y2 = apf[I].y1;
			apf[I].y1 = apf[I].y0;

			S output = apf[I].x2 + ((*Input - apf[I].y2)*apf[I].a);
			apf[I].y0 = output;
			*Input = output;

			_AllPassStage<go ? (I-1) : 0 , S>::f(apf, Input);
		}
	};
	template<typename S>
	class _AllPassStage<0, S> {
	public:
		static inline void f(stAllPass* apf, S* Input)
		{
			apf[0].x2 = apf[0].x1;
			apf[0].x1 = apf[0].x0;
			apf[0].x0 = *Input;

			apf[0].y2 = apf[0].y1;
			apf[0].y1 = apf[0].y0;

			S output = apf[0].x2 + ((*Input - apf[0].y2)*apf[0].a);
			apf[0].y0 = output;
			*Input = output;
		}
	};
};

template <int ORDER, typename T, bool IS_STEEP>
class CHalfBandFilter
{
protected:
	CAllPassCascade<ORDER/2, T> ap_a;
	CAllPassCascade<ORDER/2, T> ap_b;
	T	lastOut;
public:
	CHalfBandFilter()
	{
		if (IS_STEEP==true)
		{
			if (ORDER==12)	//rejection=104dB, transition band=0.01
			{
				T a_coefficients[6]=
				{(T)0.036681502163648017
				,(T)0.2746317593794541
				,(T)0.56109896978791948
				,(T)0.769741833862266
				,(T)0.8922608180038789
				,(T)0.962094548378084
				};

				T b_coefficients[6]=
				{(T)0.13654762463195771
				,(T)0.42313861743656667
				,(T)0.6775400499741616
				,(T)0.839889624849638
				,(T)0.9315419599631839
				,(T)0.9878163707328971
				};
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==10)	//rejection=86dB, transition band=0.01
			{
				T a_coefficients[5]=
				{(T)0.051457617441190984
				,(T)0.35978656070567017
				,(T)0.6725475931034693
				,(T)0.8590884928249939
				,(T)0.9540209867860787
				};

				T b_coefficients[5]=
				{(T)0.18621906251989334
				,(T)0.529951372847964
				,(T)0.7810257527489514
				,(T)0.9141815687605308
				,(T)0.985475023014907
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==8)	//rejection=69dB, transition band=0.01
			{
				T a_coefficients[4]=
				{(T)0.07711507983241622
				,(T)0.4820706250610472
				,(T)0.7968204713315797
				,(T)0.9412514277740471
				};

				T b_coefficients[4]=
				{(T)0.2659685265210946
				,(T)0.6651041532634957
				,(T)0.8841015085506159
				,(T)0.9820054141886075
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==6)	//rejection=51dB, transition band=0.01
			{
				T a_coefficients[3]=
				{(T)0.1271414136264853
				,(T)0.6528245886369117
				,(T)0.9176942834328115
				};

				T b_coefficients[3]=
				{(T)0.40056789819445626
				,(T)0.8204163891923343
				,(T)0.9763114515836773
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==4)	//rejection=53dB,transition band=0.05
			{
				T a_coefficients[2]=
				{(T)0.12073211751675449
				,(T)0.6632020224193995
				};

				T b_coefficients[2]=
				{(T)0.3903621872345006
				,(T)0.890786832653497
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
		
			else	//order=2, rejection=36dB, transition band=0.1
			{
				T a_coefficients=(T)0.23647102099689224;
				T b_coefficients=(T)0.7145421497126001;

				ap_a.setCoefs(&a_coefficients);
				ap_b.setCoefs(&b_coefficients);
			}
		}
		else	//softer slopes, more attenuation and less stopband ripple
		{
			if (ORDER==12)	//rejection=150dB, transition band=0.05
			{
				T a_coefficients[6]=
				{(T)0.01677466677723562
				,(T)0.13902148819717805
				,(T)0.3325011117394731
				,(T)0.53766105314488
				,(T)0.7214184024215805
				,(T)0.8821858402078155
				};

				T b_coefficients[6]=
				{(T)0.06501319274445962
				,(T)0.23094129990840923
				,(T)0.4364942348420355
				,(T)0.6329609551399348
				,(T)0.80378086794111226
				,(T)0.9599687404800694
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==10)	//rejection=133dB, transition band=0.05
			{
				T a_coefficients[5]=
				{(T)0.02366831419883467
				,(T)0.18989476227180174
				,(T)0.43157318062118555
				,(T)0.6632020224193995
				,(T)0.860015542499582
				};

				T b_coefficients[5]=
				{(T)0.09056555904993387
				,(T)0.3078575723749043
				,(T)0.5516782402507934
				,(T)0.7652146863779808
				,(T)0.95247728378667541
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==8)	//rejection=106dB, transition band=0.05
			{
				T a_coefficients[4]=
				{(T)0.03583278843106211
				,(T)0.2720401433964576
				,(T)0.5720571972357003
				,(T)0.827124761997324
				};

				T b_coefficients[4]=
				{(T)0.1340901419430669
				,(T)0.4243248712718685
				,(T)0.7062921421386394
				,(T)0.9415030941737551
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==6)	//rejection=80dB, transition band=0.05
			{
				T a_coefficients[3]=
				{(T)0.06029739095712437
				,(T)0.4125907203610563
				,(T)0.7727156537429234
				};

				T b_coefficients[3]=
				{(T)0.21597144456092948
				,(T)0.6043586264658363
				,(T)0.9238861386532906
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
			else if (ORDER==4)	//rejection=70dB,transition band=0.1
			{
				T a_coefficients[2]=
				{(T)0.07986642623635751
				,(T)0.5453536510711322
				};

				T b_coefficients[2]=
				{(T)0.28382934487410993
				,(T)0.8344118914807379
				};
		
				ap_a.setCoefs(a_coefficients);
				ap_b.setCoefs(b_coefficients);
			}
		
			else	//order=2, rejection=36dB, transition band=0.1
			{
				T a_coefficients=(T)0.23647102099689224;
				T b_coefficients=(T)0.7145421497126001;

				ap_a.setCoefs(&a_coefficients);
				ap_b.setCoefs(&b_coefficients);
			}
		}
		lastOut=0.0;
	};
	~CHalfBandFilter(){;}

	inline T tickQuick(T input)
	{
		const T output=(ap_a.tick(input)+ap_b.getLastOut())*static_cast<T>(0.5);
		ap_b.tick(input);
		return (lastOut=output);
	}
	virtual T tick(T input){
		return tickQuick(input);
	}

	T getLastOut(void) {
		return lastOut;
	}
};

#endif
