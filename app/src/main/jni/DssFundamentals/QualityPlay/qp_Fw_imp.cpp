/***ABS******************************************************************
 *                                qp_fw_imp.c
 * -------------------------------------------------------------------- *
 *
 *   qp_fw_imp  -  computation of "weighted" reflection coefficients and
 *               truncated impulse response for analysis-by-synthesis.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:          void qp_fw_imp(double *a,                i
 *                                    int np,                   i
 *                                    double alpha,             i
 *                                    double *kw,               o
 *                                    int lh,                   i
 *                                    double *h);               o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine is needed in the preprocessing stage of analysis-
 *      by-synthesis coders. It computes (on the basis of the LPC
 *      coefficients 'a') the reflection coefficients for the
 *      recursive part of the weighting filter (order 'np'), 'kw',
 *      using the weighting factor 'alpha' (0 <= alpha <= 1).
 *      Moreover, the truncated impulse response of the synthesis/
 *      weighting filter cascade, h, is computed at a length of
 *      'lh' samples.
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           qp_stepdown
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Mar-31-1992
 *
 *    modification date:        Jul-27-1995
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: qp_Fw_imp.c  $
 * Revision 1.10 2005/06/13 11:40:19CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

#define MIN(a, b)    ((a) < (b) ? (a) : (b))

namespace QualityPlay {

void qp_fw_imp(double *a, int np, double alpha, double *kw, int lh, double *h)
{
	int      i, j;
	double   *aw;
	double   fac;
	double   accu;

	/* --- allocate memory --- */
	if ((aw=(double *)calloc(np+1, sizeof(double))) == NULL) {
		fprintf(ERROROUT, "\nfw_imp: memory allocation error!\n\n");
		exit(1);
	}

	/* --- compute coefficients 'kw' --- */
	aw[0] = a[0];
	fac = alpha;

	for (i=1; i<=np; i++) {
		aw[i] = a[i] * fac;
		fac *= alpha;
	}

	qp_stepdown(aw, np, kw);

   /* --- compute impulse response 'h' --- */
	#define ECO
	#ifdef ECO
	{
		h[0] = aw[0];
		for (i=1; i<lh; h[i++]=accu)
			for (accu=0, j=1; j<=MIN(np,i); j++)
				accu -= h[i-j] * aw[j];
	}
	#else
	{
		int     n;
		double  *en, ep;

		if ((en=(double *)calloc(np, sizeof(double))) == NULL) {
			fprintf(ERROROUT, "\nfw_imp: memory allocation error!\n\n");
			exit(1);
		}

		h[0] = en[0] = 1.;
		for (j=1; j<np; j++) 
			en[j] = kw[j-1] * h[0];

		for (i=1; i<lh; h[i++]=en[0]=ep) {
			ep = kw[np-1] * en[np-1];

			for (j=np-2; j>=0; j--) {
				ep -= kw[j] * en[j];
				en[j+1] = en[j] + kw[j] * ep;
			}
		}
		free(en);
	}
	#endif

	/* --- free memory --- */
	free(aw);
}
}