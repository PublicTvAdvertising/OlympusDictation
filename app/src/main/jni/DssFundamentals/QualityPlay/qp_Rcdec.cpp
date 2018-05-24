/***ABS******************************************************************
 *                                rcdec.c
 * -------------------------------------------------------------------- *
 *
 *   rc_decoding  -  performs decoding of LPC parameters endoded by
 *         quasi-optimal scalar quanization of reflection coefficients.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:          void rc_decoding(short *lpci,         i
 *                                         int np,              i
 *                                         int *nblpc,          i
 *                                         double *kq);         o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The reflection coefficient quantization tables file must be a
 *      binary file containing double values with the tables for the
 *      np_ coefficients stored sequentially for a maximum of MAXNBITS
 *      bits each
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Feb-02-1996
 *                              Oct-10-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Rcdec.c  $
 * Revision 1.8 2005/06/13 11:40:52CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#include "qp_pow2.h"

namespace QualityPlay {

void qp_rc_decoding(short *lpci, int np, int *nblpc, double *kq)
{
	static const double	QT_LPC[17][256] =
	#include "QT_LPC.h"

	static const int	QT_LPC_SizeInBits[17]=
	#include "QT_LPC_SizeInBits.h"

	int				i;

	for (i=0; i<np; i++) {

		if((i == (np - 1)) && (nblpc[i] == QT_LPC_SizeInBits[i+1]))
			kq[i] = QT_LPC[i+1][lpci[i]];
		else
			kq[i] = QT_LPC[i][lpci[i]];
	}
}
}