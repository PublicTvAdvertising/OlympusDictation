/***ABS******************************************************************
 *                                  vad.c
 * -------------------------------------------------------------------- *
 *
 *   vad - voice activity detection.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:                int   vad(double *s);        i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      Given the current frame of input speech, this function
 *      calculates and returns a voice activity decision: 1 means that
 *      voice activity has been detected, 0 means that there is 
 *      probably noise or silence in the current frame.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    synfrmlen_
 *                              fs_
 *                              NoiseLev_
 *                              NoiseCnt_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   H. Takahashi, Olympus, H. Carl, Grundig
 *
 *    creation date:            Nov-09-1995
 *
 *    modification date:        Aug-12-1997
 *
 ************************************************************************
 *
 *    Copyright (C) 1996, 1997, Olympus, Grundig
 *
 ************************************************************************/

#include "sp_Abs.h"

#define  MIN(a,b)  ((a) < (b) ? (a) : (b))

namespace StandardPlay {

int vad(dsswork& w, double *s)
{
int            i;
double         accu;
double         P;
double         Thr;
double         rs[400];

if (w.wk.inv_filt_)
   /* apply inverse filtering with noise adapted coefficients */
   latfilnr(s, w.wk.synfrmlen_*w.wk.fs_, w.wk.synfrmlen_*w.wk.fs_, w.wk.np_, w.wk.Noise_k_, w.wk.vad_mem, rs);
else
   /* just copy the input speech frame */
   for (i=0; i<w.wk.synfrmlen_*w.wk.fs_; i++) rs[i] = s[i];

/* compute the frame energy */
for (accu=0, i=0; i<(int)(w.wk.synfrmlen_*w.wk.fs_); i++) accu += rs[i] * rs[i];
P = accu / (w.wk.synfrmlen_*w.wk.fs_);

/* update noise level estimate */
w.wk.Pn_ *= 1.015625;
if (w.wk.Pn_ > P) w.wk.Pn_ = P;
if (w.wk.Pn_ < w.wk.LowerNoiseLimit_) w.wk.Pn_ = w.wk.LowerNoiseLimit_;
if (w.wk.Pn_ > w.wk.UpperNoiseLimit_) w.wk.Pn_ = w.wk.UpperNoiseLimit_;
if (w.wk.frm_cnt_++ < 50)        /* accelerate noise level estimate setup */
   w.wk.Pmin_ = MIN(w.wk.Pmin_, P);
else
   if (w.wk.no_silence_)
      if (w.wk.Pn_ < w.wk.Pmin_)
         w.wk.Pn_ *= 1.046875;

/* treshold computation */
Thr = 0.00000762939453125 * w.wk.Pn_ * w.wk.Pn_ + 5 * w.wk.Pn_;

/* compare frame energy with threshold */
if (P > Thr)
   w.wk.NoiseCnt_ = 0;
else
   w.wk.NoiseCnt_ ++;

/* check with preceding VadHangCnt_ frames */
if (w.wk.NoiseCnt_ > w.wk.VadHangCnt_)
   {
   w.wk.NoiseCnt_ = w.wk.VadHangCnt_ + 1;  /* overflow prevention for long silence periods */
   return 0;
   }
else
   return 1;
}
}
