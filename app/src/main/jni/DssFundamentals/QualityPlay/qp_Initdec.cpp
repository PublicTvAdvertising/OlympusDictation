/***ABS******************************************************************
 *                               initdec.c
 * -------------------------------------------------------------------- *
 *
 *   absdinit  -  initialization of abs program.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:        void absdinit(int argc,                i
 *                                    char *argv[],            i
 *                                    FILE **fpi,              o
 *                                    FILE **fpo,              o
 *                                    int *nbpf);              o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This procedure opens the files needed in the absdec program
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
 *                              Oct-09-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Initdec.c  $
 * Revision 1.9 2005/06/13 11:40:17CEST kroener 
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
void qp_set_silence_compression(dsswork& w, int on) {
	w.wk.scon_ = on;
}
int qp_get_silence_compression(dsswork& w) {
	return w.wk.scon_;
}
#endif

#ifdef DSHOW_IO
void qp_absdinit(dsswork& w, int *nbpf)
#else
void qp_absdinit(int argc, char *argv[], FILE **fpi, FILE **fpo, wavfile_chunk_type *wcto, int *nbpf)
#endif
{
	int		i, j;
	double	aux;
	int		nbpsf;
#ifndef DSHOW_IO
	char	*appname;
	char	*InFileName;
	char	*OutFileName;
#endif

	#ifdef SHOW_PARAMETER
		int prarg = 0;
	#endif
#ifndef DSHOW_IO
	/* --- set table for command line evaluation --- */   
	icsiargtab_t args[] =
	{
	{ 0, "ABSDEC - Analysis-by-Synthesis Decoder Simulation \n  "DATE" - GRUNDIG BUSINESS SYSTEMS GmbH,\n" 
	"\n  usage: Decode.exe infile=<infile> outfile=<outfile> [options]\n", ARG_DESC},
	{ "infile",        "INPUT filename *.edss",                ARG_STR,    &InFileName,     ARG_REQ},
	{ "outfile",       "OUTPUT filename *.wav",                ARG_STR,    &OutFileName,    ARG_REQ},
	{ "scon",          "silence compression",                  ARG_BOOL,   &scon_,          ARG_OPT},
	#ifdef SHOW_PARAMETER
	{ "p",             "Show all arguments on execution",      ARG_BOOL,   &prarg,          ARG_OPT},
	#endif
	{0,0,0}
	};
#endif
	/* --- set parameters --- */
	setpar(w);

	w.wk.fs_ = 16;

#ifndef DSHOW_IO
	/* ---- evaluate command line ----- */
	if (icsiargs(args,&argc,&argv,&appname)<0) { 
		exit(1);
	}

	fprintf(stdout, "\nABSDEC Analysis-by-Synthesis Decoder\n\n");

	/* --- check file names --- */
	if(IsNotEDssFileName(InFileName)) {
		fprintf(ERROROUT, "\nabsdinit: infile \"%s\": edss extention required!\n\n", InFileName);
		exit(1);
	}

	if(IsNotWavFileName(OutFileName)) {
		fprintf(ERROROUT, "\nabsdinit: outfile \"%s\": wav extention required!\n\n", OutFileName);
		exit(1);
	}

	/* --- open files --- */
	if ((*fpi=fopen(InFileName, "rb")) == NULL) {
		fprintf(ERROROUT, "\nabsdinit: infile \"%s\": error opening file!\n\n", InFileName);
		exit(1);
	}

	if ((*fpo=fopen(OutFileName, "wb")) == NULL) {
		fprintf(ERROROUT, "\nabsdinit: outfile \"%s\": error creating file!\n\n", OutFileName); 
		exit(1);
	}
	/* --- initialize wav output file --- */
	strcpy(wcto->WaveFileName, OutFileName);
	if (init_wave_file(*fpo,wcto) != 0) {
		fprintf(ERROROUT, "\nabsdinit: outfile \"%s\": error creating file!\n\n", OutFileName); 
		exit(1);
	}
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
			fprintf(ERROROUT, "\nabsdinit: memory allocation error!\n\n"); 
			exit(1);
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

	// sum of all bits for LPC quantization per frame 
	for (*nbpf=0, i=0; i<w.wk.np_; i++) 
		*nbpf += w.wk.nblpc_[i];

	// one bit for VA per frame
	if (w.wk.scon_) 
		(*nbpf)++;

	// subframe calculation for speech input, all parameters needed once per subframe
	j = w.wk.nbga_ + w.wk.nbgm_ + (w.wk.nimps_ * w.wk.nbp_) + (int)ceil(LD(nchoosek((int)(w.wk.fs_*w.wk.excveclen_), w.wk.nimps_))); 

	// subframe calculation for non-speech input, all parameters needed once per subframe
	nbpsf = w.wk.nbgn_;

	// calculate lagscode per frane for speech input
	for (aux=w.wk.maxlag_-w.wk.minlag_+1, i=1; i<(int)(w.wk.synfrmlen_/w.wk.excveclen_+.5); i++) 
		aux *= w.wk.difflagrange_;

	// add bits per frame for
	// non-speech input
	nbpsf = ((LONG)(w.wk.synfrmlen_/w.wk.excveclen_+.5) * nbpsf) + *nbpf;
	// speech input (bits per subframe + bit for lags)
	*nbpf += ((long)(w.wk.synfrmlen_/w.wk.excveclen_+.5) * j + (long)ceil(LD(aux)));

#ifndef DSHOW_IO
	if (scon_) {
		fprintf(stdout, "absdec (%ld/%ldbps) - %s\n       ", (long)(*nbpf*1000./synfrmlen_), (long)(nbpsf*1000./synfrmlen_), InFileName);
	}
	else {
		fprintf(stdout, "absdec (%ldbps)  -  %s\n       ", (long)(*nbpf*1000./synfrmlen_), InFileName);
	}
	fflush(stdout);
#endif

}
}
