/***ABS******************************************************************
 *                              getbits.c
 * -------------------------------------------------------------------- *
 *
 *   getbits  -  reads arbritary number (<=64) of bits from binary file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:     unsigned long getbits(FILE *fpi,           i
 *                                         int nbits)           i
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      The input file has to be opened elsewhere and only the file
 *      pointer is passed to this routine. Data is read from the
 *      input file bit-wise: 'nbits' bits are returned right justified.
 *      The value 'nbits' must not exceed 64.
 *
 *      This function buffers read bits in a static variable until
 *      another full 16-bit portion has to be read from file.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, Grundig
 *
 *    creation date:            Jan-10-1995
 *
 *    modification date:        Aug-03-1995
 *                              Okt-15-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Getbits.c  $
 * Revision 1.5 2005/06/13 11:40:18CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

__int64_t qp_getbits(dsswork& w, FILE *fpi, int nbits)
{
	__int64_t                 mask;
	__int64_t                 result;
	int                     n;

	// start bit counting
	if (nbits == -2) {
		w.wk.qp_getbits_bits_read = 0;
		w.wk.qp_getbits_bits_from_stream = 0;
		return 1;
	}
	// get bits read
	if (nbits == -3) {
		return w.wk.qp_getbits_bits_read;
	}
	// is eof?
	if (nbits == -4) {
		return w.wk.qp_getbits_eof;
	}
	// get bits stored count
	if (nbits == -101) {
		long remain = 0;
		__int64_t maskcopy = w.wk.qp_getbits_inbitmask;
		while ((maskcopy >>=1) != 0) {
			++remain;
		}
		return remain;
	}
	// flush bits
	if (nbits < 0) {
		w.wk.qp_getbits_inbitmask = 0;
		w.wk.qp_getbits_intemp = 0;
		w.wk.qp_getbits_eof = 0;
		return 0;
	}
	for (result=0, mask=((__int64_t)1)<<(nbits-1), n=nbits; n>0; n--, mask>>=1) {
		
		if ((w.wk.qp_getbits_inbitmask>>=1) == 0) {
			int last_read_bits;
			w.wk.qp_getbits_inbitmask = 0x8000;
			last_read_bits = 8*IO_Read(w, fpi, (short*)&w.wk.qp_getbits_intemp, sizeof(short), 1);
			if (last_read_bits) {
				w.wk.qp_getbits_eof = 0;
			}
			w.wk.qp_getbits_bits_from_stream += last_read_bits;
		}

		if (w.wk.qp_getbits_intemp & w.wk.qp_getbits_inbitmask) 
			result |= mask;
	}
	w.wk.qp_getbits_bits_read += nbits;
	if (w.wk.qp_getbits_bits_read == w.wk.qp_getbits_bits_from_stream) {
		w.wk.qp_getbits_eof = 1;
	}

	return result;
}
/*	static unsigned long		bits_read = 0;
	static unsigned long		bits_from_stream = 0;
static unsigned short   intemp = 0x0;
static unsigned short   inbitmask = 0x0;
static int				eof = 0;
unsigned long           mask;
unsigned long           result;
int                     n;

// start bit counting
if (nbits == -2) {
	bits_read = 0;
	bits_from_stream = 0;
	return 1;
}
// get bits read
if (nbits == -3) {
	return bits_read;
}
// is eof?
if (nbits == -4) {
	return eof;
}
// flush bits
if (nbits < 0) {
	inbitmask = 0;
	intemp = 0;
	eof = 0;
	return 0;
}

	for (result=0, mask=1L<<(nbits-1), n=nbits; n>0; n--, mask>>=1)
	{
		if ((inbitmask>>=1) == 0)
		{
			int last_read_bits;
			inbitmask = 0x8000;
			last_read_bits = 8*IO_Read(fpi, &intemp, sizeof(short), 1);
			if (last_read_bits) {
				eof = 0;
			}
			bits_from_stream += last_read_bits;
			//fread(&intemp, sizeof(short), 1, fpi);
		}
		if (intemp & inbitmask) {
			result |= mask;
		}
	}

	bits_read += nbits;
	if (bits_read == bits_from_stream) {
		eof = 1;
	}

	return result;*/
}