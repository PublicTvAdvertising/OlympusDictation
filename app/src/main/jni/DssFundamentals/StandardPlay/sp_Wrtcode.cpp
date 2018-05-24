/***ABS******************************************************************
 *                              wrtcode.c
 * -------------------------------------------------------------------- *
 *
 *   wrtcode  -  writes encoded parameters to output file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void wrtcode(FILE *fpo,                 i
 *                                   short va,                  i
 *                                   short *lpci,               i
 *                                   short *Mi,                 i
 *                                   short *betai,              i
 *                                   long *ei,                  i
 *                                   short **ai)                i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The output file has to be opened elsewhere and only the file
 *      pointer is passed to this routine. Data is written to the
 *      output file bit-wise (bit allocation is given by the parameter
 *      definition).
 *
 *      The LTP lags of the whole frame are encoded jointly inside
 *      this routine in order to save bit rate.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    synfrmlen_
 *                              excveclen_
 *                              np_
 *                              nblpc_
 *                              maxlag_
 *                              minlag_
 *                              difflagrange_
 *                              nbga_
 *                              fs_
 *                              nimps_
 *                              nbgm_
 *                              nbp_
 *                              scon_
 *                              nbgn_
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           putbits
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-9-1995
 *
 *    modification date:        Oct-18-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

#define LD(a)     (log((double)a)/log((double)2.))

namespace StandardPlay {

static double nchoosek(int n, int k)
{
double i=1, j=1;
for (; k>0; i*=n--, j*=k--);
return(i/j);
}

void wrtcode(dsswork& w, FILE *fpo, short va, short *lpci, short *Mi, short *betai,
                                            unsigned long *ei, short **ai)
{
int             i, k;
int             nsbfrms;
unsigned long   lagscode;
unsigned long   aux;

nsbfrms = (int)(w.wk.synfrmlen_ / w.wk.excveclen_);

/* --- in silence compression mode write VA flag --- */
if (w.wk.scon_) putbits(w, fpo, va, 1);

/* --- write encoded LPC coefficients --- */
for (i=0; i<w.wk.np_; i++) putbits(w, fpo, lpci[i], w.wk.nblpc_[i]);

/* --- write excitation parameters --- */
if (va)
   {
   /* --- loop over subframes --- */
   for (i=0; i<nsbfrms; i++)
      {
      /* --- write encoded LTP gain --- */
      putbits(w, fpo, betai[i], w.wk.nbga_);
      /* --- write encoded pulse positions --- */
      putbits(w, fpo, ei[i], (int)ceil(LD(nchoosek((int)((w.wk.fs_*w.wk.excveclen_)),w.wk.nimps_))));
      /* --- write encoded pulse amplitudes --- */
      putbits(w, fpo, ai[i][0], w.wk.nbgm_);
      for (k=1; k<=w.wk.nimps_; k++) putbits(w, fpo, ai[i][k], w.wk.nbp_);
      }

   /* --- encode LTP lags and write resutling code --- */
   for (lagscode=Mi[nsbfrms-1], i=nsbfrms-2; i>0; i--)
      {
      lagscode *= w.wk.difflagrange_;             /* long multiplication !!! */
      lagscode += Mi[i];
      }
   lagscode *= w.wk.maxlag_-w.wk.minlag_+1;
   lagscode += Mi[0];
   for (aux=w.wk.maxlag_-w.wk.minlag_+1, i=1; i<nsbfrms; i++) aux *= w.wk.difflagrange_;putbits(w, fpo, lagscode, (short)ceil(LD(aux-1)));
   }
else
   for (i=0; i<nsbfrms; i++) putbits(w, fpo, ai[i][0], w.wk.nbgn_);
}
}
