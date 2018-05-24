/***ABS******************************************************************
 *                                 abs_anl.c
 * -------------------------------------------------------------------- *
 *
 *   abs_anl  -  analysis (encoding).
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void qp_abs_anl(double *s,                i
 *                                   short *va,                o
 *                                   short *lpci,              o
 *                                   short *Mi,                o
 *                                   short *betai,             o
 *                                   unsigned long *ei,        o
 *                                   short **ai);              o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine processes one (analysis) frame of the input
 *      signal s(n) according to the analysis-by-synthesis coding
 *      principle and returns the encoded parameters.
 *
 *      This version integrates several functions which have been
 *      modified in order to apply integer arithmetic, only.
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           qp_lpc_analysis
 *                              qp_fw_imp
 *                              acf
 *                              qp_latfilnr
 *                              qp_LTP_analysis
 *                              qp_excitation_evaluation
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    np_
 *                              fs_
 *                              anlfrmlen_
 *                              excveclen_
 *                              synfrmlen_
 *                              lha_
 *                              lhm_
 *                              alphaa_
 *                              alpham_
 *                              minlag_
 *                              maxlag_
 *                              difflagrange_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Feb-02-1996
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Abs_anl.c  $
 * Revision 1.7 2005/06/13 11:39:37CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#include "qp_pow2.h"

#define MIN(a,b) ((a)<(b) ? (a):(b))
#define MAX(a,b) ((a)>(b) ? (a):(b))

namespace QualityPlay {

void qp_abs_anl(dsswork& w, double *s, short *va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai)
{
	static const double    gnqtbl[] =
	#include "qp_Gnqtbld.h"

	double			*k;
	double			*a;
	double			*kltp;
	double			*hltp;
	double			*kmpe;
	double			*hmpe;
	double			*hhmpe;
	double			*r;
	int				i, n;
	double			en;
	double			aux;
	double			min;
	int				M, Mold;
	double			*sptr;

	/* --- initialize --- */
	if (!w.wk.qp_abs_anl_init) {
		w.wk.qp_abs_anl_A = w.wk.anlfrmlen_*w.wk.fs_;
		w.wk.qp_abs_anl_S = w.wk.synfrmlen_*w.wk.fs_;
		w.wk.qp_abs_anl_N = (int)(w.wk.excveclen_*w.wk.fs_);
		w.wk.qp_abs_anl_memb_if = (double *)calloc(w.wk.np_, sizeof(double));
		w.wk.qp_abs_anl_e = (double *)calloc(w.wk.qp_abs_anl_N, sizeof(double));
		w.wk.qp_abs_anl_esave = (double *)calloc(w.wk.qp_abs_anl_N, sizeof(double));
		if (w.wk.qp_abs_anl_memb_if == NULL || w.wk.qp_abs_anl_e == NULL || w.wk.qp_abs_anl_esave == NULL) {
			fprintf(ERROROUT, "\nqp_abs_anl init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_abs_anl_init = 1;
	}
	if (0 == s && 0 == va && 0 == lpci && 0 == Mi && 0 == betai && 0 == ei && 0 == ai) {
		free(w.wk.qp_abs_anl_memb_if);	w.wk.qp_abs_anl_memb_if = 0;
		free(w.wk.qp_abs_anl_e);		w.wk.qp_abs_anl_e = 0;
		free(w.wk.qp_abs_anl_esave);	w.wk.qp_abs_anl_esave = 0;
		w.wk.qp_abs_anl_init = 0;
		return;
	}

	/* --- allocate memory --- */
	k     = (double *)calloc(w.wk.np_, sizeof(double));
	a     = (double *)calloc(w.wk.np_+1, sizeof(double));
	kltp  = (double *)calloc(w.wk.np_, sizeof(double));
	hltp  = (double *)calloc((int)(w.wk.lha_*w.wk.fs_), sizeof(double));
	kmpe  = (double *)calloc(w.wk.np_, sizeof(double));
	hmpe  = (double *)calloc((int)(w.wk.lhm_*w.wk.fs_), sizeof(double));
	hhmpe = (double *)calloc(w.wk.qp_abs_anl_N, sizeof(double));
	r     = (double *)calloc(w.wk.qp_abs_anl_N, sizeof(double));
	if (k==NULL || a==NULL || kltp==NULL || hltp==NULL ||
		kmpe==NULL || hmpe==NULL || hhmpe==NULL || r==NULL) {
		fprintf(ERROROUT, "\nqp_abs_anl: memory allocation error!\n\n");
		exit(1);
	}

	/* synthesis frame pointer in input buffer */
	sptr = s + w.wk.qp_abs_anl_A/2 - w.wk.qp_abs_anl_S/2;

	/* perform VAD */
	if (w.wk.scon_)
		*va = qp_vad(w, sptr);
	else
		*va = 1;

	/* --- LPC analysis --- */
	qp_lpc_analysis(w, s, w.wk.qp_abs_anl_A, w.wk.np_, k, a, lpci);

	/* --- precomputations --- */
	if (*va) {
		/* --- compute weighted coefficients and truncated impulse responses --- */
		qp_fw_imp(a, w.wk.np_, w.wk.alphaa_, kltp, (int)(w.wk.lha_*w.wk.fs_), hltp);
		qp_fw_imp(a, w.wk.np_, w.wk.alpham_, kmpe, (int)(w.wk.lhm_*w.wk.fs_), hmpe);
		qp_acf(hmpe, (int)(w.wk.lhm_*w.wk.fs_), w.wk.qp_abs_anl_N, hhmpe);
		for (i=(int)(w.wk.lhm_*w.wk.fs_); i<w.wk.qp_abs_anl_N; i++) 
			hhmpe[i] = 0;
	}
	else {
		if (w.wk.inv_filt_) {   /* update the noise adapted LPC coefficients */
			for (i=0; i<w.wk.np_; i++) {
				w.wk.Noise_k_[i] = w.wk.Noise_k_[i] * 0.9375 + k[i] * 0.0625;
			}
		}
		w.wk.reset_memories_ = 1;    /* for next transition to VA = 1 */
	}

	/* --- loop over subframes --- */
	for (i=0; i<w.wk.qp_abs_anl_S/w.wk.qp_abs_anl_N; i++) {

		/* --- inverse filtering --- */
		qp_latfilnr(sptr+i*w.wk.qp_abs_anl_N, w.wk.qp_abs_anl_N, w.wk.qp_abs_anl_N, w.wk.np_, k, w.wk.qp_abs_anl_memb_if, r);

		if (*va)
		{
			/* --- restore last VA excitation vector for correct adaptive codebook update --- */
			if (w.wk.reset_memories_) {
				for (n=0; n<w.wk.qp_abs_anl_N; n++) 
					w.wk.qp_abs_anl_e[n] = w.wk.qp_abs_anl_esave[n];
			}

			/* --- LTP analysis --- */
			if (i==0) 
				M = 0; 
			else 
				Mold = M;

			qp_LTP_analysis(w, r, kltp, hltp, w.wk.qp_abs_anl_e, &M, betai+i);
			
			if (i==0)
				Mi[0] = M - w.wk.minlag_;
			else {
				if (Mold <= w.wk.maxlag_-w.wk.difflagrange_/2)
					Mi[i] = M - MAX(w.wk.minlag_, Mold-w.wk.difflagrange_/2+1);
				else
					Mi[i] = M - MAX(w.wk.minlag_, w.wk.maxlag_-w.wk.difflagrange_+1);
			}

			/* --- determine optimal excitation for LPC synthesis filter --- */
			qp_excitation_evaluation(w, r, kmpe, hmpe, hhmpe, w.wk.qp_abs_anl_e, ei+i, ai[i]);
		}
		else {

			/* --- calculate residual energy --- */
			for (en=0,n=0; n<w.wk.qp_abs_anl_N; n++) 
				en += r[n] * r[n];

			/* --- adjust noise level by choosing best qp_quantized gain value --- */
			aux = en / (2.*w.wk.qp_abs_anl_N*32768.*32768.);  /* normalized power - 3dB */
			min = 1.e200;
			for (n=0; n<pow2[w.wk.nbgn_]; n++) {
				if (fabs(gnqtbl[n]*gnqtbl[n]/3. - aux) < min)
				{
					min = fabs(gnqtbl[n]*gnqtbl[n]/3. - aux); 
					ai[i][0] = n;
				}
			}
		}
	}

	/* --- save VA excitation vector --- */
	if (*va) for (n=0; n<w.wk.qp_abs_anl_N; n++) 
		w.wk.qp_abs_anl_esave[n] = w.wk.qp_abs_anl_e[n];

	free(k);
	free(a);
	free(kltp);
	free(hltp);
	free(kmpe);
	free(hmpe);
	free(hhmpe);
	free(r);
}

}
