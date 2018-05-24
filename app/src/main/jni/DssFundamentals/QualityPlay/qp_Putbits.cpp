/***ABS******************************************************************
 *                              putbits.c
 * -------------------------------------------------------------------- *
 *
 *   qp_putbits  -  appends arbritary number (<=64) of bits to binary file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void qp_putbits(FILE *fpo,                 i
 *                                   unsigned long bits,        i
 *                                   int nbits)                 i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The output file has to be opened elsewhere and only the file
 *      pointer is passed to this routine. Data is written to the
 *      output file bit-wise: 'nbits' bits given in the unsigned long
 *      parameter 'bits'. Thus, the value 'nbits' must not exceed 64.
 *
 *      This function stores bits in a static variable until a full
 *      16-bit portion can be written to file. If the submitted number
 *      of bits 'nbits' is zero, the static variable is filled up
 *      with zeros and is written to file, which should be done
 *      before closing the file in order not to lose the last (up to
 *      15) bits passed to this routine.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1996
 *
 *    modification date:        Jan-10-1996
 *                              Okt-15-2003 by Michael Kroener, 64 bit
 *
 ************************************************************************
 * $Log: Putbits.c  $
 * Revision 1.7 2005/06/13 11:40:29CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

void qp_putbits(dsswork& w, FILE *fpo, __int64_t bits, int nbits)
{
	__int64_t					mask, ll=1;
	int						n;
#ifdef CHECK_BYTE_ORDERING
	static unsigned short	wordout = 0x0;
	static unsigned short	bytecnt = 0;
#endif
	//reset static vars
	if (0 == fpo && 0 == bits && 0 == nbits) {
		w.wk.qp_putbits_outtemp = 0x0;
		w.wk.qp_putbits_outbitmask = 0x8000;
		ll = 1;
		return;
	}
	if (nbits) {
		for (mask = ((__int64_t)1)<<(nbits-1), n=nbits; n>0; n--, mask>>=1) {
			if (bits & mask) 
				w.wk.qp_putbits_outtemp |= w.wk.qp_putbits_outbitmask;

			if ((w.wk.qp_putbits_outbitmask>>=1) == 0) {
				w.wk.qp_putbits_outbitmask = 0x8000;
#ifdef DSHOW_IO
				IO_Write(w, fpo, (short*)&w.wk.qp_putbits_outtemp, sizeof(short), 1);
#else
				fwrite(&w.wk.qp_putbits_outtemp, sizeof(short), 1, fpo);
#endif				
				w.wk.qp_putbits_outtemp = 0;
			}
		}
	}
	else
		if (w.wk.qp_putbits_outbitmask != 0x8000) {
#ifdef DSHOW_IO
			IO_Write(w, fpo, (short*)&w.wk.qp_putbits_outtemp, sizeof(short), 1);
#else
			fwrite(&w.wk.qp_putbits_outtemp, 1, sizeof(short), fpo);
#endif
	}
}
}