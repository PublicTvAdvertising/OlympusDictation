/***ABS******************************************************************
 *                                 latfilr.c
 * -------------------------------------------------------------------- *
 *
 *   latfilr  -  performs recursive lattice filtering.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         void latfilr(double *x,                 i
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
 *       ep(i-1) = ep(i) - k(i-1) * en(i-1) ,   N >= i >= 1,
 *
 *      with
 *
 *       ep(N) = x(n),       y(n) = ep(0)
 *
 *      are evaluated using the filter memory (mem)
 *
 *       en(i) ,                                0 <= i <= N-1,
 *
 *      which is updated according to
 *
 *       en(i+1) = en(i) + k(i) * ep(i),        0 <= i <= N-1,
 *
 *      with
 *
 *       en(0) = y(n).
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
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

namespace StandardPlay {

void latfilr(double *x, int B, int M, int N, double *k, double *mem, double *y)
{
int     i, n;
double  *en;
double  ep;

if ((en=(double *)calloc(N, sizeof(double))) == NULL)
   {
   printf("\nlatfilnr: memory allocation error!\n\n");
   exit(1);
   }
for (i=0; i<N; i++) en[i] = mem[i];
for (n=0; n<B; n++)
   {
   ep = x[n] - k[N-1] * en[N-1];
   for (i=N-2; i>=0; i--)
      {
      ep -= k[i] * en[i];
      en[i+1] = en[i] + k[i] * ep;
      }
   y[n] = en[0] = ep;
   if (n == M-1) for (i=0; i<N; i++) mem[i] = en[i];
   }
free(en);
}
}
