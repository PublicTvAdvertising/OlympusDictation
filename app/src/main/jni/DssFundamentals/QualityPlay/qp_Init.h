/************************************************************************
 *                                 init.h
 *
 ************************************************************************
 * $Log: Init.h  $
 * Revision 1.4 2005/06/13 11:49:05CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_ipkclib.h"

#define LD(a)     (log((double)a)/log((double)2.))

namespace QualityPlay {

static unsigned int antipow(unsigned int inpow)
{
	unsigned int i=0;
	while(inpow > 1) { 
		i++; 
		inpow /=2;
	}
	return(i);
}

static double nchoosek(int n, int k)
{ //  return ( (n! - k!) / k!)
	double i=1, j=1;
	for (; k>0; i*=n--, j*=k--);
	return i/j;
}

static short IsNotWavFileName(char *FileName)
{
	char *charptr;

	if ((charptr=strrchr(FileName, '.')) == NULL)
		return(1);
	else {
		if(	charptr[1] != 'w' && charptr[1] != 'W' ||
			charptr[2] != 'a' && charptr[2] != 'A' ||
			charptr[3] != 'v' && charptr[3] != 'V' ||
			charptr[4] != '\0')	{
			return(1);
		}
		else {
			return(0);
		}
	}
}

static short IsNotEDssFileName(char *FileName)
{
	char *charptr;

	if ((charptr=strrchr(FileName, '.')) == NULL)
		return(1);
	else {
		if(	charptr[1] != 'e' && charptr[1] != 'E' ||
			charptr[2] != 'd' && charptr[1] != 'D' ||
			charptr[3] != 's' && charptr[2] != 'S' ||
			charptr[4] != 's' && charptr[3] != 'S' ||
			charptr[5] != '\0')	{
			return(1);
		}
		else {
			return(0);
		}
	}
}

}