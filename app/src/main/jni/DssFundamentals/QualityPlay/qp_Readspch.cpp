/************************************************************************
 *                              readspch.c
 * -------------------------------------------------------------------- *
 *
 *   qp_readspch  -  reads blocks of speech data from input file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:          int qp_readspch(FILE *fpi,               i
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
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Readspch.c  $
 * Revision 1.7 2005/06/13 11:40:51CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#define BLK 1024

#define _countof(a) (sizeof(a)/sizeof(*(a))) // shaheen
namespace QualityPlay {

int qp_readspch(dsswork& w, FILE *fpi, int blksz, double *data)
{

	short          *x, *xo;
	double         *y, *yo;
	double         *dataptr;
	int            i, smplsleft;
	int            b, k;

	if (w.wk.qp_readspch_first) {
#ifndef DSHOW_IO
		fseek(fpi, hdrlen_, SEEK_SET); 
#endif
		w.wk.qp_readspch_first = 0;
	}
	if (blksz < 0 && data == 0) {
		w.wk.qp_readspch_xold = 0;
		w.wk.qp_readspch_yold = 0;
		w.wk.qp_readspch_y1old = 0;
		w.wk.qp_readspch_endoffile = 0;
	}

	smplsleft = blksz;
	dataptr = data;

	while (!w.wk.qp_readspch_endoffile && smplsleft > 0)	{

		b = smplsleft > _countof(w.wk.qp_readspch_buf) ? _countof(w.wk.qp_readspch_buf) : smplsleft;
#ifdef DSHOW_IO
		if ((k = IO_Read(w, fpi, w.wk.qp_readspch_buf, sizeof(short), b)) < b)
#else
		if ((k=fread(buf,sizeof(short),b,fpi)) < b) 
#endif
			w.wk.qp_readspch_endoffile = 1;

		smplsleft -= k;
		x = xo = w.wk.qp_readspch_buf;
		y = yo = w.wk.qp_readspch_buf1;

		if (w.wk.notchfilter_) {
			*y++ = (double)*x++ - (double)w.wk.qp_readspch_xold + w.wk.qp_readspch_alpha * w.wk.qp_readspch_yold;
			for (i=1; i<k; i++)
				*y++ = (double)*x++ - (double)*xo++ + w.wk.qp_readspch_alpha * *yo++;
			w.wk.qp_readspch_xold = *xo; w.wk.qp_readspch_yold = *yo;
		}
		else
			for (i=0; i<k; i++) 
				*y++ = (double)*x++;

		y = yo = w.wk.qp_readspch_buf1;

		if (w.wk.emphasiscoeff_ != 0.) {
			*dataptr++ = *y++ - w.wk.emphasiscoeff_ * w.wk.qp_readspch_y1old;
			for (i=1; i<k; i++)
				*dataptr++ = *y++ - w.wk.emphasiscoeff_ * *yo++;
			w.wk.qp_readspch_y1old = *yo;
		}
		else
			for (i=0; i<k; i++) 
				*dataptr++ = *y++;
	}
	for (i=1; i<smplsleft; i++) 
		*dataptr++ = 0.;

	return(blksz - smplsleft);
}
}