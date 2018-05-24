/***ABS******************************************************************
 *                                  mpe.c
 * -------------------------------------------------------------------- *
 *
 *   mp_excitation_evaluation  -  evaluation of multi-pulse excitation.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:   void mp_excitation_evaluation(
 *                                        double *t,                i
 *                                        double *h,                i
 *                                        double *hh,               i
 *                                        double *empe,             o
 *                                        unsigned long *indexe,    o
 *                                        short *indicesa)          o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine generates a multi-pulse excitation vector using
 *      an analysis-by-synthesis loop.
 *      The target vector is 't', the truncated impulse response of
 *      the weighting/synthesis filter cascade is 'h' (length =
 *      'excveclen_').
 *      The excitation vector of 'excveclen_*fs_' samples length
 *      will contain 'nimps_' excitation pulses.
 *      The resulting excitation vector is returned in 'empe'.
 *
 *      This routine uses a suboptimal method to derive a
 *      multi-pulse excitation vector, which is based on Singhal's
 *      ideas [1]. Some (hopefully self-explanatory) simplifications
 *      have been introduced in order to obtain a more "DSP-friendly"
 *      algorithm.
 *
 *      After completion of the MPE optimization the found pulses
 *      are sorted according to their locations within the frame
 *      (descending order) and the positions are encoded to give
 *      one long word. The resulting positions code value is returned
 *      in 'indexe'.
 *
 *      The amplitudes quantization indices are returned in array
 *      'indicesa', the block maximum index being the first entry.
 *
 *      [1] Singhal, S., Atal, B. S., "Amplitude Optimization and
 *          Pitch Prediction in Multipulse Coders", IEEE Trans. ASSP,
 *          vol. 37, no. 3, pp. 317-327, March 1989.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    excveclen_
 *                              fs_
 *                              lhm_
 *                              nimps_
 *                              ampquant_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Apr-9-1992
 *
 *    modification date:        Dec-2-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

#define  MIN(a,b)  ((a) < (b) ? (a) : (b))
#define  MAX(a,b)  ((a) > (b) ? (a) : (b))

namespace StandardPlay {

static void bubblesort(int *pos, double *amp, int length)
{
int       i, j, aux;
double    auxd;
for (i=0; i<length-1; i++)
   for (j=0; j<length-1-i; j++)
      if (pos[j] < pos[j+1])
         {
         aux = pos[j]; pos[j] = pos[j+1]; pos[j+1] = aux;
         auxd = amp[j]; amp[j] = amp[j+1]; amp[j+1] = auxd;
         }
}

static void amplitudes_quantization(dsswork& w, double *amp, short *indices)
{
static const double   gmqtbl[510] =
#include "sp_GMQTBLD.H"
static const double   pqtbl[510] =
#include "PQTBLD.H"

int            i;
double         max;
double         maxq;
/* --- detection of maximum magnitude --- */
for (max=0, i=0; i<w.wk.nimps_; i++) if (fabs(amp[i]) > max) max = fabs(amp[i]);
/* --- quantization of block maximum --- */
maxq = quantize(max, gmqtbl, w.wk.nbgm_, indices);
if (maxq > 0)
   {
   /* --- quantization of normalized amplitudes --- */
   for (i=0; i<w.wk.nimps_; i++)
      amp[i] = quantize(amp[i]/maxq, pqtbl, w.wk.nbp_, indices+1+i) * maxq;
   }
else
   for (i=0; i<w.wk.nimps_; i++) {indices[i+1] = 0; amp[i] = 0;}
}

