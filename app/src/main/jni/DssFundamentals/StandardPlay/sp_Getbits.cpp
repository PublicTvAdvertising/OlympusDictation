/***ABS******************************************************************
 *                              getbits.c
 * -------------------------------------------------------------------- *
 *
 *   getbits  -  reads arbritary number (<=32) of bits from binary file.
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
 *      The value 'nbits' must not exceed 32.
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
 *    modification date:        Aug-3-1995
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

namespace QualityPlay {
extern __int64_t qp_getbits(dsswork& w, FILE *fpi, int nbits);
}

namespace StandardPlay {

#ifndef STANDARDPLAY_USE_OWN_GETBITS
// Use the same getbits function as in the QP decode section, so CustomIO.cpp can query getbits internal state without 
// specifying which getbits.  The functions are essentially the same, anyway.

unsigned long getbits(dsswork& w, FILE *fpi, int nbits)
{
	return (long)QualityPlay::qp_getbits(w, fpi, nbits);
}
#else
unsigned long getbits(FILE *fpi, int nbits)
{
	static unsigned long		bits_read = 0;
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

	return result;
}
#endif
}
