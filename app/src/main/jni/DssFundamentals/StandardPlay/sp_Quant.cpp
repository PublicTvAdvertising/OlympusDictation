/***ABS******************************************************************
 *                                  quant.c
 * -------------------------------------------------------------------- *
 *
 *   quantize  -  nonlinear quantization with table look-up.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         double quantize(double inp,             i
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
 *    creation date:            Aug-2-1994
 *
 *    modification date:        Sep-18-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"
#include "sp_pow2.h"

namespace StandardPlay {

double quantize(double inp, const double *table, int nbits, short *index)
{
const double                *tblptr;
int                   i;
int                   offset, weight;
double                thres;

/* --- set table pointer to first entry of table corresponding to nbits --- */
tblptr = table;
for (i=1; i<nbits; i++) tblptr += pow2[i];

/* --- binary search --- */
offset = weight = pow2[nbits-1];
for (i=1; i<nbits; i++)
   {
   thres = (tblptr[offset-1] + tblptr[offset]) / 2;
   weight /= 2;
   if (inp > thres)
      offset += weight;
   else
      offset -= weight;
   }
thres = (tblptr[offset-1] + tblptr[offset]) / 2;
if (inp > thres)
   *index = (short)offset;
else
   *index = (short)offset - 1;
return(tblptr[*index]);
}
}
