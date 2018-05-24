/***ABS******************************************************************
 *                              putbits.c
 * -------------------------------------------------------------------- *
 *
 *   putbits  -  appends arbritary number (<=32) of bits to binary file.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void putbits(FILE *fpo,                 i
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
 *      parameter 'bits'. Thus, the value 'nbits' must not exceed 32.
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
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"

//#define CHECK_BYTE_ORDERING 

namespace StandardPlay {

void putbits(dsswork& w, FILE *fpo, unsigned long bits, int nbits)
{
unsigned long           mask;
int                     n;
#ifdef CHECK_BYTE_ORDERING
static unsigned short	wordout = 0x0;
static unsigned short	bytecnt = 0;
#endif

//reset static vars
if (0 == fpo && 0 == bits && 0 == nbits) {
	w.wk.putbits_outtemp = 0x0;
	w.wk.putbits_outbitmask = 0x8000;
	return;
}
if (nbits)
   for (mask=1L<<(nbits-1), n=nbits; n>0; n--, mask>>=1)
   {
	  if (bits & mask) {
		  w.wk.putbits_outtemp |= w.wk.putbits_outbitmask;
	  }
	  w.wk.putbits_outbitmask >>= 1;
#ifdef CHECK_BYTE_ORDERING
	  if (outbitmask == 0x0080 || outbitmask == 0) {
		  wordout <<= 8;
		  wordout |= (bytecnt<<0);
		  outtemp = wordout;
		  ++bytecnt;
		  bytecnt = bytecnt % 41;
	  }
#endif
      if (w.wk.putbits_outbitmask == 0)
         {
         w.wk.putbits_outbitmask = 0x8000;
#ifdef DSHOW_IO
		 IO_Write(w, fpo, (short*)&w.wk.putbits_outtemp, sizeof(short), 1);
#else
         fwrite(&outtemp, sizeof(short), 1, fpo);
#endif
         w.wk.putbits_outtemp = 0;
         }
      }
else
if (w.wk.putbits_outbitmask != 0x8000) {
#ifdef DSHOW_IO
     IO_Write(w, fpo, (short*)&w.wk.putbits_outtemp, sizeof(short), 1);
#else
       fwrite(&outtemp, 1, sizeof(short), fpo);
#endif
}
}
}
