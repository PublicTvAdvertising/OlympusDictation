/***ABS******************************************************************
 *                                  quant.c
 * -------------------------------------------------------------------- *
 *
 *   qp_quantize  -  nonlinear quantization with table look-up.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         double qp_quantize(double inp,             i
 *                                       double *table           i
 *                                       int nbits,              i
 *                                       short *index);          o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The quantization of the input value is done by performing
 *      a binary search in a prestored table of quantized output
 *      values. The limits of the quantizer intervals are taken
 *      to be the average of adjacent table entries.
 *      The quantization tables needed consist of successive parts
 *      for one number of bits each starting at one and reaching up
 *      to the maximum number of bits that will ever be passed to
 *      this routine in nbits!
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Aug-02-1994
 *
 *    modification date:        Sep-18-1995
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Quant.c  $
 * Revision 1.9 2005/06/13 10:09:11CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#include "qp_pow2.h"

namespace QualityPlay {

double qp_quantize(double inp, const double *table, int nbits, short *index)
{
	int      i;
	int      offset, weight;
	double   thres;

	/* --- binary search --- */
	offset = weight = pow2[nbits-1];

	for (i=1; i<nbits; i++)	{
		thres = (table[offset-1] + table[offset]) / 2;
		weight /= 2;
		if (inp > thres)
			offset += weight;
		else
			offset -= weight;
	}

	thres = (table[offset-1] + table[offset]) / 2;
	if (inp > thres)
		*index = (short)offset;
	else
		*index = (short)offset - 1;

	return(table[*index]);
}

}