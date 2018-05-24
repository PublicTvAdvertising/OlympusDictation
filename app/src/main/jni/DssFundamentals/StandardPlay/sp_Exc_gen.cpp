/***ABS******************************************************************
 *                                exc_gen.c
 * -------------------------------------------------------------------- *
 *
 *   excitation_generation  -  generates excitation vector.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:   void excitation_generation(int M,               i
 *                                            short betai,         i
 *                                            unsigned long ei,    i
 *                                            short *ai,           i
 *                                            double *e);          o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine generates the excitation vector using the 
 *      excitation parameters LTP lag, encoded LTP gain, MPE position
 *      code, and encoded MPE amplitudes (block maximum and individual
 *      relative amplitudes).
 *      The excitation vector returned in 'e' is the compound
 *      excitation consisting of both the adaptive excitation and the
 *      (MP) innovation.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    maxlag_
 *                              excveclen_
 *                              fs_
 *                              nbga_
 *                              nbgm_
 *                              nbp_
 *                              nimps_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Feb-29-1996
 *
 ************************************************************************
 *
 *    Copyright (C) 1995, 1996 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"
#include "sp_pow2.h"

namespace StandardPlay {

void excitation_generation(dsswork& w, int M, short betai, unsigned long ei,
                                                      short *ai, double *e)
{

static const double         gaqtbl[510]=
#include "sp_GAQTBLD.H"
static const double         gmqtbl[510] =
#include "sp_GMQTBLD.H"
static const double         pqtbl[510] =
#include "PQTBLD.H"

double                beta;
unsigned long         index;
int                   *pos;
double                max;
double                *amp;
int                   i, j, k;
ULONG         y[8];

/* --- initialize --- */
if (0 == M && 0 == betai && 0 == ei && 0 == ai && 0 == e) {
	free(w.wk.excitation_generation_excdel);
	w.wk.excitation_generation_excdel = 0;
	w.wk.excitation_generation_ed = 0;
	w.wk.excitation_generation_init = 0;
	return;
}
if (!w.wk.excitation_generation_init)
   {
   w.wk.excitation_generation_excdel = (double *)calloc(w.wk.maxlag_+1, sizeof(double));
   if (w.wk.excitation_generation_excdel==NULL)
      {
      printf("\nexcitation_generation init: memory allocation error!\n\n");
      exit(1);
      }
   w.wk.excitation_generation_ed = w.wk.excitation_generation_excdel + w.wk.maxlag_ + 1;
   w.wk.excitation_generation_N = (int)(w.wk.excveclen_*w.wk.fs_);
   w.wk.excitation_generation_init = 1;
   }

/* --- allocate memory --- */
pos = (int *)calloc(w.wk.nimps_, sizeof(int));
amp = (double *)calloc(w.wk.nimps_, sizeof(double));
if (pos==NULL || amp==NULL)
   {
   printf("\nexcitation_generation: memory allocation error!\n\n");
   exit(1);
   }

/* --- compute adaptive excitation --- */
beta = gaqtbl[pow2[w.wk.nbga_]-2 + betai];
for (k=1, i=0; i<w.wk.excitation_generation_N; k++)
   for (; i<k*M && i<w.wk.excitation_generation_N; i++)
      e[i] = beta * w.wk.excitation_generation_ed[i-k*M];

/* --- add innovation --- */
y[0] = w.wk.excitation_generation_N;                  /*  y[j]  =  72 choose (j+1)  */
y[1] = (y[0]*(w.wk.excitation_generation_N-1))/2;
y[2] = (y[1]*(w.wk.excitation_generation_N-2))/3;
y[3] = (y[2]*(w.wk.excitation_generation_N-3))/4;
y[4] = (y[3]*(w.wk.excitation_generation_N-4))/5;
y[5] = (y[4]*(w.wk.excitation_generation_N-5))/6;
y[6] = (ULONG)((double)y[5]*(w.wk.excitation_generation_N-6)/7);
y[7] = (ULONG)((double)y[6]*(w.wk.excitation_generation_N-7)/8);
index = ei;
if (index >= y[w.wk.nimps_-1]) index = y[w.wk.nimps_-1] - 1; // make sure there is no invalid poscode
for (j=w.wk.nimps_-1; j>=0; j--)
   {
   while (index < y[j]) for (y[0]--,k=1; k<=j; k++) y[k] -= y[k-1];
   pos[w.wk.nimps_-1-j] = y[0];
   index -= y[j];
   }
max = gmqtbl[pow2[w.wk.nbgm_]-2+ai[0]];
for (j=0; j<w.wk.nimps_; j++) amp[j] = max * pqtbl[pow2[w.wk.nbp_]-2+ai[j+1]];
for (j=0; j<w.wk.nimps_; j++) e[pos[j]] += amp[j];

/* --- update excitation delay line --- */
for (i=0; i<=w.wk.maxlag_-w.wk.excitation_generation_N; i++) w.wk.excitation_generation_excdel[i] = w.wk.excitation_generation_excdel[i+w.wk.excitation_generation_N];
for (i=0; i<w.wk.excitation_generation_N; i++) w.wk.excitation_generation_excdel[i+w.wk.maxlag_+1-w.wk.excitation_generation_N] = e[i];

/* --- free memory --- */
free(pos);
free(amp);
}
}
