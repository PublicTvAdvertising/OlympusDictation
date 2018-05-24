/***ABS******************************************************************
 *                                rcqunt.c
 * -------------------------------------------------------------------- *
 *
 *   qp_rc_quantization  -  performs LPC parameter quantization by
 *         quasi-optimal scalar quanization of reflection coefficients.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         void qp_rc_quantization(double *k,         i
 *                                            int np,            i
 *                                            int *nblpc,        i
 *                                            short *lpci,       o
 *                                            double *kq);       o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      An optimal quantization of the reflection coefficients is
 *      performed.
 *
 *      The reflection coefficient quantization tables file must be a
 *      binary file containing double values with the tables for the
 *      np_ coefficients stored sequentially for a maximum of MAXNBITS
 *      bits each
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           qp_quantize
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Aug-2-1994
 *
 *    modification date:        Jan-13-1995
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Rcqunt.c  $
 * Revision 1.9 2005/06/13 10:09:12CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_rc_quantization(double *k, int np, int *nblpc, short *lpci, double *kq)
{
	static const double	QT_LPC[17][256] =
	#include "QT_LPC.h"

	static const int	QT_LPC_SizeInBits[17]=
	#include "QT_LPC_SizeInBits.h"

	int				i;

	for (i=0; i<np; i++) {

		if((i == (np - 1)) && (nblpc[i] == QT_LPC_SizeInBits[i+1]))
			kq[i] = qp_quantize(k[i], QT_LPC[i+1], QT_LPC_SizeInBits[i+1], lpci+i);
		else
			kq[i] = qp_quantize(k[i], QT_LPC[i], QT_LPC_SizeInBits[i], lpci+i);
	}
}

}