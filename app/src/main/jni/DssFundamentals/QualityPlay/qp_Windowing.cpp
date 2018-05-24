/***ABS******************************************************************
 *                               Windowing.c
 * -------------------------------------------------------------------- *
 *
 *   different windowing methods
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:     void window_trapezoid(double *inp,         i
 *                                         double *outp,        o
 *                                         int frmlen,          i
 *                                         int np);             i
 *
 *                   void window_hamming(double *inp,           i
 *                                       double *outp,          o
 *                                       int frmlen);           i
 *
 *                   void window_hanning(double *inp,           i
 *                                       double *outp,          o
 *                                       int frmlen)            i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Michael Kroener, Grundig Business Systems
 *
 *    creation date:            May-28-2003
 *
 *    modification date:        Jun-11-2003
 *
 ************************************************************************
 * $Log: Windowing.c  $
 * Revision 1.9 2005/06/13 11:41:08CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void window_trapezoid(double *inp, double *outp, int frmlen, int np)
{
	int i;
	for (i=0; i<np; i++)     outp[i] = inp[i] * (double)(i+1.)/np;
	for (; i<frmlen-np; i++) outp[i] = inp[i];
	for (; i<frmlen; i++)    outp[i] = inp[i] * (double)(frmlen-i)/np;
}


#if 0

	#define PI	3.141592653589793

	void window_hamming(double *inp, double *outp, int frmlen)
	{
		// Hamming  wm[n] = 0.54 - 0.46 cos(2pn/(N-1)) 
		int i;
		for (i=0; i<frmlen; i++) outp[i] = inp[i] * (0.54 -  0.46 * cos((double)((2 * PI * i) / (frmlen - 1)))); 
	}


	void window_hanning(double *inp, double *outp, int frmlen)
	{
		// Hanning  wn[n] = 0.5 - 0.5 cos(2pn/(N-1))
		int i;
		for (i=0; i<frmlen; i++) outp[i] = inp[i] * (0.5 -  0.5 * cos((double)((2 * PI * i) / (frmlen - 1)))); 
	}

#endif




}