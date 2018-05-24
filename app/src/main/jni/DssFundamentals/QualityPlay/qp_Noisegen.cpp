/***ABS******************************************************************
 *                                  noisegen.c
 * -------------------------------------------------------------------- *
 *
 *   qp_noisegen  -  random number generator.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:         short qp_noisegen(short *seed);         io
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine generates random numbers which are approximately 
 *      uniformly distributed in the range -32768 .. 32767.
 *      An external short variable '*seed' has to be provided.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Mar-7-1996
 *
 *    modification date:        Aug-12-1997
 *
 ************************************************************************
 * $Log: Noisegen.c  $
 * Revision 1.3 2005/06/13 10:04:56CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1996 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

namespace QualityPlay {

short qp_noisegen(short *seed)
{
	long      temp;

	temp = (long) *seed;
	temp &= 0x0000ffffL;
	temp = temp * 521L + 259L;
	*seed = (short) temp;

	return (short) temp;
}
}