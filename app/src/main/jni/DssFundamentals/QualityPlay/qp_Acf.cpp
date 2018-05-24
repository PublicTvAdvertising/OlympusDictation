/***ABS******************************************************************
 *                                 acf.c
 * -------------------------------------------------------------------- *
 *
 *   acf  -  computes autocorrelation.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void acf(double *x,                      i
 *                               int xlen,                       i
 *                               int nlags,                      i
 *                               double *acfx);                  o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The ouput is computed according to
 *
 *                             xlen-n
 *              acfx(n)   =     SUM    (x(i) * x(i+n)) ,
 *                              i=0
 *
 *                                             n = 0,...,nlags-1   .
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            May-19-1994
 *
 *    modification date:        Jul-27-1995
 *                              Jun-02-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Acf.c  $
 * Revision 1.10 2005/06/13 11:39:51CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_acf(double *x, int xlen, int nlags, double *acfx)
{
	int		i, n;
	double	*p1, *p2, *p3;

	p3 = acfx;
	for (n=0; n<nlags; n++)	{
		p1 = x;
		p2 = x + n;
		*p3 = 0;
		for (i=n; i<xlen; i++) { 
			*p3 += *p1++ * *p2++; 
		}
		p3++;
	}
}

}