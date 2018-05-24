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
 *    modification date:        Feb-02-1996
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Initenc.c  $
 * Revision 1.10 2005/06/13 11:40:19CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"
#include "qp_Init.h"
#include "qp_Setpar.h"

namespace QualityPlay {

#ifdef DSHOW_IO
void qp_abseinit(dsswork& w) {
#else
void qp_abseinit(int argc, char *argv[], FILE **fpi, FILE **fpo, wavfile_chunk_type *wcti) {
	char	*appname;
	char	*InFileName;
	char	*OutFileName;
#endif
	int		i, j;
	long	nbpf, nbpsf;
	double	aux;

	#ifdef SHOW_PARAMETER
		int	prarg = 0;
	#endif

#ifndef DSHOW_IO
/* --- set table for command line evaluation --- */   
	icsiargtab_t args[] =
	{
		{ 0, "ABSENC - Analysis-by-Synthesis Encoder Simulation \n  "DATE" - GRUNDIG BUSINESS SYSTEMS GmbH,\n" 
		"\n  usage: Encode.exe infile=<infile> outfile=<outfile> [options]\n", ARG_DESC},
		{ "infile",        "INPUT filename *.wav",                 ARG_STR,    &InFileName,     ARG_REQ},
		{ "outfile",       "OUTPUT file name *.edss",              ARG_STR,    &OutFileName,    ARG_REQ},
		{ "scon",          "silence compression",                  ARG_BOOL,   &scon_,          ARG_OPT},
		#ifdef SHOW_PARAMETER
		{ "p",             "Show all arguments on execution",      ARG_BOOL,   &prarg,          ARG_OPT},
		#endif
		{0,0,0}
	};
#endif
	/* --- set parameters --- */
	setpar(w);

#ifndef DSHOW_IO
	/* ---- evaluate command line ----- */
	if (icsiargs(args,&argc,&argv,&appname)<0) { 
		exit(1);
	}

	fprintf(stdout, "\nABSENC Analysis-by-Synthesis Encoder\n\n");


	/* --- check wav file name --- */
	if(IsNotWavFileName(InFileName)) {
		fprintf(ERROROUT, "\nabseinit: infile \"%s\": wav extention required!\n\n", InFileName);
		exit(1);
	}

	if(IsNotEDssFileName(OutFileName)) {
		fprintf(ERROROUT, "\nabseinit: outfile \"%s\": edss extention required!\n\n", OutFileName);
		exit(1);
	}

	/* --- open files --- */
	if ((*fpi=fopen(InFileName, "rb")) == NULL)	{
		fprintf(ERROROUT, "\nabseinit: infile \"%s\": error opening file!\n\n", InFileName);
		exit(1);
	}

	/* --- read wav input file --- */
	strcpy(wcti->WaveFileName, InFileName);
	if (read_wave_file(*fpi,wcti) != 0) 
		exit(1); // error message created by read_wave_file()

	if((wcti->format_chk.sample_fq != 16000)) {
		fprintf(ERROROUT, "\nabseinit: infile \"%s\": sampling frequency must be 16 kHz!\n\n", wcti->WaveFileName); 
		exit(1);
	}
	else {
		fs_=16;
	}

	if ((*fpo=fopen(OutFileName, "wb")) == NULL) {
		fprintf(ERROROUT, "\nabseinit: outfile \"%s\": error creating file!\n\n", OutFileName); 
		exit(1);
	}
#else
	w.wk.fs_=16;
#endif
/* -------------------------------------------- */
/* --- changed by Michael Kroener 2003.10.09 -- */
/* --- this code was originally in Setpar.h --- */
	if (w.wk.scon_) {
		w.wk.NoiseCnt_ = 0;
		w.wk.Pn_ = 128.;                 // o
		w.wk.Pmin_ = 1.e200;
		w.wk.frm_cnt_ = 0;
		w.wk.no_silence_ = 1;
		w.wk.LowerNoiseLimit_ = 128.;    // o
		w.wk.UpperNoiseLimit_ = 262144.; // o
		w.wk.VadHangCnt_ = 8;
		w.wk.seed_ = 0;
		w.wk.inv_filt_ = 0;
		for (i=0 ; i<w.wk.np_ ; i++) 
			w.wk.Noise_k_[i] = 0;
	}

	if (w.wk.lpcquant_) {

		w.wk.nblpc_ = (int *)calloc(w.wk.np_, sizeof(int));
		if (w.wk.nblpc_==NULL) {
#ifndef DSHOW_IO
			fprintf(ERROROUT, "\nabseinit: memory allocation error!\n\n"); 
			exit(1);
#else
			return;
#endif
		}

		w.wk.nblpc_[0]  = 7;
		w.wk.nblpc_[1]  = 7;
		w.wk.nblpc_[2]  = 6;
		w.wk.nblpc_[3]  = 6;
		w.wk.nblpc_[4]  = 5;
		w.wk.nblpc_[5]  = 5;
		w.wk.nblpc_[6]  = 5;
		w.wk.nblpc_[7]  = 5;
		w.wk.nblpc_[8]  = 5;
		w.wk.nblpc_[9]  = 4;
		w.wk.nblpc_[10] = 4;
		w.wk.nblpc_[11] = 4;
		w.wk.nblpc_[12] = 4;
		w.wk.nblpc_[13] = 3;   
		w.wk.nblpc_[14] = 3;

		if (w.wk.scon_)
			w.wk.nblpc_[15] = 2;
		else
			w.wk.nblpc_[15] = 3;
	}

	if (w.wk.ltpquant_) {
		#ifdef LTP_5_Bit	
			nbga_ = 5;
		#else
			w.wk.nbga_ = 6;
		#endif	
	}


	if (w.wk.ampquant_) {
		w.wk.nbgm_ = 6;
		w.wk.nbp_ = 3;
	}

	if (w.wk.scon_) 
		w.wk.nbgn_ = 5;

/* --- this code was originally in Setpar.h --- */
/* -------------------------------------------- */


	/* compute bit rate */

	for (nbpf=0, i=0; i<w.wk.np_; i++) 
		nbpf += w.wk.nblpc_[i]; // sum of all bits for LPC quantization per frame

	if (w.wk.scon_) 
		nbpf++;// one bit for VA per frame

	// subframe calculation for speech input, all parameters needed once per subframe
	j = w.wk.nbga_ + w.wk.nbgm_ + (w.wk.nimps_ * w.wk.nbp_) + (int)ceil(LD(nchoosek((int)(w.wk.fs_*w.wk.excveclen_), w.wk.nimps_))); 

	// subframe calculation for non-speech input, all parameters needed once per subframe
	nbpsf = w.wk.nbgn_;

	// calculate lagscode per frane for speech input
	for (aux=w.wk.maxlag_-w.wk.minlag_+1, i=1; i<(int)(w.wk.synfrmlen_/w.wk.excveclen_+.5); i++) { 
		aux *= w.wk.difflagrange_;
	}

	// add bits per frame for
	// non-speech input
	nbpsf = ((long)(w.wk.synfrmlen_/w.wk.excveclen_+.5) * nbpsf) + nbpf;
	// speech input
	nbpf += ((long)(w.wk.synfrmlen_/w.wk.excveclen_+.5) * j + (long)ceil(LD(aux)));

	#ifndef DSHOW_IO
	if (scon_) {
		fprintf(stdout, "absenc (%ld/%ldbps) - %s\n       ", (long)(nbpf*1000./synfrmlen_), (long)(nbpsf*1000./synfrmlen_), InFileName);
	}
	else {
		fprintf(stdout, "absenc (%ldbps)  -  %s\n       ", (long)(nbpf*1000./synfrmlen_), InFileName);
	}

	fflush(stdout);
	#endif
}
}
