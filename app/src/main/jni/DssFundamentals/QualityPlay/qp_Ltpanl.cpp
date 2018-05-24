/***ABS******************************************************************
 *                               ltpanl.c
 * -------------------------------------------------------------------- *
 *
 *   qp_LTP_analysis  -  evaluates pitch predictor parameters.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void qp_LTP_analysis(double *r,             i
 *                                        double *kltp,          i
 *                                        double *hltp,          i
 *                                        double *e,             io
 *                                        int *M,                o
 *                                        short *indexb);        o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine performs a closed-loop analysis (autocorrelation
 *      method) in order to determine the pitch predictor parameters:
 *      lag 'M' and (encoded) gain 'indexb'.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    np_
 *                              fs_
 *                              excveclen_
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           adaptive_codebook_search
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Apr-06-1992
 *
 *    modification date:        Jun-13-1997
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Ltpanl.c  $
 * Revision 1.11 2005/06/13 11:40:15CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_LTP_analysis(dsswork& w, double *r, double *kltp, double *hltp, double *e, int *M, short *indexb)
{

	int				i;
	double			*x, *y, *t;
	double			*zero;
	double			*dummy;

	/* --- initialize --- */
	if (!w.wk.qp_LTP_analysis_init) {
		w.wk.qp_LTP_analysis_mema_wf   = (double *)calloc(w.wk.np_, sizeof(double));
		w.wk.qp_LTP_analysis_mema_swf  = (double *)calloc(w.wk.np_, sizeof(double));
		w.wk.qp_LTP_analysis_kltp_prev = (double *)calloc(w.wk.np_, sizeof(double));
		if (w.wk.qp_LTP_analysis_mema_wf==NULL || w.wk.qp_LTP_analysis_mema_swf==NULL || w.wk.qp_LTP_analysis_kltp_prev==NULL)	{
			fprintf(ERROROUT, "\nLTP_analysis init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_LTP_analysis_N = (short)(w.wk.excveclen_*w.wk.fs_);
		w.wk.qp_LTP_analysis_init = 1;
	}
	if (0 == r && 0 == kltp && 0 == hltp && 0 == e && 0 == M && 0 == indexb) {
		free(w.wk.qp_LTP_analysis_mema_wf);	w.wk.qp_LTP_analysis_mema_wf = 0;
		free(w.wk.qp_LTP_analysis_mema_swf);	w.wk.qp_LTP_analysis_mema_swf = 0;
		free(w.wk.qp_LTP_analysis_kltp_prev);	w.wk.qp_LTP_analysis_kltp_prev = 0;
		w.wk.qp_LTP_analysis_init = 0;
		return;
	}

	/* --- allocate memory --- */
	x     = (double *)calloc(w.wk.qp_LTP_analysis_N, sizeof(double));
	y     = (double *)calloc(w.wk.qp_LTP_analysis_N, sizeof(double));
	t     = (double *)calloc(w.wk.qp_LTP_analysis_N, sizeof(double));
	zero  = (double *)calloc(w.wk.qp_LTP_analysis_N, sizeof(double));
	dummy = (double *)calloc(w.wk.qp_LTP_analysis_N, sizeof(double));
	if (x==NULL || y==NULL || t==NULL || zero==NULL || dummy==NULL) {
		fprintf(ERROROUT, "\nLTP_analysis: memory allocation error!\n\n");
		exit(1);
	}

	/* --- if first speech frame following a silence period,
	reset filter memories --- */
	if (w.wk.reset_memories_)
		for (i=0; i<w.wk.np_; i++)
			w.wk.qp_LTP_analysis_mema_wf[i] = w.wk.qp_LTP_analysis_mema_swf[i] = 0;

	/* --- weight input signal --- */
	qp_latfilr(r, w.wk.qp_LTP_analysis_N, w.wk.qp_LTP_analysis_N, w.wk.np_, kltp, w.wk.qp_LTP_analysis_mema_wf, x);

	/* --- update memory of synthesis/weighting filter cascade --- */
	qp_latfilr(e, w.wk.qp_LTP_analysis_N, w.wk.qp_LTP_analysis_N, w.wk.np_, w.wk.qp_LTP_analysis_kltp_prev, w.wk.qp_LTP_analysis_mema_swf, dummy);

	/* --- compute ringing vector --- */
	qp_latfilr(zero, w.wk.qp_LTP_analysis_N, 0, w.wk.np_, kltp, w.wk.qp_LTP_analysis_mema_swf, y);

	/* --- compute target vector --- */
	for (i=0; i<w.wk.qp_LTP_analysis_N; i++) t[i] = x[i] - y[i];

	/* --- perform adaptive codebook search --- */
	qp_adaptive_codebook_search(w, t, hltp, e, M, indexb);

	/* --- keep LPC parameters for next pass --- */
	for (i=0; i<w.wk.np_; i++) {
		w.wk.qp_LTP_analysis_kltp_prev[i] = kltp[i];
	}

	free(x);
	free(y);
	free(t);
	free(zero);
	free(dummy);
}

}