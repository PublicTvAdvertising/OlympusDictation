/************************************************************************
 *                                 setpar.h
 *
 ************************************************************************
 * $Log: Setpar.h  $
 * Revision 1.20 2005/06/13 11:48:50CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

/* --- set parameters --- */

static void setpar(dsswork& w)
{

/* common */
	w.wk.synfrmlen_      = 16;

/* input */
	w.wk.hdrlen_         = 44; /* always 44 in wave files*/
	w.wk.notchfilter_    = 1;
	w.wk.emphasiscoeff_  = 0.1;

/* LPC */
	w.wk.anlfrmlen_      = 20;
	w.wk.np_             = 16;
	w.wk.stab_           = 0.0001;
	w.wk.bwe_            = 20.;

/* excitation */
	w.wk.excveclen_      = 4.;

/* adaptive excitation */
	w.wk.minlag_         = 45;
	w.wk.maxlag_         = 300;
	w.wk.difflagrange_   = 256;
	w.wk.alphaa_         = 0.7;
	w.wk.lha_            = 3.;

/* innovation */
	w.wk.lhm_            = 3.9;
	w.wk.alpham_         = 0.9;
	w.wk.nimps_          = 11;

/* silence compression */
//	scon_ = 0;
	w.wk.reset_memories_ = 0;

/* quantization */
	w.wk.lpcquant_       = 1;
	w.wk.ltpquant_       = 1;
	w.wk.ampquant_       = 1;
}