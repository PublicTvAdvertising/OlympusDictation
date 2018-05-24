/***ABS******************************************************************
 *                                init.c
 * -------------------------------------------------------------------- *
 *
 *   abs_init  -  initialization of abs program.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void abs_init(int argc,                i
 *                                    char *argv[],            i
 *                                    FILE **fpi,              o
 *                                    FILE **fpo);             o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This procedure opens the files needed in the abs program
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
printf("\nabs - Analysis-by-Synthesis Coder Simulation        GRUNDIG        %s\n", DATE);
printf("\nusage: abs infile outfile [-s]\n\n");
exit(0);
}

static double nchoosek(int n, int k)
{
double i=1, j=1;
for (; k>0; i*=n--, j*=k--);
return i/j;
}

void abs_init(dsswork& w, int argc, char *argv[], FILE **fpi, FILE **fpo)
{
int            i, j;
long           nbpf, nbpsf;
double         aux;
char           infilename[256];
char           outfilename[256];

/* --- evaluate command line --- */
w.wk.scon_ = 0;
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
      w.wk.scon_ = 1;
      strcpy(infilename, argv[2]);
      strcpy(outfilename, argv[3]);
      }
   else
      {
      strcpy(infilename, argv[1]);
      strcpy(outfilename, argv[2]);
      if (!strcmp(argv[3], "-s") || !strcmp(argv[3], "-S") ||
          !strcmp(argv[3], "/s") || !strcmp(argv[3], "/S"))
         w.wk.scon_ = 1;
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
   printf("\nabs_init: error opening input speech file %s!\n\n", infilename);
   exit(1);
   }
if ((*fpo=fopen(outfilename, "wb")) == NULL)
   {
   printf("\nabs_init: error opening output speech file %s!\n\n", outfilename);
   exit(1);
   }

/* --- set parameters --- */
setpar(w);

/* compute bit rate if fully quantized version is to be simulated */
if (w.wk.lpcquant_ && w.wk.ltpquant_ && w.wk.ampquant_)
   {
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
   if (w.wk.scon_)
      printf("abs (%ld/%ldbps) - %s \n    ", (long)(nbpf*1000./w.wk.synfrmlen_), 
                                    (long)(nbpsf*1000./w.wk.synfrmlen_), infilename);
   else
      printf("abs (%ldbps)  -  %s \n    ", (long)(nbpf*1000./w.wk.synfrmlen_), infilename);
   }
else
   printf("abs  -  %s \n    ", infilename);
fflush(stdout);
}
}
}
