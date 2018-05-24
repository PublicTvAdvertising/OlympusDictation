/***ABS******************************************************************
 *                                  vad.c
 * -------------------------------------------------------------------- *
 *
 *   qp_vad - voice activity detection.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:                int   qp_vad(double *s);        i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      Given the current frame of input speech, this function
 *      calculates and returns a voice activity decision: 1 means that
 *      voice activity has been detected, 0 means that there is 
 *      probably noise or silence in the current frame.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    synfrmlen_
 *                              fs_
 *                              NoiseLev_
 *                              NoiseCnt_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   H. Takahashi, Olympus, H. Carl, Grundig
 *
 *    creation date:            Nov-09-1995
 *
 *    modification date:        Aug-12-1997
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Vad.c  $
 * Revision 1.9 2005/06/13 11:41:09CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1996, 1997, Olympus, Grundig
 *
 ************************************************************************/

#include "qp_abs.h"

#define  MIN(a,b)  ((a) < (b) ? (a) : (b))

namespace QualityPlay {

int qp_vad(dsswork& w, double *s)
{
	int            i, synfs;
	double         accu;
	double         P;
	double         Thr;
	double         rs[400];

	synfs = w.wk.synfrmlen_*w.wk.fs_;

	if (w.wk.inv_filt_)
		/* apply inverse filtering with noise adapted coefficients */
		qp_latfilnr(s, synfs, synfs, w.wk.np_, w.wk.Noise_k_, w.wk.qp_vad_mem, rs);
	else
		/* just copy the input speech frame */
		for (i=0; i < synfs; i++) rs[i] = s[i];

	/* compute the frame energy */
	for (accu=0, i=0; i<synfs; i++) 
		accu += rs[i] * rs[i];

	P = accu / synfs;

	/* update noise level estimate */
	w.wk.Pn_ *= 1.015625; //Pn_ *= 1.015625;
	if (w.wk.Pn_ > P) 
		w.wk.Pn_ = P;
	if (w.wk.Pn_ < w.wk.LowerNoiseLimit_) 
		w.wk.Pn_ = w.wk.LowerNoiseLimit_;
	if (w.wk.Pn_ > w.wk.UpperNoiseLimit_) 
		w.wk.Pn_ = w.wk.UpperNoiseLimit_;
	if (w.wk.frm_cnt_++ < 50)        /* accelerate noise level estimate setup */
		w.wk.Pmin_ = MIN(w.wk.Pmin_, P);
	else
		if (w.wk.no_silence_)
			if (w.wk.Pn_ < w.wk.Pmin_)
				w.wk.Pn_ *= 1.046875;

	/* treshold computation */
	//Thr = 0.00000762939453125 * Pn_ * Pn_ + 5 * Pn_;
	Thr = 0.00000762939453125 * w.wk.Pn_ * w.wk.Pn_ + w.wk.VadHangCnt_ * w.wk.Pn_;

	/* compare frame energy with threshold */
	if (P > Thr)
		w.wk.NoiseCnt_ = 0;
	else
		w.wk.NoiseCnt_ ++;

	/* check with preceding VadHangCnt_ frames */
	if (w.wk.NoiseCnt_ > w.wk.VadHangCnt_) {
		w.wk.NoiseCnt_ = w.wk.VadHangCnt_ + 1;  /* overflow prevention for long silence periods */
		return 0;
	}
	else
		return 1;
}


}