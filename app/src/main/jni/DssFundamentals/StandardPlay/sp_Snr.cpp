/***ABS******************************************************************
 *                                  snr.c
 * -------------------------------------------------------------------- *
 *
 *   snr  -  computes SNR results during run of abs program.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:             void snr(int f,                  i
 *                                    double *s1,             i/o
 *                                    double *s2);            i/o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      For calls with positive values 'f', the segmental SNR is
 *      computed on the basis of the input signal vector stored
 *      at the location pointed to by 's1' and the corresponding
 *      output signal vector stored in 's2' ('f' values each). The
 *      average for these values as well as the global SNR value is
 *      updated.
 *
 *      A final call of this routine with a negative value passed in
 *      'f' will cause the final SNR results, average segmental SNR
 *      and global SNR, to be returned to the calling function in
 *      '*s1' and '*s2', respectively.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Jan-11-1992
 *
 *    modification date:        Jan-13-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

#define THRES     50.

namespace StandardPlay {

void snr(dsswork& w, int f, double *s1, double *s2)
{
int             i;
double          *ps1, *ps2;
double          S, N, diff, segSNR;

if (f > 0)
   {
   ps1 = s1; ps2 = s2;
   S = 0.;
   N = 0.;
   for (i=0; i<f; i++)
      {
      S += *ps1 * *ps1;
      diff = *ps1++ - *ps2++;
      N += diff * diff;
      }
   if (S/f > THRES)
      {
      w.wk.snr_nfrmsig++;
      if (N > 0.)
         segSNR = 10. * log10(S/N);
      else
         segSNR = 130;
      w.wk.snr_av_segSNR = (w.wk.snr_av_segSNR * (w.wk.snr_nfrmsig-1) + segSNR) / w.wk.snr_nfrmsig;
      }
   w.wk.snr_Stot += S;
   w.wk.snr_Ntot += N;
   w.wk.snr_frmno++;
   }
else
   {
   if (w.wk.snr_Stot > 0. && w.wk.snr_Ntot > 0.)
      {
      *s1 = w.wk.snr_av_segSNR;
      *s2 = 10*log10(w.wk.snr_Stot/w.wk.snr_Ntot);
      }
   else
      *s1 = *s2 = 0;
   }
}
}
