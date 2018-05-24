/***ABS******************************************************************
 *                                rcqunt.c
 * -------------------------------------------------------------------- *
 *
 *   rc_quantization  -  performs LPC parameter quantization by
 *         quasi-optimal scalar quanization of reflection coefficients.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         void rc_quantization(double *k,         i
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
 *    calls routines:           quantize
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Aug-2-1994
 *
 *    modification date:        Jan-13-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

namespace StandardPlay {

void rc_quantization(double *k, int np, int *nblpc, short *lpci, double *kq)
{
static const double  rcqtbls[14][510] =
#include "sp_RCQTBLSD.H"
int            i;

for (i=0; i<np; i++) kq[i] = quantize(k[i], rcqtbls[i], nblpc[i], lpci+i);
}
}
