/***ABS******************************************************************
 *                                 schur.c
 * -------------------------------------------------------------------- *
 *
 *   qp_schur  -  performs Schur recursion (LPC analysis)
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         void qp_schur(double *acfs,               i
 *                                  int np,                     i
 *                                  double *k);                 o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This realization of the Schur recursion uses an optional
 *      stabilization which consists of increasing the main diagonal
 *      elements of the autocorrelation matrix by multiplying them
 *      by a factor of 1+stab_.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Apr-28-1995
 *
 *    modification date:        Apr-28-1995
 *
 ************************************************************************
 * $Log: Schur.c  $
 * Revision 1.5 2005/06/13 11:40:51CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_schur(dsswork& w, double *acfs, int np, double *k)
{
	int     i, m, n;
	double  stabfac;
	double  kk[21];
	double  pp[21];

	/* --- if input signal has zero energy set coefficients to zero */
	if (acfs[0] == 0) {
		for (i=0; i<np; i++) 
			k[i] = 0;
	}
	else {
		/* --- stabilization --- */
		stabfac = 1./(1.+w.wk.stab_);
		for (i=1; i<=w.wk.np_; i++)	
			acfs[i] *= stabfac;

		/* --- Schur recursion --- */
		/* initialise arrays pp[..] and kk[..] for the recursion */
		for (i=1; i<np; i++)	
			kk[np+1-i] = acfs[i];
		for (i=0; i<=np; i++)	
			pp[i] = acfs[i];

		/* compute reflection coefficients */
		for (n=0; n<np; n++) {

			if (pp[0] <= fabs(pp[1])) {
				for(i=n; i<np; i++) 
					k[i] = 0;
				return;
			}

			k[n] = -pp[1] / pp[0];
			if (n==np-1) 
				return;

			/* recursion */
			pp[0] += pp[1] * k[n];

			for (m=1; m<np-n; m++) {
				pp[m] = pp[m+1] + kk[np+1-m] * k[n];
				kk[np+1-m] += pp[m+1] * k[n];
			}
		}
	}
}
}