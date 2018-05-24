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
 *                              Jun-12-2003 by Michael Kroener
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Wrtspch.c  $
 * Revision 1.7 2005/06/13 11:41:08CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_wrtspch(dsswork& w, FILE *fpo, int blksz, double *data)
{

	double         *x, *y, *yo;
	int            i, b, smplsleft;

	smplsleft = blksz;
	x = data;
	do {
		b = smplsleft > qp_wrtspch_BLK ? qp_wrtspch_BLK : smplsleft;
		y = yo = w.wk.qp_wrtspch_buf;

		if (w.wk.emphasiscoeff_ != 0.) {
			*y++ = *x++ + w.wk.emphasiscoeff_ * w.wk.qp_wrtspch_yold;
			for (i=1; i<b; i++)
				*y++ = *x++ + w.wk.emphasiscoeff_ * *yo++;
			w.wk.qp_wrtspch_yold = *yo;
		}
		else
			for (i=0; i<b; i++) 
				*y++ = *x++;

		y = w.wk.qp_wrtspch_buf;

		for (i=0; i<b; i++) {

			if (*y > 32767.) {
				w.wk.qp_wrtspch_obuf[i] = 32767;
			}
			else if (*y < -32767.) {
				w.wk.qp_wrtspch_obuf[i] = -32767;
			}
			else {
				w.wk.qp_wrtspch_obuf[i] = (short) floor(*y + 0.5);
			}

			y++;
		}

		if (IO_Write(w, fpo, w.wk.qp_wrtspch_obuf, sizeof(short), b) != (size_t)b) {
//		if (fwrite(obuf,sizeof(short),b,fpo) != (size_t)b) {
//			fprintf(ERROROUT, "\nwrtspch: error writing output file!\n\n");
//			exit(1);
		}
		smplsleft -= b;
	}
	while (smplsleft > 0);

//	if (fflush(fpo) != 0) {
//		fprintf(ERROROUT, "\nwrtspch: error writing output file!\n\n");
//		exit(1);
//	}
}
}