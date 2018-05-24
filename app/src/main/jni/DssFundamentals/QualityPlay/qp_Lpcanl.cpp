/***ABS******************************************************************
 *                               lpcanl.c
 * -------------------------------------------------------------------- *
 *
 *   qp_lpc_analysis  -  performs autocorrelation method LPC-analysis
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:     void qp_lpc_analysis_auto(double *s,          i
 *                                          int frmlen,         i
 *                                          int np,             i
 *                                          double *k,          o
 *                                          double *a,          o
 *                                          short *lpci);       o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The acf of the windowed input signal is the basis for
 *      obtaining the reflection coefficients
 *
 *              k     ,            i = 0, np
 *               i
 *
 *      from a Schur recursion. From the reflection coefficients the
 *      LPC coefficients
 *
 *              a     ,            i = 0, np             with
 *               i
 *                           a   =  1
 *                            0
 *
 *      are computed by the well-known stepup procedure.
 *      Subsequently a bandwidth expansion is applied and the
 *      corresponding final reflection coefficients are obtained
 *      from a stepdown procedure.
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           window_trapezoid
 *                              acf
 *                              schur
 *                              stepup
 *                              qp_stepdown
 *                              qp_rc_quantization
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    bwe_
 *                              fs_
 *                              lpcquant_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Oct-15-1990
 *
 *    modification date:        Jul-28-1995
 *                              Jun-11-2003 by Michael Kroener
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Lpcanl.c  $
 * Revision 1.14 2005/06/13 11:40:17CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

#define PI             3.141592653589793

namespace QualityPlay {

void qp_lpc_analysis(dsswork& w, double *s, int frmlen, int np, double *k, double *a, short *lpci)
{
	double	*swlpc;
	double	*acfs;
	double	*kq;
	int		i;
	double	beta, fac;

	swlpc = (double *)calloc(frmlen, sizeof(double));
	acfs  = (double *)calloc(np+1,   sizeof(double));
	kq    = (double *)calloc(np,     sizeof(double));

	if (swlpc == NULL || acfs == NULL || kq == NULL) {
		fprintf(ERROROUT, "lpc_analysis_auto: memory allocation error!\n");
		exit(1);
	}

	/* --- windowing --- changed by Michael Kroener 2003.06.24 */
	window_trapezoid(s, swlpc, frmlen, np);

	/* --- autocorrelation computation --- */
	qp_acf(swlpc, frmlen, np+1, acfs);

	/* --- recursion --- */
	qp_schur(w, acfs, np, k);

	/* --- compute LPC coefficients from reflection coefficients --- */
	qp_stepup(k, np, a);

/* --- bandwidth expansion --- */
	if (w.wk.bwe_ > 0.) {
		fac = beta = exp(-PI*w.wk.bwe_/w.wk.fs_/1000.);
		for (i=1; i<=np; i++) {
			a[i] *= fac; 
			fac *= beta;
		}

		/* compute final reflection coefficients */
		qp_stepdown(a, np, k);
	}

	/* --- LPC quantization --- */
	if (w.wk.lpcquant_) {
		qp_rc_quantization(k, w.wk.np_, w.wk.nblpc_, lpci, kq);
		for (i=0; i<w.wk.np_; i++) {
			k[i] = kq[i];
		}
		qp_stepup(k, np, a);
	}

	free(swlpc);
	free(acfs);
	free(kq);
}
}