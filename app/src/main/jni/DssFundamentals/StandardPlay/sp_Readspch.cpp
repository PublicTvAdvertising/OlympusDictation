/************************************************************************
 *                              readspch.c
 * -------------------------------------------------------------------- *
 *
 *   readspch  -  reads blocks of speech data from input file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:          int readspch(FILE *fpi,               i
 *                                     int blksz,               i
 *                                     double *data);           o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The input file has to be opened elsewhere and only the file
 *      pointer is passed to this routine. Data in the input file is
 *      expected to be in short integer format and is converted to
 *      double. The function returns the number of samples read from
 *      the input file before encountering the end-of-file marker.
 *      The remaining values up to the number blksz are filled up
 *      with zeros.
 *
 *      Optionally, the input data read from file can be notch filtered
 *      in order to eliminate a possible DC component. The difference
 *      equation of the notch filter is
 *
 *          y(n) = x(n) - x(n-1) + alpha * y(n-1)
 *
 *                                 alpha = 1.0 - e  (e << 1.0)  ,
 *
 *      where x(n) denotes the input data and y(n) is the filtered
 *      sequence passed to the calling routine.
 *
 *      Furthermore, the input data can be preemphasised by using
 *      the standard preemphasis equation
 *
 *          y1(n) = y(n) - beta * y(n-1)
 *
 *                                                 0 < beta < 1  .
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    hdrlen_
 *                              notchfilter_
 *                              emphasiscoeff_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Oct-17-1990
 *
 *    modification date:        Feb-14-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

namespace StandardPlay {

int readspch(dsswork& w, FILE *fpi, int blksz, double *data)
{

short          *x, *xo;
double         *y, *yo;
double         *dataptr;
int            i, smplsleft;
int            b, k;

if (w.wk.readspch_first) {
#ifndef DSHOW_IO
		fseek(fpi, hdrlen_, SEEK_SET); 
#endif
		w.wk.readspch_first = 0;
}
if (blksz < 0 && data == 0) {
	w.wk.readspch_xold = 0;
	w.wk.readspch_yold = 0;
	w.wk.readspch_y1old = 0;
	w.wk.readspch_endoffile = 0;
}

smplsleft = blksz;
dataptr = data;
while (!w.wk.readspch_endoffile && smplsleft > 0)
   {
   b = smplsleft > readspch_BLK ? readspch_BLK : smplsleft;
   if ((k=IO_Read(w, fpi, w.wk.readspch_buf,sizeof(short),b)) < b) 
	   w.wk.readspch_endoffile = 1;
   smplsleft -= k;
   x = xo = w.wk.readspch_buf;
   y = yo = w.wk.readspch_buf1;
   if (w.wk.notchfilter_)
      {
      *y++ = (double)*x++ - (double)w.wk.readspch_xold + w.wk.readspch_alpha * w.wk.readspch_yold;
      for (i=1; i<k; i++)
         *y++ = (double)*x++ - (double)*xo++ + w.wk.readspch_alpha * *yo++;
      w.wk.readspch_xold = *xo; w.wk.readspch_yold = *yo;
      }
   else
      for (i=0; i<k; i++) *y++ = (double)*x++;
   y = yo = w.wk.readspch_buf1;
   if (w.wk.emphasiscoeff_ != 0.)
      {
      *dataptr++ = *y++ - w.wk.emphasiscoeff_ * w.wk.readspch_y1old;
      for (i=1; i<k; i++)
         *dataptr++ = *y++ - w.wk.emphasiscoeff_ * *yo++;
      w.wk.readspch_y1old = *yo;
      }
   else
      for (i=0; i<k; i++) *dataptr++ = *y++;
   }
for (i=1; i<smplsleft; i++) *dataptr++ = 0.;
return(blksz - smplsleft);
}
}
