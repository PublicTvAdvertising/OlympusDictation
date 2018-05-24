/***ABS******************************************************************
 *                              readcode.c
 * -------------------------------------------------------------------- *
 *
 *   readcode  -  reads encoded parameters from input file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void readcode(FILE *fpi,                 i
 *                                    short *va,                 o
 *                                    short *lpci,               o
 *                                    short *Mi,                 o
 *                                    short *betai,              o
 *                                    long *ei,                  o
 *                                    short **ai)                o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The input file has to be opened elsewhere and only the file
 *      pointer is passed to this routine. Data is read from the
 *      input file bit-wise (bit allocation is given by the parameter
 *      definition).
 *
 *      The LTP lags of the whole frame are decoded inside this
 *      routine.
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
 *    calls routines:           getbits
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1995
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
return i/j;
}

short readcode(dsswork& w, FILE *fpi, short *va, short *lpci, short *Mi, short *betai,
                                              unsigned long *ei, short **ai)
{
int             i, k;
int             nsbfrms;
unsigned long   lagscode;
long            aux;

nsbfrms = (int)(w.wk.synfrmlen_ / w.wk.excveclen_);

/* --- in silence compression mode, read VA flag, otherwise set flag --- */
if (w.wk.scon_)
   *va = (short)getbits(w, fpi, 1);
else
   *va = 1;

/* --- read encoded LPC coefficients --- */
for (i=0; i<w.wk.np_; i++) lpci[i] = (short)getbits(w, fpi, w.wk.nblpc_[i]);

/* --- check for end of file --- */
if (IO_IsEOF(w, fpi))
	return 0;

/* --- read excitation parameters --- */
if (*va)
   {
   /* --- loop over subframes --- */
   for (i=0; i<nsbfrms; i++)
      {
      /* --- read encoded LTP gain --- */
      betai[i] = (short)getbits(w, fpi, w.wk.nbga_);
      /* --- read encoded pulse positions --- */
      ei[i] = getbits(w, fpi, (int)ceil(LD(nchoosek((int)((w.wk.fs_*w.wk.excveclen_)),w.wk.nimps_))));
      /* --- read encoded pulse amplitudes --- */
      ai[i][0] = (short)getbits(w, fpi, w.wk.nbgm_);
      for (k=1; k<=w.wk.nimps_; k++) ai[i][k] = (short)getbits(w, fpi, w.wk.nbp_);
      }

   /* --- read LTP lags code and decode --- */
   for (aux=w.wk.maxlag_-w.wk.minlag_+1, i=1; i<nsbfrms; i++) aux *= w.wk.difflagrange_;
   lagscode = getbits(w, fpi, (int)ceil(LD(aux-1)));
   aux = lagscode / (w.wk.maxlag_-w.wk.minlag_+1);                        /* !! */
   Mi[0] = (short)(lagscode - aux * (w.wk.maxlag_-w.wk.minlag_+1));       /* !! */
   for (i=1; i<nsbfrms; i++)
      {
      lagscode = aux;
      aux = lagscode / w.wk.difflagrange_;                           /* !! */
      Mi[i] = (short)(lagscode - aux * w.wk.difflagrange_);          /* !! */
      }
   }
else
   for (i=0; i<nsbfrms; i++) ai[i][0] = (short)getbits(w, fpi, w.wk.nbgn_);

return 1;
}
}
