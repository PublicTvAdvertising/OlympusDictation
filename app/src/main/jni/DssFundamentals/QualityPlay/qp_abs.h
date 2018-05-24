/***ABS******************************************************************
 *                                  abs.h
 * -------------------------------------------------------------------- *
 *
 *    Headerfile for ABS C-Programs
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            May-19-1994
 *
 *    modification date:        Feb-02-1996
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Abs.h  $
 * Revision 1.30 2005/06/13 11:49:34CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

/*
Debug Commandline

   Codec : infile="W:\FPDebug\DEBUG.wav" outfile="W:\FPDebug\DEBUG_codec.wav"
   Encode: infile="W:\FPDebug\DEBUG.wav" outfile="W:\FPDebug\DEBUG_encode.rss"
   Decode: infile="W:\FPDebug\DEBUG_encode.rss" outfile="W:\FPDebug\DEBUG_decode.wav"
*/

/* --- debug issues - Michael Kroener 2003.06.24 ---------------------------------------------------- */
//#define NOOUTPUT       /* disables: output to file                                                  */
//#define SHOW_PARAMETER /* enables : show arguments                                                  */
//#define LTP_5_Bit // otherwise LTP Gain has 6 Bit resolution

/* -------------------------------------------------------------------------------------------------- */

#include <stdio.h>
#include <math.h>
#include <string.h>
#include <sys/types.h>
#include <stdlib.h>
#include "qp_wave.h"

/* --- constants --- */
#define DATE      __DATE__
#define ERROROUT  stderr
#define DSHOW_IO 

#include "../dsswork.h"
#include "../CustomIO.h"


namespace QualityPlay {

void	qp_abs_anl(dsswork& w, double *s, short *va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai);
void	qp_abs_syn(dsswork& w, short va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai, double *shat);
short	qp_readcode(dsswork& w, FILE *fpi, short *va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai);
void	qp_wrtcode(dsswork& w, FILE *fpo, short  va, short *lpci, short *Mi, short *betai, __int64_t *ei, short **ai);
void	qp_excitation_evaluation(dsswork& w, double *r, double *kmpe, double *hmpe, double *hhmpe, double *e, __int64_t *indexe, short *indicesa);
void	qp_excitation_generation(dsswork& w, int M, short betai, __int64_t ei, short *ai, double *e);
void	qp_mp_excitation_evaluation(dsswork& w, double *t, double *h, double *hh, double *empe, __int64_t *indexe, short *indicesa);
void	qp_putbits(dsswork& w, FILE *fpo, __int64_t bits, int nbits);
__int64_t	qp_getbits(dsswork& w, FILE *fpi, int nbits);

/* ABS enc-dec */
void	qp_abs_anl_syn(dsswork& w, double *si, double *so);
void	abs_init(int argc, char *argv[], FILE **fpi, FILE **fpo, wavfile_chunk_type *wcti, wavfile_chunk_type *wcto);

/* enc */

/* dec */
#ifdef DSHOW_IO
void	qp_absdinit(dsswork& w, int *nbpf);
void	qp_abseinit(dsswork& w);
void	qp_set_silence_compression(dsswork& w, int on);
int		qp_get_silence_compression(dsswork& w);
#else
void	qp_absdinit(int argc, char *argv[], FILE **fpi, FILE **fpo, wavfile_chunk_type *wcto, int *nbpf);
void	qp_abseinit(int argc, char *argv[], FILE **fpi, FILE **fpo, wavfile_chunk_type *wcti);
#endif
void	qp_rc_decoding(short *lpci, int np, int *nblpc, double *kq);

/* common */
void	qp_acf(double *x, int xlen, int nlags, double *acfs);
void	qp_adaptive_codebook_search(dsswork& w, double *t, double *h, double *e, int *M, short *indexb);
void	qp_fw_imp(double *a, int np, double alpha, double *kw, int lh, double *h);
void	qp_latfilnr(double *x, int B, int M, int N, double *k, double *mem, double *y);
void	qp_latfilr(double *x, int B, int M, int N, double *k, double *mem, double *y);
void	qp_lpc_analysis(dsswork& w, double *s, int frmlen, int np, double *k, double *a, short *lpci);
void	qp_LTP_analysis(dsswork& w, double *r, double *kltp, double *hltp, double *e, int *M, short *indexb);


short	qp_noisegen(short *seed);
double	qp_quantize(double inp, const double *table, int nbits, short *index);
void	qp_rc_quantization(double *a, int np, int *nblpc, short *lpci, double *aq);
int		qp_readspch(dsswork& w, FILE *fpi, int blksz, double *data);
void	qp_schur(dsswork& w, double *acfs, int np, double *k);
void	qp_snr(int f, double *s1, double *s2);
void	qp_stepdown(double *a, int np, double *k);
void	qp_stepup(double *k, int np, double *a);
int		qp_vad(dsswork& w, double *s);
void	window_trapezoid(double *inp, double *outp, int frmlen, int np);
void	qp_wrtspch(dsswork& w, FILE *fpo, int blksz, double *data);

/************************************************************************/

#if 0
void	window_hamming(double *inp, double *outp, int frmlen);
void	window_hanning(double *inp, double *outp, int frmlen);
#endif

}

//#ifdef __cplusplus
//
//}
//#endif
