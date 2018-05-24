/***ABS******************************************************************
 *                                 abs_syn.c
 * -------------------------------------------------------------------- *
 *
 *   abs_syn  -  synthesis (decoding).
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void abs_syn(short va,                  i
 *                                   short *lpci,               i
 *                                   short *Mi,                 i
 *                                   short *betai,              i
 *                                   unsigned long *ei,         i
 *                                   short **ai,                i
 *                                   double *shat,);            o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine synthesises one frame of speech from the
 *      encoded parameters passed to this function.
 *
 *      This version integrates several functions which have been
 *      modified in order to apply integer arithmetic, only.
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           qp_rc_quantization ??
 *                              excitation_generation
 *                              latfilr
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    fs_
 *                              excveclen_
 *                              synfrmlen_
 *                              np_
 *                              nblpc_
 *                              minlag_
 *                              maxlag_
 *                              difflagrange_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Feb-02-1996
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Abs_syn.c  $
 * Revision 1.7 2005/06/13 11:39:39CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

#define MIN(a,b) ((a)<(b) ? (a):(b))
#define MAX(a,b) ((a)>(b) ? (a):(b))

namespace QualityPlay {

void qp_abs_syn(dsswork& w, short va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai, double *shat)
{
	static const double	gnqtbl[] =
	#include "qp_Gnqtbld.h"

	double			*k;
	double			*e;
	int				M;
	int				i, n;

	/* Alexis: static mema_sf must be freed! */
	if (va == 0 && lpci == 0 && Mi == 0 && betai == 0 && ei == 0 && ai == 0 && shat == 0) {
		free(w.wk.qp_abs_syn_mema_sf);
		w.wk.qp_abs_syn_mema_sf = 0;
		w.wk.qp_abs_syn_init = 0;
		return;
	}	
	/* --- initialize --- */
	if (!w.wk.qp_abs_syn_init) {
		w.wk.qp_abs_syn_S = w.wk.synfrmlen_*w.wk.fs_;
		w.wk.qp_abs_syn_N = (int)(w.wk.excveclen_*w.wk.fs_);
		if ((w.wk.qp_abs_syn_mema_sf = (double *)calloc(w.wk.np_, sizeof(double))) == NULL) {
			//fprintf(ERROROUT, "\nabs_syn init: memory allocation error!\n\n");
			exit(1);
		}
		w.wk.qp_abs_syn_init = 1;
	}

	/* --- allocate memory --- */
	k = (double *)calloc(w.wk.np_, sizeof(double));
	e = (double *)calloc(w.wk.qp_abs_syn_N, sizeof(double));
	if (k == NULL || e == NULL) {
		//fprintf(ERROROUT, "\nabs_syn: memory allocation error!\n\n");
		exit(1);
	}

	/* --- LPC parameter decoding --- */
	qp_rc_decoding(lpci, w.wk.np_, w.wk.nblpc_, k);

	/* --- loop over subframes --- */
	for (i=0; i<w.wk.qp_abs_syn_S/w.wk.qp_abs_syn_N; i++) {
		if (va) {
			/* --- parameter decoding, generation of excitation vector --- */
			if(i==0)
				M = Mi[0] + w.wk.minlag_;
			else {
				if(M<= w.wk.maxlag_-w.wk.difflagrange_/2)
					M = Mi[i] + MAX(w.wk.minlag_, M-w.wk.difflagrange_/2+1);
				else
					M = Mi[i] + MAX(w.wk.minlag_, w.wk.maxlag_-w.wk.difflagrange_+1);
			}
			qp_excitation_generation(w, M, betai[i], ei[i], ai[i], e);
		}
		else {
			/* --- generate noise excitation vector --- */
			for (n=0; n<w.wk.qp_abs_syn_N; n++)
				e[n] = gnqtbl[ai[i][0]] * (double)qp_noisegen(&w.wk.seed_);
		}

		/* --- synthesis filtering --- */
		qp_latfilr(e, w.wk.qp_abs_syn_N, w.wk.qp_abs_syn_N, w.wk.np_, k, w.wk.qp_abs_syn_mema_sf, shat+i*w.wk.qp_abs_syn_N);
	}

	free(k);
	free(e);
}

}
