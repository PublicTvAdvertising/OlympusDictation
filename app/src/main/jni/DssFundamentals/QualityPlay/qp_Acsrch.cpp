/***ABS******************************************************************
 *                               acsrch.c
 * -------------------------------------------------------------------- *
 *
 *   adaptive_codebook_search  -  adaptive excitation evaluation
 *                                     in analysis-by-synthesis loop.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:    void adaptive_codebook_search(double *t,       i
 *                                                double *h,       i
 *                                                double *e,       io
 *                                                int *M,          o
 *                                                short *indexb);  o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine performs analysis-by-synthesis pitch predictor
 *      parameter evaluation (autocorrelation method).
 *
 *      The expression to be minimized is
 *
 *                        (c'*H'*t)        H = impulse response matrix
 *         E = z'*z - ----------------     c = candidate codeword
 *                     sqrt(c'*H'*H*c)     t = target vector          .
 *
 *      In order to save computational complexity, some of the
 *      computations are carried out in a recursive manner.
 *
 *      The minimum pitch lag 'minlag_' must not be less than the
 *      number of samples corresponding to half the LTP vector length
 *      'excveclen_'.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    maxlag_
 *                              minlag_
 *                              difflagrange_
 *                              lha_
 *                              excveclen_
 *                              fs_
 *                              betapos_
 *                              LTPstab_
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           qp_quantize
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Oct-20-1994
 *
 *    modification date:        Nov-30-1995
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Acsrch.c  $
 * Revision 1.16 2005/06/13 11:39:52CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

#define MIN(a,b) ((a)<(b) ? (a):(b))
#define MAX(a,b) ((a)>(b) ? (a):(b))

namespace QualityPlay {

void qp_adaptive_codebook_search(dsswork& w, double *t, double *h, double *e, int *M, short *indexb)
{
#ifdef LTP_5_Bit
	/* 5 bit resolution */
	static const double    QT_LTP[32] =
	#include "QT_LTP.h"
#else
	/* 6 bit resolution */
	static const double    QT_LTP[64] =
	#include "QT_LTP6.h"
