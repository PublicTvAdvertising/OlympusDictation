/***ABS******************************************************************
 *								  absdec.c
 * -------------------------------------------------------------------- *
 *
 *	 absdec  -	analysis-by-synthesis decoder simulation.
 *
 * -------------------------------------------------------------------- *
 *
 *	  description:
 *
 *		This program simulates an analysis-by-synthesis linear-
 *		predictive decoder.
 *
 *		usage:	absdec infile outfile
 *
 * -------------------------------------------------------------------- *
 *
 *	  calls routines:			readcode
 *								abs_syn
 *								absdinit
 *								wrtspch
 *
 * -------------------------------------------------------------------- *
 *
 *	  uses global variables:	fs_
 *								synfrmlen_
 *								LTPfrmlen_
 *								nimps_
 *
 * -------------------------------------------------------------------- *
 *
 *	  author:					Holger Carl, AGDSV-RUB, Grundig
 *
 *	  creation date:			Jan-10-1995
 *
 *	  modification date:		Feb-2-1996
 *
 ************************************************************************
 *
 *	  Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

//#include <windows.h>
#include "DssQpEnc.h"

#ifndef max
#define max(a,b)			(((a) > (b)) ? (a) : (b))
#endif


using namespace QualityPlay;

bool DssQpEnc::init() {
	/* --- initialization --- */
	qp_abseinit(w);

	/* --- compute array lengths --- */
	A = w.wk.anlfrmlen_*w.wk.fs_;				  /* all times expressed in  */
	S = w.wk.synfrmlen_*w.wk.fs_;				  /*	 multiples of input  */
	E = (int)(w.wk.excveclen_*w.wk.fs_);		  /*		 sampling period */
	R = max(A/2, S/2);
	lens = A/2+R;
	lsp = A/2+R-S;
	nsbfrms = S/E;

	/* --- allocate memory --- */
	s = new double[lens];
	memset(s, 0, sizeof(double)*lens);
	lpci = new short[w.wk.np_];
	memset(lpci, 0, sizeof(short)*w.wk.np_);
	Mi = new short[nsbfrms]; 
	memset(Mi, 0, sizeof(short)*nsbfrms);
	betai = new short[nsbfrms];
	memset(betai, 0, sizeof(short)*nsbfrms);
	ei = new __int64_t[nsbfrms];
	memset(ei, 0, sizeof(__int64_t)*nsbfrms);
	ai = new short *[nsbfrms];
	for (int i = 0; i < nsbfrms; i++) {
		ai[i] = new short[w.wk.nimps_+1];
		memset(ai[i], 0, sizeof(short)*(w.wk.nimps_+1));
	}
	block_num_ = 0;
	return (is_inited_ = true);
}
void DssQpEnc::flush()
{
	IO_Flush(w);
}
int DssQpEnc::get_frame_length(byte_t* frame, int mode) {
	bool is_voiced;
	BitReader breader((word_t *)frame, 0, 4);
	long frame_header = breader.getbits(1);

	is_voiced = (1 == frame_header);

	if (mode & 0x1)
		return (is_voiced) ? 328/8 : 72/8;
	else 
		return 328/8;
}
long DssQpEnc::get_remaining() {
	return IO_GetTotalInputBytes(w, 0);
}

int DssQpEnc::encode_frames(short *psInput, int cbInSamples, byte_t *output, int& cbOutBytes, int mode)
{
	IO_PushToReadQueue(w, (unsigned char *)psInput, cbInSamples*sizeof(short));

	int silence_compression = (mode & 0x1);
	if (silence_compression != qp_get_silence_compression(w) || w.wk.fs_ == 0) {
		finish();
		qp_set_silence_compression(w, silence_compression);
		init();
	}

	*((short *)output) = mode;
	output += 2;
	cbOutBytes -= 2;

	if (block_num_ == 0) {
		qp_readspch(w, 0, lsp, s);
	}

	short			va;
	int frm= 0;
	/* --- coder loop --- */
	for (frm=1; IO_GetTotalInputBytes(w, 0) > (S + lsp)*sizeof(short) && qp_readspch(w, 0, S, s+lsp) > 0; frm++)
	   {
	   qp_abs_anl(w, s, &va, lpci, Mi, betai, ei, ai);

	   qp_wrtcode(w, 0, va, lpci, Mi, betai, ei, ai);

	   for (int i=0; i<lsp; i++) s[i] = s[i+lens-lsp];
	   }

//	putbits(0, 0, 0);
	++block_num_;
	cbOutBytes = 2+ IO_SaveOutQueueB(w, (char *)output, cbOutBytes);
	return cbInSamples;
}
void DssQpEnc::finish() {
	qp_readspch(w, 0,-1,0);
	qp_putbits(w, 0,0,0);
	qp_abs_anl(w, 0,0,0,0,0,0,0);
	qp_abs_anl_syn(w, 0,0);
	qp_excitation_evaluation(w, 0,0,0,0,0,0,0);
	qp_LTP_analysis(w, 0,0,0,0,0,0);
	qp_adaptive_codebook_search(w, 0,0,0,0,0);

	if (w.wk.nblpc_){
		free(w.wk.nblpc_);
		w.wk.nblpc_ = 0;
	}
	delete[] lpci; 
	lpci = 0;
	delete[] Mi;
	Mi = 0;
	delete[] betai;
	betai = 0;
	delete[] ei;
	ei = 0;
	if (ai != 0) {
		for (int i = 0; i < nsbfrms; i++) {
			if (ai[i] != 0) {
				delete[] ai[i];
				ai[i] = 0;
			}
		}
	}
	delete[] ai;
	ai = 0;
	delete[] s;
	s = 0;

	/* unset all parameters */
	w.wk.synfrmlen_ 	 = 0;

/* input */
	w.wk.hdrlen_		 = 0; /* always 44 in wave files*/
	w.wk.notchfilter_	 = 0;
	w.wk.emphasiscoeff_  = 0.;

/* LPC */
	w.wk.anlfrmlen_ 	 = 0;
	w.wk.stab_			 = 0.;
	w.wk.bwe_			 = 0.;

/* excitation */
	w.wk.excveclen_ 	 = 0.;

/* adaptive excitation */
	w.wk.minlag_		 = 0;
	w.wk.maxlag_		 = 0;
	w.wk.difflagrange_	 = 0;
	w.wk.alphaa_		 = 0.;
	w.wk.lha_			 = 0.;

/* innovation */
	w.wk.lhm_			 = 0.;
	w.wk.alpham_		 = 0.;
	w.wk.nimps_ 		 = 0;

/* silence compression */
//	scon_ = 0;
	w.wk.reset_memories_ = 0;

/* quantization */
	w.wk.lpcquant_		 = 0;
	w.wk.ltpquant_		 = 0;
	w.wk.ampquant_		 = 0;


	w.wk.NoiseCnt_ = 0;
	w.wk.Pn_ = 0.;				   // o
	w.wk.Pmin_ = 0;
	w.wk.frm_cnt_ = 0;
	w.wk.no_silence_ = 0;
	w.wk.LowerNoiseLimit_ = 0.;    // o
	w.wk.UpperNoiseLimit_ = 0.; // o
	w.wk.VadHangCnt_ = 0;
	w.wk.seed_ = 0;
	w.wk.inv_filt_ = 0;
	for (int i=0 ; i<w.wk.np_ ; i++) {
			w.wk.Noise_k_[i] = 0;
	}

	w.wk.nbga_ = 0;
	w.wk.nbgm_ = 0;
	w.wk.nbp_ = 0;
	w.wk.nbgn_ = 5;
	w.wk.np_			 = 0;
	is_inited_ = false;

}