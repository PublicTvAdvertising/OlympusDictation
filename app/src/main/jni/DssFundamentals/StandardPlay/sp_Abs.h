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
 *    modification date:        Feb-2-1996
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include <stdio.h>
#include <math.h>
#include <string.h>
#include <sys/types.h>
#include <stdlib.h>


/* --- constants --- */

#define DSHOW_IO		1
#define DATE         __DATE__

#include "../dsswork.h"
#include "../CustomIO.h"

/* --- prototypes ----------------------------------------------------- */



namespace StandardPlay {

void abs_anl_syn(dsswork& w,double *si, double *so);
void abs_anl(dsswork& w, double *si, short *va, short *lpci, short *Mi, short *betai,
                                                  unsigned long *ei, short **ai);
void abs_syn(dsswork& w, short va, short *lpci, short *Mi, short *betai,
                                      unsigned long *ei, short **ai, double *so);
void abs_init(dsswork& w, int argc, char *argv[], FILE **fpi, FILE **fpo);
#ifdef DSHOW_IO
void set_silence_compression(dsswork& w, int on);
int get_silence_compression(dsswork& w);
void absdinit(dsswork& w, int *nbpf);
void abseinit(dsswork& w);
#else
void absdinit(int argc, char *argv[], FILE **fpi, FILE **fpo, int *nbpf);
void abseinit(int argc, char *argv[], FILE **fpi, FILE **fpo);
#endif
void acf(double *x, int xlen, int nlags, double *acfs);
void adaptive_codebook_search(dsswork& w, double *t, double *h,
                                               double *e, int *M, short *indexb);
void excitation_evaluation(dsswork& w, double *r, double *kmpe, double *hmpe,
               double *hhmpe, double *e, unsigned long *indexe, short *indicesa);
void excitation_generation(dsswork& w, int M, short betai, unsigned long ei,
                                                           short *ai, double *e);
void fw_imp(double *a, int np, double alpha, double *kw, int lh, double *h);
unsigned long getbits(dsswork& w, FILE *fpi, int nbits);
void latfilnr(double *x, int B, int M, int N, double *k, double *mem, double *y);
void latfilr(double *x, int B, int M, int N, double *k, double *mem, double *y);
void lpc_analysis(dsswork& w, double *s, int frmlen, int np,
                                              double *k, double *a, short *lpci);
void LTP_analysis(dsswork& w, double *r, double *kltp, double *hltp,
                                               double *e, int *M, short *indexb);
void mp_excitation_evaluation(dsswork& w, double *t, double *h,
               double *hh, double *empe, unsigned long *indexe, short *indicesa);
short noisegen(short *seed);
void putbits(dsswork& w, FILE *fpo, unsigned long bits, int nbits);
double quantize(double inp, const double *table, int nbits, short *index);
void rc_decoding(short *lpci, int np, int *nblpc, double *kq);
void rc_quantization(double *a, int np, int *nblpc, short *lpci, double *aq);
short readcode(dsswork& w, FILE *fpo, short *va, short *lpci, short *Mi, short *betai,
                                                  unsigned long *ei, short **ai);
int readspch(dsswork& w, FILE *fpi, int blksz, double *data);
void schur(dsswork& w, double *acfs, int np, double *k);
void snr(int f, double *s1, double *s2);
void stepdown(double *a, int np, double *k);
void stepup(double *k, int np, double *a);
int vad(dsswork& w, double *s);
void wrtcode(dsswork& w, FILE *fpo, short va, short *lpci, short *Mi, short *betai,
                                                  unsigned long *ei, short **ai);

}

void wrtspch(dsswork& w, FILE *fpo, int blksz, double *data);

//#ifdef __cplusplus
//}
//#endif

/************************************************************************/
