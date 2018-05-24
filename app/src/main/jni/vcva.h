#include <math.h>

class CVcva
{
public:
	enum state_t {
		VCVA_STATE_INIT = 0,
		VCVA_STATE_IDLE,
		VCVA_STATE_START,
		VCVA_STATE_MUTE,
	};
	CVcva() 
		: m_State(VCVA_STATE_INIT)
		, m_lCount(0)
		, m_lFramesBeforeMute(20)
		, m_fHysteresisDb(2.f)
		, m_lSampleRate(0)
	{
		m_fOffThresholdDb = -200.f;
		m_fOnThresholdDb = -200.f + (float)m_fHysteresisDb;
	};

	long InitializeVCVA(long lSampleRate, long *lCounts) ;
	long IsVoicedFrame(short * pInput, bool * bStatus);
	long SetVCVALevel(long lLevel ) ;

protected:
	state_t							m_State;
	long							m_lCount;
	long							m_lFramesBeforeMute;
	float							m_fOffThresholdDb;
	float							m_fOnThresholdDb;
	float							m_fHysteresisDb;
	long							m_lSampleRate;
	long							m_lCountLength; 
	float							m_fOneOverCount;
};
