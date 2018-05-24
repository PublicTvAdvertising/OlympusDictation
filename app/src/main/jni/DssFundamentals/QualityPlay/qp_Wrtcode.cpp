/***ABS******************************************************************
 *                              wrtcode.c
 * -------------------------------------------------------------------- *
 *
 *   qp_wrtcode  -  writes encoded parameters to output file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void qp_wrtcode(FILE *fpo,                 i
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
 *    calls routines:           qp_putbits
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-09-1995
 *
 *    modification date:        Oct-18-1995
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Wrtcode.c  $
 * Revision 1.8 2005/06/13 11:41:07CEST kroener 
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
	return(i/j);
}

void qp_wrtcode(dsswork& w, FILE *fpo, short  va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai)
{
	int i, k;
	int nsbfrms;

	nsbfrms = (int)(w.wk.synfrmlen_ / w.wk.excveclen_);

	/* --- in silence compression mode write VA flag --- */
	if (w.wk.scon_) 
		qp_putbits(w, fpo, (__int64_t)va, 1);

	/* --- write encoded LPC coefficients --- */
	for (i=0; i<w.wk.np_; i++) 
		qp_putbits(w, fpo, (__int64_t)lpci[i], w.wk.nblpc_[i]);
	
	/* --- write excitation parameters --- */
	if (va)	{

		/* --- loop over subframes --- */
		for (i=0; i<nsbfrms; i++) {

			/* --- write encoded LTP Lag --- */ // changed 2003.10.13 by Michael Kroener
			qp_putbits(w, fpo, (__int64_t)(Mi[i]) , (i==0) ? ((int)ceil(LD(w.wk.maxlag_-w.wk.minlag_+1))) : ((int)ceil(LD(w.wk.difflagrange_)))   );

			/* --- write encoded LTP gain --- */
			qp_putbits(w, fpo, (__int64_t)(betai[i]), w.wk.nbga_);

			/* --- write encoded pulse positions --- */
			qp_putbits(w, fpo, ei[i], (int)ceil(LD(nchoosek((int)((w.wk.fs_*w.wk.excveclen_)),w.wk.nimps_))));

			/* --- write encoded pulse amplitudes --- */
			qp_putbits(w, fpo, (__int64_t)(ai[i][0]), w.wk.nbgm_); //Blockmaximum

			for (k=1; k<=w.wk.nimps_; k++) 
				qp_putbits(w, fpo, (__int64_t)(ai[i][k]), w.wk.nbp_); // Amps
		}
	}
	else
		for (i=0; i<nsbfrms; i++) 
			qp_putbits(w, fpo, (__int64_t)(ai[i][0]), w.wk.nbgn_);
}

}