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
 *    modification date:        Feb-2-1996
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"
#include "sp_pow2.h"

namespace StandardPlay {

void rc_decoding(short *lpci, int np, int *nblpc, double *kq)
{
static const double         rcqtbls[14][510] =
#include "sp_RCQTBLSD.H"
int                   i;

for (i=0; i<np; i++) kq[i] = rcqtbls[i][pow2[nblpc[i]]-2 + lpci[i]];
}

}