#endif

	double			beta;
	register int	i, n;
	int				N, R, lag;
	double			*p1, *p2;
	double			*hcbm, *hcbmprev, *hcwm;
	double			cross, energy;
	double			*acb;
	double			maxc, maxe;
	double			accu, accu2;
	double			enmem;
	int				startlag, stoplag;
	int				firstpass;

	if (!w.wk.qp_adaptive_codebook_search_init) {
		if ((w.wk.qp_adaptive_codebook_search_acdbk=(double *)calloc(w.wk.maxlag_, sizeof(double))) == NULL) {
			fprintf(ERROROUT,"\nadaptive_codebook_search init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_adaptive_codebook_search_init = 1;
	}
	if (0 == t && 0 == h && 0 == e && 0 == M && 0 == indexb) {
		free(w.wk.qp_adaptive_codebook_search_acdbk);		w.wk.qp_adaptive_codebook_search_acdbk = 0;
		w.wk.qp_adaptive_codebook_search_init = 0;
		return;
	}

	N = (short)(w.wk.excveclen_*w.wk.fs_);
	R = (short)(w.wk.lha_*w.wk.fs_);
	hcbm     = (double *)calloc(N, sizeof(double));
	hcbmprev = (double *)calloc(N, sizeof(double));
	hcwm     = (double *)calloc(N, sizeof(double));

	if (hcbm==NULL || hcbmprev==NULL || hcwm==NULL) {
		fprintf(ERROROUT, "\nadaptive_codebook_search: memory allocation error!\n\n");
		exit(1);
	}

	/* --- preset range of lags to be tested --- */
	if (*M > 0) {
		if (*M <= w.wk.maxlag_-w.wk.difflagrange_/2) {
			startlag = MAX(w.wk.minlag_, *M-w.wk.difflagrange_/2+1);
			stoplag  = MIN(w.wk.maxlag_, startlag+w.wk.difflagrange_-1);
		}
		else {
			stoplag  = w.wk.maxlag_;
			startlag = MAX(w.wk.minlag_, stoplag-w.wk.difflagrange_+1);
		}
	}
	else {
		startlag = w.wk.minlag_;
		stoplag  = w.wk.maxlag_;
	}

	/* --- update adaptive codebook --- */
	p1 = w.wk.qp_adaptive_codebook_search_acdbk; 
	p2 = w.wk.qp_adaptive_codebook_search_acdbk + N;
	for (i=0; i<w.wk.maxlag_-N; i++) 
		*p1++ = *p2++;
	for (i=0; i<N; i++) 
		*p1++ = e[i];

	/* --- set pointer to relevant section of adaptive codebook */
	acb = w.wk.qp_adaptive_codebook_search_acdbk + w.wk.maxlag_-stoplag;

	/* --- put maximum absolute adaptive codebook section amplitude in accu --- */
	for (accu=0, n=0; n<MIN(stoplag-startlag+N-1,stoplag); n++)
		accu = MAX(fabs(acb[n]), accu);

	/* --- computation of cross and energy values for all the lags
	thereby detecting best lag --- */
	maxc = 0;
	maxe = 1e20;
	*M = startlag;
	beta = 0;

	if (accu) {

		/* computations for lag = startlag */
		lag = startlag;
		for (n=0; n<N; hcbm[n++]=accu) {
			for (accu=0, i=MAX(0,n-lag+1); i<=MIN(n,R-1); i++) 
				accu += acb[stoplag-lag+n-i] * h[i];
		}
		for (n=0; n<MIN(lag,N); n++) 
			hcwm[n] = hcbm[n];
		for (; n<N; n++) 
			hcwm[n] = hcbm[n] + hcbm[n-lag];

		for (accu=0, n=0; n<N; n++) {
			accu += hcwm[n] * t[n];
			hcbmprev[n] = hcbm[n];
		}

		if ((cross=accu) > 0) {
			for (accu=0, n=0; n<N; n++) 
				accu += hcwm[n] * hcwm[n];
			energy = accu;
			accu = maxe * cross;
			accu *= cross;
			accu2 = maxc * maxc;
			accu2 *= energy;
			if (accu > accu2) {
				maxc = cross; 
				maxe = energy; 
				*M = lag;
			}
		}

		/* computations for startlag < lag < N */
		for (lag=startlag+1; lag<N; lag++) {

			hcbm[0] = acb[stoplag-lag] * h[0];
			for (n=1; n<R; n++) 
				hcbm[n] = hcbmprev[n-1] + acb[stoplag-lag] * h[n];
			for (n=R; n<N; n++) 
				hcbm[n] = hcbmprev[n-1];
			for (n=0; n<lag; n++) 
				hcwm[n] = hcbm[n];
			for (; n<N; n++) 
				hcwm[n] = hcbm[n] + hcbm[n-lag];
			for (accu=0, n=0; n<N; n++) {
				accu += hcwm[n] * t[n];
				hcbmprev[n] = hcbm[n];
			}

			if ((cross=accu) > 0) {
				for (accu=0, n=0; n<N; n++) 
					accu += hcwm[n] * hcwm[n];
				energy = accu;
				accu = maxe * cross;
				accu *= cross;
				accu2 = maxc * maxc;
				accu2 *= energy;
				if (accu > accu2) {
					maxc = cross; 
					maxe = energy; 
					*M = lag;
				}
			}
		}

		/* computations for max(startlag+1,N) <= lag <= stoplag */
		for (firstpass=1; lag<=stoplag; lag++) {

			hcbm[0] = acb[stoplag-lag] * h[0];
			for (n=1; n<R; n++) 
				hcbm[n] = hcbmprev[n-1] + acb[stoplag-lag] * h[n];
			for (n=R; n<N; n++) 
				hcbm[n] = hcbmprev[n-1];
			for (n=0; n<N; n++) 
				hcwm[n] = hcbm[n];

			if (firstpass) {
				for (enmem=0, n=R; n<N; n++) 
					enmem += hcwm[n] * hcwm[n];
				firstpass = 0;
			}
			else {
				enmem -= hcbmprev[N-1] * hcbmprev[N-1];
				enmem += hcbmprev[R-1] * hcbmprev[R-1];
			}

			for (accu=0, n=0; n<N; n++) 
				accu += hcwm[n] * t[n];

			if ((cross=accu) > 0) {
				for (accu=enmem, n=0; n<R; n++) 
					accu += hcwm[n] * hcwm[n];
				energy = accu;
				accu = maxe * cross;
				accu *= cross;
				accu2 = maxc * maxc;
				accu2 *= energy;
				if (accu > accu2) {
					maxc = cross; 
					maxe = energy; 
					*M = lag;
				}
			}

			for (n=0; n<N; n++) 
				hcbmprev[n] = hcbm[n];
		}

	}

	/* --- compute gain factor for adaptive excitation --- */
	if (maxe)
		beta = maxc / maxe;
	else
		beta = 0;

	/* --- compute adaptive excitation --- */
	if (w.wk.ltpquant_) {
		beta = qp_quantize(beta, QT_LTP, w.wk.nbga_, indexb);
	}

	for (n=1, i=0; i<N; n++) {
		for (; i<n*(*M) && i<N; i++) {
			e[i] = beta * w.wk.qp_adaptive_codebook_search_acdbk[w.wk.maxlag_-n*(*M)+i];
		}
	}

	free(hcbm);
	free(hcbmprev);
	free(hcwm);
}


}