/*
#include "stdafx.h"

#include <comdef.h>
#include <malloc.h>
*/
#include <android/log.h>
#include "vcva.h"

static const float kMuteLevels[10] = {	
										-13.16f,	//  20 * log(12821/32767 * 1/sqrt(2))
										-14.04f,	//  20 * log(11588/32767 * 1/sqrt(2))
										-15.02f,	// ...
										-16.12f, 
										-17.38f, 
										-18.85f, 
										-20.63f, 
										-22.87f, 
										-25.89f, 
										-30.57f,
									};

// InitializeVCVA(long lSampleRate, long *lCounts)
//
// Details: Initialize VCVA object and obtain number of sample for one frame
//			You have to call this method initially.
// Parameters:
// lSampleRate	sampling rate for PCM, 16bit, mono
// lCounts		Returned number of sample for one frame(30ms)

long CVcva::InitializeVCVA(long lSampleRate, long *lCounts)
{
	m_lSampleRate = lSampleRate;

	m_lCountLength = (long)ceil(0.03*double(lSampleRate));
	m_fOneOverCount = 1.f/float(m_lCountLength);

	*lCounts = m_lCountLength ;
	return 0 ;
}

// IsVoicedFrame(short * pInput, BOOL * bStatus)
//
// Details: Judge if a frame is voice or unvoice
// Parameters:
// pInput		Pointer to the input buffer for one frame
// bStatus		Returned status which a frame is voice/unvoice
//              If a frame is voice, true is returned, 
//				you can record this input buffer as recording data

long CVcva::IsVoicedFrame(short * pInput, bool * bStatus)
{
		/*for ( int k = 0 ; k < m_lCountLength ; k++ )
		{
			__android_log_print(ANDROID_LOG_VERBOSE, "VCVA", "vcva pInput- %d : %u ",k,pInput[k]);
		}*/

	int i ;
	float fRunningSum = 0 ;
	float fSample ;

	*bStatus = false ;

	if (m_lSampleRate == 0 )
		return -1 ;

	for ( i = 0 ; i < m_lCountLength ; i++ )
	{
		//__android_log_print(ANDROID_LOG_VERBOSE, "VCVA", "vcva pInput- %d : %u ",i,pInput[i]);


		fSample = ((float)pInput[i]) /32768.0f ;
		fRunningSum += fSample * fSample * m_fOneOverCount;



	}
	if( fRunningSum > 0.f )
		fRunningSum = 10.f*log10f(fRunningSum) ;
	else
		fRunningSum = -1e10f ;

	// Voice/Unvoice calcuration
	switch (m_State)
	{
		case VCVA_STATE_INIT:
		case VCVA_STATE_IDLE:
			if (fRunningSum < m_fOffThresholdDb) 
			{
				m_State = VCVA_STATE_START;
			}
			break;
		case VCVA_STATE_START:
			if (fRunningSum < m_fOffThresholdDb)
			{
				++m_lCount;
				if (m_lCount > m_lFramesBeforeMute) {
					m_State = VCVA_STATE_MUTE;
				}
			}
			else 
			{
				m_State = VCVA_STATE_IDLE;
				m_lCount = 0;
			}
			break;
		case VCVA_STATE_MUTE:
			if (fRunningSum > m_fOnThresholdDb)
			{
				m_State = VCVA_STATE_IDLE;
				m_lCount = 0;
			}
	}
	if (m_State != VCVA_STATE_MUTE)
		* bStatus = true ;

	return 0;
}

// SetVCVALevel(long lLevel )
//
// Details: Change the VCVA threshold level
//			You can call this method anytime
// Parameters:
// lLevel	You can specify the value in 0 to 9
// 
long CVcva::SetVCVALevel(long lLevel )
{
	if (lLevel > 9 || lLevel < 0 ) {
		return -1 ;
	}

	m_fOffThresholdDb = kMuteLevels[lLevel];
	m_fOnThresholdDb = kMuteLevels[lLevel] + (float)m_fHysteresisDb;

	return 0;

}
