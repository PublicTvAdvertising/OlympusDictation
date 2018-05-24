/***ABS******************************************************************
 *                              wrtspch.c
 * -------------------------------------------------------------------- *
 *
 *   wrtspch  -  writes blocks of speech data to output file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void wrtspch(FILE *fpo,                 i
 *                                   int blksz,                 i
 *                                   double *data);             i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The output file has to be opened elsewhere and only the file
 *      pointer is passed to this routine. Data is written to the
 *      output file as short integer.
 *
 *      Before writing the output speech data to file, in can be
 *      deemphasised by using the standard deemphasis equation
 *
 *          y(n) = data(n) + beta * y(n-1)
 *
 *                                                 0 < beta < 1  .
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    emphasiscoeff_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            May-19-1994
 *
 *    modification date:        Jan-11-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"


void wrtspch(dsswork& w, FILE *fpo, int blksz, double *data)
{

double         *x, *y, *yo;
int            i, b, smplsleft;

smplsleft = blksz;
x = data;
do
   {
   b = smplsleft > wrtspch_BLK ? wrtspch_BLK : smplsleft;
   y = yo = w.wk.wrtspch_buf;
   if (w.wk.emphasiscoeff_ != 0.)
      {
      *y++ = *x++ + w.wk.emphasiscoeff_ * w.wk.wrtspch_yold;
      for (i=1; i<b; i++)
         *y++ = *x++ + w.wk.emphasiscoeff_ * *yo++;
      w.wk.wrtspch_yold = *yo;
      }
   else
      for (i=0; i<b; i++) *y++ = *x++;
   y = w.wk.wrtspch_buf;
   for (i=0; i<b; i++)
      {
      if (*y > 32767.) w.wk.wrtspch_obuf[i] = 32767;
      else if (*y < -32767.) w.wk.wrtspch_obuf[i] = -32767;
      else w.wk.wrtspch_obuf[i] = (short) floor(*y + 0.5);
      y++;
      }
   if (IO_Write(w, fpo, w.wk.wrtspch_obuf, sizeof(short), b) != (size_t)b)
      {
      printf("wrtspch: error writing output file!\n");
      exit(1);
      }
   smplsleft -= b;
   }
while (smplsleft > 0);
#ifndef DSHOW_IO
if (fflush(fpo) != 0)
   {
   printf("wrtspch: error writing output file!\n");
   exit(1);
   }
#endif
}
