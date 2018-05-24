/***ABS******************************************************************
 *                               initenc.c
 * -------------------------------------------------------------------- *
 *
 *   abseinit  -  initialization of absenc program.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void abseinit(int argc,                i
 *                                    char *argv[],            i
 *                                    FILE **fpi,              o
 *                                    FILE **fpo);             o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This procedure opens the files needed in the absenc program
 *      and initializes the global variables (marked by '_'
 *      terminating their names) by assigning the values read from
 *      the parameter input file.
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Jun-29-1991
 *
 *    modification date:        Feb-2-1996
 *
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "sp_Abs.h"
#include "sp_Setpar.h"

#define LD(a)     (log((double)a)/log((double)2.))

namespace StandardPlay {

static void usage(void)
{
printf("\nabsenc - Analysis-by-Synthesis Encoder Simulation      GRUNDIG      %s\n", DATE);
printf("\nusage: absenc infile outfile [-s]\n\n");
exit(0);
}

static double nchoosek(int n, int k)
{
double i=1, j=1;
for (; k>0; i*=n--, j*=k--);
return i/j;
}

#ifdef DSHOW_IO
void abseinit(dsswork& w)
#else
void abseinit(int argc, char *argv[], FILE **fpi, FILE **fpo)
#endif
{
int            i, j;
long           nbpf, nbpsf;
double         aux;

#ifdef DSHOW_IO
	// scon_ is set by accessor
#else
char           infilename[256];
char           outfilename[256];

/* --- evaluate command line --- */
scon_ = 0;
if (argc == 3)
   {
   strcpy(infilename, argv[1]);
   strcpy(outfilename, argv[2]);
   }
else if (argc == 4)
   {
   if (!strcmp(argv[1], "-s") || !strcmp(argv[1], "-S") ||
       !strcmp(argv[1], "/s") || !strcmp(argv[1], "/S"))
      {
      scon_ = 1;
      strcpy(infilename, argv[2]);
      strcpy(outfilename, argv[3]);
      }
   else
      {
      strcpy(infilename, argv[1]);
      strcpy(outfilename, argv[2]);
      if (!strcmp(argv[3], "-s") || !strcmp(argv[3], "-S") ||
          !strcmp(argv[3], "/s") || !strcmp(argv[3], "/S"))
         scon_ = 1;
      else
         usage();
      }
   }
else
   usage();

/* --- open files --- */
{
if ((*fpi=fopen(infilename, "rb")) == NULL)
   {
   printf("\nabseinit: error opening input speech file %s!\n\n", infilename);
   exit(1);
   }
if ((*fpo=fopen(outfilename, "wb")) == NULL)
   {
   printf("\nabseinit: error opening output speech file %s!\n\n", outfilename);
   exit(1);
   }
#endif
/* --- set parameters --- */
setpar(w);

/* compute bit rate */
for (nbpf=0, i=0; i<w.wk.np_; i++) nbpf += w.wk.nblpc_[i];
if (w.wk.scon_) nbpf++;
for (nbpsf=nbpf, i=0; i<(int)(w.wk.synfrmlen_/w.wk.excveclen_+.5); i++) nbpsf += w.wk.nbgn_;
for (aux=w.wk.maxlag_-w.wk.minlag_+1, i=1; i<(int)(w.wk.synfrmlen_/w.wk.excveclen_+.5); i++)
   aux *= w.wk.difflagrange_;
nbpf += (long)ceil(LD(aux));
nbpf += (long)(w.wk.synfrmlen_/w.wk.excveclen_+.5) * w.wk.nbga_;
j = w.wk.nbgm_;
j += w.wk.nimps_ * w.wk.nbp_;
j += (int)ceil(LD(nchoosek((int)(w.wk.fs_*w.wk.excveclen_), w.wk.nimps_)));
for (i=0; i<(int)(w.wk.synfrmlen_/w.wk.excveclen_+.5); i++) nbpf += j;
#ifndef DSHOW_IO
if (scon_)
   printf("absenc (%ld/%ldbps) - %s\n       ", (long)(nbpf*1000./synfrmlen_), 
                                 (long)(nbpsf*1000./synfrmlen_), infilename);
else
   printf("absenc (%ldbps)  -  %s\n       ", (long)(nbpf*1000./synfrmlen_), infilename);

fflush(stdout);
}
#endif
}
}
