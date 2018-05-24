/***ABS******************************************************************
 *                                stepup.c
 * -------------------------------------------------------------------- *
 *
 *   stepup  -  performs step-up procedure.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void stepup(double *k,                i
 *                                  int np,                 i
 *                                  double *a);               o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This function performs the stepup procedure (part of Levinson's
 *      recursion) returning the 'np'+1 LPC coefficients 'a' given the
 *      'np' reflection coefficients 'k'.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB/INESC, Grundig
 *
 *    creation date:            Jun-22-1991
 *
 *    modification date:        Nov-30-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

namespace StandardPlay {

void stepup(double *k, int np, double *a)
{
int     m, i;
double  ti, tj;
a[0] = 1.;
for (m=0; m<np; m++)
   {
   a[m+1] = k[m];
   for (i=1; i<=(m+1)/2; i++)
      {
      ti = a[i];
      tj = a[m-i+1];
      a[i] = ti + k[m] * tj;
      a[m-i+1] = tj + k[m] * ti;
      }
   }
}
}
