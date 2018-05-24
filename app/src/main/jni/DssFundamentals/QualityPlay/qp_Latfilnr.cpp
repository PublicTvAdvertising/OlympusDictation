/***ABS******************************************************************
 *                                latfilnr.c
 * -------------------------------------------------------------------- *
 *
 *   qp_latfilnr  -  performs non-recursive lattice filtering.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void qp_latfilnr(double *x,                 i
 *                                    int B,                     i
 *                                    int M,                     i
 *                                    int N,                     i
 *                                    double *k,                 i
 *                                    double *mem,               io
 *                                    double *y);                o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      In this function, for each sample the recursive equations
 *
 *       xp(i+1) = xp(i) + k(i) * xn(i) ,       0 <= i <= N,
 *
 *      with
 *
 *       xp(0) = x(n),       y(n) = xp(N)
 *
 *      are evaluated using the filter memory
 *
 *       xn(i) ,                                0 <= i <= N-1,
 *
 *      which is updated according to
 *
 *       xn(i) = xn(i-1) + k(i-1) * xp(i-1),      1 <= i <= N-1,
 *
 *      with
 *
 *       xn(0) = x(n).
 *
 *      The filter memory has to be kept elsewhere and pointers to
 *      the filter state value arrays have to be passed to this routine
 *      (size of the array: at least N!).
 *
 *      Another parameter M has to be submitted to indicate the number
 *      of samples of the input signal block after which the filter
 *      state values should be saved (for overlapping input blocks!).
 *      In the case of no overlap of succesive input blocks, this
 *      parameter has to be chosen equal to the block size B.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Sep-12-1994
 *
 *    modification date:        Jan-11-1995
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Latfilnr.c  $
 * Revision 1.9 2005/06/13 11:40:20CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_latfilnr(double *x, int B, int M, int N, double *k, double *mem, double *y)
{
	int     i, n;
	double  newmem, aux;
	double  xp, *xn;

	if ((xn=(double *)calloc(N, sizeof(double))) == NULL) {
		fprintf(ERROROUT, "\nlatfilnr: memory allocation error!\n\n");
		exit(1);
	}

	for (i=0; i<N; i++) {
		xn[i] = mem[i];
	}

	for (n=0; n<B; n++) {
		xp = newmem = x[n];
		for (i=0; i<N; i++)	{
			aux = xn[i] + k[i] * xp;
			xp += k[i] * xn[i];
			xn[i] = newmem;
			newmem = aux;
		}
		y[n] = xp;

		if (n == M-1) 
			for (i=0; i<N; i++) 
				mem[i] = xn[i];
	}

	free(xn);
}
}