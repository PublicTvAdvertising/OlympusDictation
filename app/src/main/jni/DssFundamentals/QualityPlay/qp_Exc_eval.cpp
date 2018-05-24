/***ABS******************************************************************
 *                               exc_eval.c
 * -------------------------------------------------------------------- *
 *
 *   qp_excitation_evaluation  -  determines excitation vector.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype: void qp_excitation_evaluation(double *r,               i
 *                                          double *k,               i
 *                                          double *hmpe,            i
 *                                          double *hhmpe,           i
 *                                          double *e,               io
 *                                          unsigned long *indexe,   o
 *                                          short *indicesa);        o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine determines a multi-pulse excitation vector taking
 *      into account the filter memory and LTP-filter contribution. The
 *      vector returned in 'e' is the compound excitation consisting of
 *      both the adaptive excitation and the innovation. The pulse
 *      positions encoding result is returned in '*indexe'. The pulse
 *      amplitudes quantization indices are returned in '*indicesa'.
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           latfiltnr
 *                              latfiltr
 *                              qp_mp_excitation_evaluation
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    np_
 *                              excveclen_
 *                              fs_
 *                              alpham_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Apr-6-1992
 *
 *    modification date:        Jul-28-1995
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Exc_eval.c  $
 * Revision 1.9 2005/06/13 11:39:52CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_excitation_evaluation(dsswork& w, double *r, double *kmpe, double *hmpe, double *hhmpe, double *e, __int64_t *indexe, short *indicesa)
{
	int				i;
	double			*empe;
	double			*x, *y, *t;
	double			*dummy;


	/* --- initialize --- */
	if (!w.wk.qp_excitation_evaluation_init) {
		w.wk.qp_excitation_evaluation_mema_wf  = (double *)calloc(w.wk.np_, sizeof(double));
		w.wk.qp_excitation_evaluation_mema_swf = (double *)calloc(w.wk.np_, sizeof(double));
		if (w.wk.qp_excitation_evaluation_mema_wf==NULL || w.wk.qp_excitation_evaluation_mema_swf==NULL) {
			fprintf(ERROROUT, "\nexcitation_evaluation init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_excitation_evaluation_N = (short)(w.wk.excveclen_*w.wk.fs_);
		w.wk.qp_excitation_evaluation_init = 1;
	}
	if (0 == r && 0 == kmpe && 0 == hmpe && 0 == hhmpe && 0 == e && 0 == indexe && 0 == indicesa) {
		free(w.wk.qp_excitation_evaluation_mema_wf);		w.wk.qp_excitation_evaluation_mema_wf = 0;
		free(w.wk.qp_excitation_evaluation_mema_swf);		w.wk.qp_excitation_evaluation_mema_swf = 0;
		w.wk.qp_excitation_evaluation_init = 0;
		return;
	}

	/* --- allocate memory --- */
	empe  = (double *)calloc(w.wk.qp_excitation_evaluation_N, sizeof(double));
	x     = (double *)calloc(w.wk.qp_excitation_evaluation_N, sizeof(double));
	y     = (double *)calloc(w.wk.qp_excitation_evaluation_N, sizeof(double));
	t     = (double *)calloc(w.wk.qp_excitation_evaluation_N, sizeof(double));
	dummy = (double *)calloc(w.wk.qp_excitation_evaluation_N, sizeof(double));
	if (empe==NULL || x==NULL || y==NULL || t==NULL || dummy==NULL) {
		fprintf(ERROROUT, "\nexcitation_evaluation: memory allocation error!\n\n");
		exit(1);
	}

	/* --- if first speech frame following a silence period,
	reset filter memories --- */
	if (w.wk.reset_memories_) {
		for (i=0; i<w.wk.np_; i++) 
			w.wk.qp_excitation_evaluation_mema_wf[i] = w.wk.qp_excitation_evaluation_mema_swf[i] = 0;
		w.wk.reset_memories_ = 0;
	}

	/* --- weight input signal --- */
	qp_latfilr(r, w.wk.qp_excitation_evaluation_N, w.wk.qp_excitation_evaluation_N, w.wk.np_, kmpe, w.wk.qp_excitation_evaluation_mema_wf, x);

	/* --- compute ringing vector and contribution of LTP excitation --- */
	qp_latfilr(e, w.wk.qp_excitation_evaluation_N, 0, w.wk.np_, kmpe, w.wk.qp_excitation_evaluation_mema_swf, y);

	/* --- compute target vector --- */
	for (i=0; i<w.wk.qp_excitation_evaluation_N; i++) 
		t[i] = x[i] - y[i];

	/* --- determine innovation --- */
	qp_mp_excitation_evaluation(w, t, hmpe, hhmpe, empe, indexe, indicesa);

	/* --- compute excitation signal --- */
	for (i=0; i<w.wk.qp_excitation_evaluation_N; i++) 
		e[i] += empe[i];

	/* --- update memory of synthesis/weighting filter cascade --- */
	qp_latfilr(e, w.wk.qp_excitation_evaluation_N, w.wk.qp_excitation_evaluation_N, w.wk.np_, kmpe, w.wk.qp_excitation_evaluation_mema_swf, dummy);

	free(empe);
	free(x);
	free(y);
	free(t);
	free(dummy);
}


}