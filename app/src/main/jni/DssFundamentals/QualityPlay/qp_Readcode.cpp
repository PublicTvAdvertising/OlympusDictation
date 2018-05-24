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
 *    calls routines:           qp_getbits
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Oct-18-1995
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Readcode.c  $
 * Revision 1.7 2005/06/13 11:40:53CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

#define LD(a)     (log((double)a)/log((double)2.))

namespace QualityPlay {

static double nchoosek(int n, int k)
{
   double i=1, j=1;
   for (; k>0; i*=n--, j*=k--);
   return i/j;
}

short  qp_readcode(dsswork& w, FILE *fpi, short *va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai)
{
	int  i, k;
	int  nsbfrms;
	long aux;

	nsbfrms = (int)(w.wk.synfrmlen_ / w.wk.excveclen_);

	/* --- in silence compression mode, read VA flag, otherwise set flag --- */
	if (w.wk.scon_)
		*va = (short)qp_getbits(w, fpi, 1);
	else
		*va = 1;

	/* --- read encoded LPC coefficients --- */
	for (i=0; i<w.wk.np_; i++) 
		lpci[i] = (short)qp_getbits(w, fpi, w.wk.nblpc_[i]);

	/* --- check for end of file --- */
	if (IO_IsEOF(w, 0)) 
		return 0;

	/* --- read excitation parameters --- */
	if (*va) {
		/* --- loop over subframes --- */
		for (i=0; i<nsbfrms; i++) {

			/* --- read encoded LTP Lag --- */
			Mi[i] = (short)qp_getbits(w, fpi, (i==0) ? ((int)ceil(LD(w.wk.maxlag_-w.wk.minlag_+1))) : ((int)ceil(LD(w.wk.difflagrange_)))  );

			/* --- read encoded LTP gain --- */
			betai[i] = (short)qp_getbits(w, fpi, w.wk.nbga_);

			/* --- read encoded pulse positions --- */
			ei[i] = qp_getbits(w, fpi, (int)ceil(LD(nchoosek((int)((w.wk.fs_*w.wk.excveclen_)),w.wk.nimps_))));

			/* --- read encoded pulse amplitudes --- */
			ai[i][0] = (short)qp_getbits(w, fpi, w.wk.nbgm_);

			for (k=1; k<=w.wk.nimps_; k++) 
				ai[i][k] = (short)qp_getbits(w, fpi, w.wk.nbp_);
		}
	}
	else
		for (i=0; i<nsbfrms; i++) 
			ai[i][0] = (short)qp_getbits(w, fpi, w.wk.nbgn_);

	return 1;
}

}