void mp_excitation_evaluation(dsswork& w, double *t, double *h, double *hh,
                 double *empe, unsigned long *indexe, short *indicesa)
{
LONG                  i, j;
long				  k, n;
double                *th;
int                   *pos;
double                *d;
double                **m;
double                *amp;
double                max;
int                   ibest;
register double       accu;
unsigned long         ul_accu;
double                *p1, *p2;
double                one_over_enh;
int                   start;
long                  y[8];

/* --- initialization --- */
if (!w.wk.mp_excitation_evaluation_init)
   {
   w.wk.mp_excitation_evaluation_N = (int)(w.wk.excveclen_*w.wk.fs_);
   w.wk.mp_excitation_evaluation_lh = (int)(w.wk.lhm_*w.wk.fs_);
   w.wk.mp_excitation_evaluation_init = 1;
   }

/* --- allocate memory --- */
th = (double *)calloc(w.wk.mp_excitation_evaluation_N, sizeof(double));
pos = (int *)calloc(w.wk.nimps_, sizeof(int));
d = (double *)calloc(w.wk.nimps_, sizeof(double));
if (w.wk.nimps_ > 1)
   {
   m = (double **)calloc(w.wk.mp_excitation_evaluation_N, sizeof(double *));
   for (i=0; i<w.wk.mp_excitation_evaluation_N && m!=NULL; i++)
      if ((m[i] = (double *)calloc(w.wk.nimps_-1, sizeof(double))) == NULL) m = NULL;
   if (m == NULL)
      {
      printf("\nmp_excitation_evaluation: memory allocation error!\n\n");
      exit(1);
      }
   }
amp = (double *)calloc(w.wk.nimps_, sizeof(double));
if (th==NULL || d==NULL || pos==NULL || amp==NULL)
   {
   printf("\nmp_excitation_evaluation: memory allocation error!\n\n");
   exit(1);
   }

/* --- initialize vector th --- */
for (i=0; i<w.wk.mp_excitation_evaluation_N; th[i++]=accu)
   for (accu=0, p1=h, p2=t+i, n=i; n<MIN(w.wk.mp_excitation_evaluation_N,i+w.wk.mp_excitation_evaluation_lh); n++)
      accu += *p1++ * *p2++;

/* --- to avoid repeated division by hh[0] --- */
one_over_enh = 1. / hh[0];

/* --- search first pulse --- */
for (ibest=0, max=0, i=0; i<w.wk.mp_excitation_evaluation_N; i++)
   if ((accu=th[i]*th[i]) > max) {max = accu; ibest = i;}
pos[0] = ibest;
d[0] = th[ibest];

/* --- search remaining pulses --- */
for (j=0; j<w.wk.nimps_-1;)
   {
   /* --- compute new column for matrix m and update th --- */
   for (i=0; i<w.wk.mp_excitation_evaluation_N; i++)
      {
      for (accu=0, n=0; n<j; n++) accu += m[i][n] * m[ibest][n];
      m[i][j] = hh[abs(i-ibest)] * one_over_enh - accu;
      th[i] -= m[i][j] * d[j];
      }
   /* --- force elements of th corresponding to already detected pulse
                           positions to zero to avoid repeated detection --- */
   for (k=0; k<=j; k++) th[pos[k]] = 0;
   /* --- search next pulse --- */
   for (ibest=++j, max=0, i=0; i<w.wk.mp_excitation_evaluation_N; i++)
      if ((accu=th[i]*th[i]) > max) {max = accu; ibest = i;}
   pos[j] = ibest;
   d[j] = th[ibest];
   }

/* --- finally, compute pulse amplitudes --- */
for (j=w.wk.nimps_-1; j>=0; j--)
   {
   for (accu=0, k=j+1; k<w.wk.nimps_; k++) accu += m[pos[k]][j] * amp[k];
   amp[j] = d[j] * one_over_enh - accu;
   }

/* --- sort pulses and encode pulse positions --- */
bubblesort(pos, amp, w.wk.nimps_);
for (j=0; j<w.wk.nimps_; j++) y[j] = 0;
for (start=0, ul_accu=0, j=w.wk.nimps_-1; j>=0; j--)
   {
   for (n=start; n<pos[j]; n++)
      {
      for (i=w.wk.nimps_-1; i>0; i--) y[i] += y[i-1];
      y[0] = n+1;
      }
   ul_accu += y[w.wk.nimps_-j-1];
   start = pos[j];
   }
*indexe = ul_accu;

/* --- quantization of pulse amplitudes --- */
if (w.wk.ampquant_) amplitudes_quantization(w, amp, indicesa);

/* --- construct multi-pulse excitation vector --- */
for (n=0; n<w.wk.mp_excitation_evaluation_N; n++) empe[n] = 0;
for (j=0; j<w.wk.nimps_; j++) empe[pos[j]] = amp[j];

/* --- free memory --- */
free(th);
free(pos);
free(d);
if (w.wk.nimps_ > 1)
   {
   for (i=0; i<w.wk.mp_excitation_evaluation_N; i++) free(m[i]);
   free(m);
   }
free(amp);
}
}
