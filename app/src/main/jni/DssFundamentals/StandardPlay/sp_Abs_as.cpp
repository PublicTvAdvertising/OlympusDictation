/***ABS******************************************************************
 *                                 abs_as.c
 * -------------------------------------------------------------------- *
 *
 *   abs_anl_syn  -  analysis and synthesis.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void abs_anl_syn(double *s,            i
 *                                       double *shat,);       o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine processes one (analysis) frame of the input
 *      signal s(n) according to the analysis-by-synthesis coding
 *      principle and returns the resynthesized signal shat(n).
 *
 * -------------------------------------------------------------------- *
 *
 *    calls routines:           lpc_analysis
 *                              fw_imp
 *                              acf
 *                              latfilnr
 *                              LTP_analysis
 *                              excitation_evaluation
 *                              latfilr
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    np_
 *                              fs_
 *                              excveclen_
 *                              synfrmlen_
 *                              anlfrmlen_
 *                              lha_
 *                              lhm_
 *                              alphaa_
 *                              alpham_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Nov-8-1993
 *
 *    modification date:        Aug-3-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"
#include "sp_pow2.h"

namespace StandardPlay {

void abs_anl_syn(dsswork& w, double *s, double *shat)
{
static const double     gnqtbl[] =
#include "sp_Gnqtbld.h"

double            *k;
double            *a;
short             *lpci;
double            *kltp;
double            *hltp;
double            *kmpe;
double            *hmpe;
double            *hhmpe;
double            *r;
int               i, n;
int               va;
double            en;
double            aux;
double            min;
int               M;
short             betai;
unsigned long     indexe;
short             indicesa[9];
double            *sptr;

/* --- initialize --- */
if (!w.wk.abs_anl_syn_init)
   {
   w.wk.abs_anl_syn_A = w.wk.anlfrmlen_*w.wk.fs_;
   w.wk.abs_anl_syn_S = w.wk.synfrmlen_*w.wk.fs_;
   w.wk.abs_anl_syn_N = (int)(w.wk.excveclen_*w.wk.fs_);
   w.wk.abs_anl_syn_memb_if = (double *)calloc(w.wk.np_, sizeof(double));
   w.wk.abs_anl_syn_mema_sf = (double *)calloc(w.wk.np_, sizeof(double));
   w.wk.abs_anl_syn_e = (double *)calloc(w.wk.abs_anl_syn_N, sizeof(double));
   w.wk.abs_anl_syn_esave = (double *)calloc(w.wk.abs_anl_syn_N, sizeof(double));
   if (w.wk.abs_anl_syn_memb_if==NULL || w.wk.abs_anl_syn_mema_sf==NULL || w.wk.abs_anl_syn_e==NULL || w.wk.abs_anl_syn_esave==NULL)
      {
      printf("\nabs_anl_syn init: memory allocation error!\n\n");
      exit(1);
      }
   w.wk.abs_anl_syn_init = 1;
   }

	if (0 == s && 0 == shat) {
		free(w.wk.abs_anl_syn_memb_if);	w.wk.abs_anl_syn_memb_if = 0;
		free(w.wk.abs_anl_syn_mema_sf);	w.wk.abs_anl_syn_mema_sf = 0;
		free(w.wk.abs_anl_syn_e);		w.wk.abs_anl_syn_e = 0;
		free(w.wk.abs_anl_syn_esave);	w.wk.abs_anl_syn_esave = 0;
		w.wk.abs_anl_syn_init = 0;
		return;
	}

/* --- allocate memory --- */
k = (double *)calloc(w.wk.np_, sizeof(double));
a = (double *)calloc(w.wk.np_+1, sizeof(double));
lpci = (short *)calloc(w.wk.np_, sizeof(short));
kltp = (double *)calloc(w.wk.np_, sizeof(double));
hltp = (double *)calloc((short)(w.wk.lha_*w.wk.fs_), sizeof(double));
kmpe = (double *)calloc(w.wk.np_, sizeof(double));
hmpe = (double *)calloc((short)(w.wk.lhm_*w.wk.fs_), sizeof(double));
hhmpe = (double *)calloc(w.wk.abs_anl_syn_N, sizeof(double));
r = (double *)calloc(w.wk.abs_anl_syn_N, sizeof(double));
if (k==NULL || a==NULL || lpci==NULL || kltp==NULL || hltp==NULL ||
                          kmpe==NULL || hmpe==NULL || hhmpe==NULL || r==NULL)
   {
   printf("\nabs_anl_syn: memory allocation error!\n\n");
   exit(1);
   }

