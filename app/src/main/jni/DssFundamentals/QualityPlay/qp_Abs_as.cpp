/***ABS******************************************************************
 *                                 abs_as.c
 * -------------------------------------------------------------------- *
 *
 *   qp_abs_anl_syn  -  analysis and synthesis.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void qp_abs_anl_syn(double *s,            i
 *                                       double *shat,);       o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine processes one (analysis) frame of the input
 *      signal s(n) according to the analysis-by-synthesis coding
 *      principle and returns the resynthesized signal shat(n).
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           qp_lpc_analysis
 *                              qp_fw_imp
 *                              acf
 *                              qp_latfilnr
 *                              qp_LTP_analysis
 *                              qp_excitation_evaluation
 *                              latfilr
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    np_
 *                              fs_
 *                              excveclen_
 *                              synfrmlen_
 *                              anlfrmlen_
 *                              lha_
 *                              lhm_
 *                              alphaa_
 *                              alpham_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Nov-08-1993
 *
 *    modification date:        Aug-03-1995
 *                              Jul-29-2003 by Michael Kroener
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Abs_as.c  $
 * Revision 1.13 2005/06/13 11:39:38CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#include "qp_pow2.h"

namespace QualityPlay {

void qp_abs_anl_syn(dsswork& w, double *s, double *shat)
{
	static const double	gnqtbl[] =
					#include "qp_Gnqtbld.h"

	double			*k;
	double			*a;
	short			*lpci;
	double			*kltp;
	double			*hltp;
	double			*kmpe;
	double			*hmpe;
	double			*hhmpe;
	double			*r;
	int				i, n;
	int				va;
	double			en;
	double			aux;
	double			min;
	int				M;
	short			betai;
	__int64_t			indexe;
	short			*indicesa;
	double			*sptr;

	/* --- initialize --- */
	if (!w.wk.qp_abs_anl_syn_init) {
		w.wk.qp_abs_anl_syn_A = w.wk.anlfrmlen_*w.wk.fs_;
		w.wk.qp_abs_anl_syn_S = w.wk.synfrmlen_*w.wk.fs_;
		w.wk.qp_abs_anl_syn_N = (int)(w.wk.excveclen_*w.wk.fs_);
		w.wk.qp_abs_anl_syn_memb_if = (double *)calloc(w.wk.np_, sizeof(double));
		w.wk.qp_abs_anl_syn_mema_sf = (double *)calloc(w.wk.np_, sizeof(double));
		w.wk.qp_abs_anl_syn_e       = (double *)calloc(w.wk.qp_abs_anl_syn_N  , sizeof(double));
		w.wk.qp_abs_anl_syn_esave   = (double *)calloc(w.wk.qp_abs_anl_syn_N  , sizeof(double));
		if (w.wk.qp_abs_anl_syn_memb_if==NULL || w.wk.qp_abs_anl_syn_mema_sf==NULL || w.wk.qp_abs_anl_syn_e==NULL || w.wk.qp_abs_anl_syn_esave==NULL) {
			fprintf(ERROROUT, "\nabs_anl_syn init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_abs_anl_syn_init = 1;
	}
	if (0 == s && 0 == shat) {
		free(w.wk.qp_abs_anl_syn_memb_if);	w.wk.qp_abs_anl_syn_memb_if = 0;
		free(w.wk.qp_abs_anl_syn_mema_sf);	w.wk.qp_abs_anl_syn_mema_sf = 0;
		free(w.wk.qp_abs_anl_syn_e);		w.wk.qp_abs_anl_syn_e = 0;
		free(w.wk.qp_abs_anl_syn_esave);	w.wk.qp_abs_anl_syn_esave = 0;
		w.wk.qp_abs_anl_syn_init = 0;
		return;
	}

	/* --- allocate memory --- */
	k        = (double *)calloc(w.wk.np_              , sizeof(double));
	a        = (double *)calloc(w.wk.np_+1            , sizeof(double));
	lpci     = (short *) calloc(w.wk.np_              , sizeof(short) );
	kltp     = (double *)calloc(w.wk.np_              , sizeof(double));
	hltp     = (double *)calloc((short)(w.wk.lha_*w.wk.fs_), sizeof(double));
	kmpe     = (double *)calloc(w.wk.np_              , sizeof(double));
	hmpe     = (double *)calloc((short)(w.wk.lhm_*w.wk.fs_), sizeof(double));
	hhmpe    = (double *)calloc(w.wk.qp_abs_anl_syn_N                , sizeof(double));
	r        = (double *)calloc(w.wk.qp_abs_anl_syn_N                , sizeof(double));
	indicesa = (short *) calloc(w.wk.nimps_+2         , sizeof(short) );

	if (k==NULL    || a==NULL    || lpci==NULL  || kltp==NULL || hltp==NULL ||
		kmpe==NULL || hmpe==NULL || hhmpe==NULL || r==NULL    || indicesa==NULL) {
		fprintf(ERROROUT, "\nabs_anl_syn: memory allocation error!\n\n");
		exit(1);
	}

	/* synthesis frame pointer in input buffer */
	sptr = s + w.wk.qp_abs_anl_syn_A/2 - w.wk.qp_abs_anl_syn_S/2;

	/* perform VAD */
	if (w.wk.scon_)
		va = qp_vad(w, sptr);
	else
		va = 1;

	/* --- LPC analysis --- */
	qp_lpc_analysis(w, s, w.wk.qp_abs_anl_syn_A, w.wk.np_, k, a, lpci);

	/* --- precomputations --- */
	if (va)	{
		qp_fw_imp(a, w.wk.np_, w.wk.alphaa_, kltp, (int)(w.wk.lha_*w.wk.fs_), hltp);
		qp_fw_imp(a, w.wk.np_, w.wk.alpham_, kmpe, (int)(w.wk.lhm_*w.wk.fs_), hmpe);
		qp_acf(hmpe, (int)(w.wk.lhm_*w.wk.fs_), w.wk.qp_abs_anl_syn_N, hhmpe);
		for (i=(int)(w.wk.lhm_*w.wk.fs_); i<w.wk.qp_abs_anl_syn_N; i++) 
			hhmpe[i] = 0;
	}
	else {
		if (w.wk.inv_filt_)   /* update the noise adapted LPC coefficients */
			for (i=0; i<w.wk.np_; i++)
				w.wk.Noise_k_[i] = w.wk.Noise_k_[i] * 0.9375 + k[i] * 0.0625;
		w.wk.reset_memories_ = 1;   /* for next transition to VA = 1 */
	}

	/* --- loop over subframes --- */
	for (i=0; i<w.wk.qp_abs_anl_syn_S/w.wk.qp_abs_anl_syn_N; i++) {

		/* --- compute LPC residual --- */
		qp_latfilnr(sptr+i*w.wk.qp_abs_anl_syn_N, w.wk.qp_abs_anl_syn_N, w.wk.qp_abs_anl_syn_N, w.wk.np_, k, w.wk.qp_abs_anl_syn_memb_if, r);

		if (va)	{
		/* --- restore last VA excitation vector for correct adaptive codebook update --- */
			if (w.wk.reset_memories_)
				for (n=0; n<w.wk.qp_abs_anl_syn_N; n++) 
					w.wk.qp_abs_anl_syn_e[n] = w.wk.qp_abs_anl_syn_esave[n];

			/* --- LTP analysis --- */
			if (i==0) 
				M = 0;

			qp_LTP_analysis(w, r, kltp, hltp, w.wk.qp_abs_anl_syn_e, &M, &betai);

			/* --- determine optimal excitation for LPC synthesis filter --- */
			qp_excitation_evaluation(w, r, kmpe, hmpe, hhmpe, w.wk.qp_abs_anl_syn_e, &indexe, indicesa);

		}
		else {
			/* --- calculate residual energy --- */
			for (en=0,n=0; n<w.wk.qp_abs_anl_syn_N; n++) 
				en += r[n] * r[n];

			/* --- adjust noise level by choosing best qp_quantized gain value --- */
			aux = en / (2.*w.wk.qp_abs_anl_syn_N*32768.*32768.);  /* normalized power - 3dB */
			min = 1.e200;
			for (n=0; n<pow2[w.wk.nbgn_]; n++) {
				if (fabs(gnqtbl[n]*gnqtbl[n]/3. - aux) < min) {
					min = fabs(gnqtbl[n]*gnqtbl[n]/3. - aux); 
					indicesa[0] = n;
				}
			}
			/* --- construct resulting noise excitation vector --- */
			for (n=0; n<w.wk.qp_abs_anl_syn_N; n++)	{
				w.wk.qp_abs_anl_syn_e[n] = gnqtbl[indicesa[0]] * (double)qp_noisegen(&w.wk.seed_);
			}
		}  

		/* --- synthesis filtering --- */
		qp_latfilr(w.wk.qp_abs_anl_syn_e, w.wk.qp_abs_anl_syn_N, w.wk.qp_abs_anl_syn_N, w.wk.np_, k, w.wk.qp_abs_anl_syn_mema_sf, shat+i*w.wk.qp_abs_anl_syn_N);
	}

	/* --- save VA excitation vector --- */
	if (va) for (n=0; n<w.wk.qp_abs_anl_syn_N; n++) {
		w.wk.qp_abs_anl_syn_esave[n] = w.wk.qp_abs_anl_syn_e[n];
	}

	free(k);
	free(a);
	free(lpci);
	free(kltp);
	free(hltp);
	free(kmpe);
	free(hmpe);
	free(hhmpe);
	free(r);
	free(indicesa);
}

}
