/***ABS******************************************************************
 *                                exc_gen.c
 * -------------------------------------------------------------------- *
 *
 *   excitation_generation  -  generates excitation vector.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:   void excitation_generation(int M,               i
 *                                            short betai,         i
 *                                            unsigned long ei,    i
 *                                            short *ai,           i
 *                                            double *e);          o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine generates the excitation vector using the 
 *      excitation parameters LTP lag, encoded LTP gain, MPE position
 *      code, and encoded MPE amplitudes (block maximum and individual
 *      relative amplitudes).
 *      The excitation vector returned in 'e' is the compound
 *      excitation consisting of both the adaptive excitation and the
 *      (MP) innovation.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    maxlag_
 *                              excveclen_
 *                              fs_
 *                              nbga_
 *                              nbgm_
 *                              nbp_
 *                              nimps_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Feb-29-1996
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Exc_gen.c  $
 * Revision 1.9 2005/06/13 11:39:53CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995, 1996 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#include "qp_pow2.h"

namespace QualityPlay {

void qp_excitation_generation(dsswork& w, int M, short betai, __int64_t ei, short *ai, double *e)
{
#ifdef LTP_5_Bit	
	static double	QT_LTP[32]=
	#include "QT_LTP.h"
#else
	/* 6 bit resolution */
	static const double    QT_LTP[64] =
	#include "QT_LTP6.h"
#endif

	static const double	QT_MPEB[64] =
	#include "QT_MPEB.h"
	static const double	QT_MPEA[8] =
	#include "QT_MPEA.h"

	double			beta;
	int				*pos;
	double			max;
	double			*amp;
	int				i, j, k;
	__int64_t			index;
	__int64_t			*y;


	/* --- initialize --- */
	if (0 == M && 0 == betai && 0 == ei && 0 == ai && 0 == e) {
		free(w.wk.qp_excitation_generation_excdel);
		w.wk.qp_excitation_generation_excdel = 0;
		w.wk.qp_excitation_generation_ed = 0;
		w.wk.qp_excitation_generation_init = 0;
		return;
	}
	if (!w.wk.qp_excitation_generation_init) {
		w.wk.qp_excitation_generation_excdel = (double *)calloc(w.wk.maxlag_+1, sizeof(double));
		if (w.wk.qp_excitation_generation_excdel==NULL) {
			//fprintf(ERROROUT, "\nexcitation_generation init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_excitation_generation_ed = w.wk.qp_excitation_generation_excdel + w.wk.maxlag_ + 1;
		w.wk.qp_excitation_generation_N = (int)(w.wk.excveclen_*w.wk.fs_);
		w.wk.qp_excitation_generation_init = 1;
	}

	/* --- allocate memory --- */
	pos = (int *)calloc(w.wk.nimps_,     sizeof(int    ));
	amp = (double *)calloc(w.wk.nimps_,  sizeof(double ));
	y   = (__int64_t *)calloc(w.wk.nimps_, sizeof(__int64_t));

	if (pos==NULL || amp==NULL || y==NULL) {
		//fprintf(ERROROUT, "\nexcitation_generation: memory allocation error!\n\n");
		exit(1);
	}

	/* --- compute adaptive excitation --- */
	beta = QT_LTP[betai];
	for (k=1, i=0; i<w.wk.qp_excitation_generation_N; k++) {
		for (; i<k*M && i<w.wk.qp_excitation_generation_N; i++) {
			e[i] = beta * w.wk.qp_excitation_generation_ed[i-k*M];
		}
	}

	/* --- add innovation --- */
	for (y[0] = (__int64_t)w.wk.qp_excitation_generation_N, i=1; i<w.wk.nimps_; i++) {
		y[i] = (__int64_t)( (double)y[i-1] * (w.wk.qp_excitation_generation_N-i) / (i+1) );
	}

	/*
	y[0] = N;                  //  y[j]  =  72 choose (j+1)  
	y[1] = (y[0]*(N-1))/2;
	y[2] = (y[1]*(N-2))/3;
	y[3] = (y[2]*(N-3))/4;
	y[4] = (y[3]*(N-4))/5;
	y[5] = (y[4]*(N-5))/6;
	y[6] = (unsigned long)((double)y[5]*(N-6)/7);
	y[7] = (unsigned long)((double)y[6]*(N-7)/8);
	*/

	index = ei;
	if (index >= y[w.wk.nimps_-1]) 
		index = y[w.wk.nimps_-1] - 1; // make sure there is no invalid poscode


	for (j=w.wk.nimps_-1; j>=0; j--) {
		while (index < y[j]) {
			for (y[0]--, k=1; k <= j; k++) {
				y[k] -= y[k-1];
			}
		}
		pos[w.wk.nimps_-1-j] = (int)y[0];
		index -= y[j];
	}

	max = QT_MPEB[ai[0]];
	for (j=0; j<w.wk.nimps_; j++) 
		amp[j] = max * QT_MPEA[ai[j+1]];
	for (j=0; j<w.wk.nimps_; j++) 
		e[pos[j]] += amp[j];

	/* --- update excitation delay line --- */
	for (i=0; i<=w.wk.maxlag_-w.wk.qp_excitation_generation_N; i++) 
		w.wk.qp_excitation_generation_excdel[i] = w.wk.qp_excitation_generation_excdel[i+w.wk.qp_excitation_generation_N];
	for (i=0; i<w.wk.qp_excitation_generation_N; i++) 
		w.wk.qp_excitation_generation_excdel[i+w.wk.maxlag_+1-w.wk.qp_excitation_generation_N] = e[i];

	/* --- free memory --- */
	free(pos); pos = 0;
	free(amp); amp = 0;
	free(y); y = 0;
}
}