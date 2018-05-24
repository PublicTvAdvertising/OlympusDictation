/***ABS******************************************************************
 *                               stepdown.c
 * -------------------------------------------------------------------- *
 *
 *   stepdown  -  computes reflection coefficients
 *                                             from LPC coefficients.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void stepdown(double *a,              i
 *                                    int np,                 i
 *                                    double *k);             o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This funtion performs the recursive computation of the np
 *      reflection coefficients from the np+1 LPC coefficients.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB/INESC, Grundig
 *
 *    creation date:            Jul-8-1991
 *
 *    modification date:        Nov-30-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

#define MAXORD       20

namespace StandardPlay {

void stepdown(double *a, int np, double *k)
{
int     i, m;
double  aa[MAXORD+1], t[MAXORD], ee, fac;
for (i=0; i<=np; i++) aa[i] = a[i];
k[np-1] = aa[np];
for (i=np-2; i>=0; i--)
   {
   ee = 1. - k[i+1] * k[i+1];
   if (ee) fac = 1. / ee; else fac = 1e20;
   for (m=0; m<=i+1; m++) t[m] = (aa[m] - k[i+1] * aa[i-m+2]) * fac;
   for (m=0; m<=i+1; m++) aa[m] = t[m];
   k[i] = aa[i+1];
   }
}
}