/* synthesis frame pointer in input buffer */
sptr = s + w.wk.abs_anl_syn_A/2 - w.wk.abs_anl_syn_S/2;

/* perform VAD */
if (w.wk.scon_)
   va = vad(w, sptr);
else
   va = 1;

/* --- LPC analysis --- */
lpc_analysis(w, s, w.wk.abs_anl_syn_A, w.wk.np_, k, a, lpci);

/* --- precomputations --- */
if (va)
   {
   fw_imp(a, w.wk.np_, w.wk.alphaa_, kltp, (int)(w.wk.lha_*w.wk.fs_), hltp);
   fw_imp(a, w.wk.np_, w.wk.alpham_, kmpe, (int)(w.wk.lhm_*w.wk.fs_), hmpe);
   acf(hmpe, (int)(w.wk.lhm_*w.wk.fs_), w.wk.abs_anl_syn_N, hhmpe);
   for (i=(int)(w.wk.lhm_*w.wk.fs_); i<w.wk.abs_anl_syn_N; i++) hhmpe[i] = 0;
   }
else
   {
   if (w.wk.inv_filt_)   /* update the noise adapted LPC coefficients */
      for (i=0; i<w.wk.np_; i++)
         w.wk.Noise_k_[i] = w.wk.Noise_k_[i] * 0.9375 + k[i] * 0.0625;
   w.wk.reset_memories_ = 1;   /* for next transition to VA = 1 */
   }

/* --- loop over subframes --- */
for (i=0; i<w.wk.abs_anl_syn_S/w.wk.abs_anl_syn_N; i++)
   {
   /* --- compute LPC residual --- */
   latfilnr(sptr+i*w.wk.abs_anl_syn_N, w.wk.abs_anl_syn_N, w.wk.abs_anl_syn_N, w.wk.np_, k, w.wk.abs_anl_syn_memb_if, r);

   if (va)
      {
      /* --- restore last VA excitation vector for correct adaptive codebook update --- */
      if (w.wk.reset_memories_)
         for (n=0; n<w.wk.abs_anl_syn_N; n++) w.wk.abs_anl_syn_e[n] = w.wk.abs_anl_syn_esave[n];

      /* --- LTP analysis --- */
      if (i==0) M = 0;
      LTP_analysis(w, r, kltp, hltp, w.wk.abs_anl_syn_e, &M, &betai);

      /* --- determine optimal excitation for LPC synthesis filter --- */
      excitation_evaluation(w, r, kmpe, hmpe, hhmpe, w.wk.abs_anl_syn_e, &indexe, indicesa);
      }
   else
      {
      /* --- calculate residual energy --- */
      for (en=0,n=0; n<w.wk.abs_anl_syn_N; n++) en += r[n] * r[n];

      /* --- adjust noise level by choosing best quantized gain value --- */
      aux = en / (2.*w.wk.abs_anl_syn_N*32768.*32768.);  /* normalized power - 3dB */
      min = 1.e200;
      for (n=0; n<pow2[w.wk.nbgn_]; n++)
         if (fabs(gnqtbl[n]*gnqtbl[n]/3. - aux) < min)
            {min = fabs(gnqtbl[n]*gnqtbl[n]/3. - aux); indicesa[0] = n;}

      /* --- construct resulting noise excitation vector --- */
      for (n=0; n<w.wk.abs_anl_syn_N; n++)
         w.wk.abs_anl_syn_e[n] = gnqtbl[indicesa[0]] * (double)noisegen(&w.wk.seed_);
      }  

   /* --- synthesis filtering --- */
   latfilr(w.wk.abs_anl_syn_e, w.wk.abs_anl_syn_N, w.wk.abs_anl_syn_N, w.wk.np_, k, w.wk.abs_anl_syn_mema_sf, shat+i*w.wk.abs_anl_syn_N);
   }

/* --- save VA excitation vector --- */
if (va) for (n=0; n<w.wk.abs_anl_syn_N; n++) w.wk.abs_anl_syn_esave[n] = w.wk.abs_anl_syn_e[n];

free(k);
free(a);
free(lpci);
free(kltp);
free(hltp);
free(kmpe);
free(hmpe);
free(hhmpe);
free(r);
}
}
