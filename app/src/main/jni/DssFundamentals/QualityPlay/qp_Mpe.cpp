/***ABS******************************************************************
 *                                  mpe.c
 * -------------------------------------------------------------------- *
 *
 *   qp_mp_excitation_evaluation  -  evaluation of multi-pulse excitation.
 *
 * -------------------------------------------------------------------- *
 *
 *    prototype:   void qp_mp_excitation_evaluation(
 *                                        double *t,                i
 *                                        double *h,                i
 *                                        double *hh,               i
 *                                        double *empe,             o
 *                                        unsigned long *indexe,    o
 *                                        short *indicesa)          o
 *
 * -------------------------------------------------------------------- *
 *
 *    description:
 *
 *      This routine generates a multi-pulse excitation vector using
 *      an analysis-by-synthesis loop.
 *      The target vector is 't', the truncated impulse response of
 *      the weighting/synthesis filter cascade is 'h' (length =
 *      'excveclen_').
 *      The excitation vector of 'excveclen_*fs_' samples length
 *      will contain 'nimps_' excitation pulses.
 *      The resulting excitation vector is returned in 'empe'.
 *
 *      This routine uses a suboptimal method to derive a
 *      multi-pulse excitation vector, which is based on Singhal's
 *      ideas [1]. Some (hopefully self-explanatory) simplifications
 *      have been introduced in order to obtain a more "DSP-friendly"
 *      algorithm.
 *
 *      After completion of the MPE optimization the found pulses
 *      are sorted according to their locations within the frame
 *      (descending order) and the positions are encoded to give
 *      one long word. The resulting positions code value is returned
 *      in 'indexe'.
 *
 *      The amplitudes quantization indices are returned in array
 *      'indicesa', the block maximum index being the first entry.
 *
 *      [1] Singhal, S., Atal, B. S., "Amplitude Optimization and
 *          Pitch Prediction in Multipulse Coders", IEEE Trans. ASSP,
 *          vol. 37, no. 3, pp. 317-327, March 1989.
 *
 * -------------------------------------------------------------------- *
 *
 *    uses global variables:    excveclen_
 *                              fs_
 *                              lhm_
 *                              nimps_
 *                              ampquant_
 *
 * -------------------------------------------------------------------- *
 *
 *    author:                   Holger Carl, AGDSV-RUB, Grundig
 *
 *    creation date:            Apr-09-1992
 *
 *    modification date:        Dec-02-1995
 *                              Jun-11-2003 by Michael Kroener
 *                              Jul-12-2003 by Holger Carl
 *                              Aug-06-2003 by Michael Kroener
 *                              Oct-09-2003 by Michael Kroener
 *                              Oct-20-2003 by Michael Kroener
 *
 ************************************************************************
 * $Log: Mpe.c  $
 * Revision 1.19 2005/06/13 10:02:43CEST kroener 
 * 
 ************************************************************************
 *
 *    Copyright (C) 1995 Grundig Professional Electronics GmbH
 *
 ************************************************************************/

#include "qp_abs.h"

#define  MIN(a,b)  ((a) < (b) ? (a) : (b))
#define  MAX(a,b)  ((a) > (b) ? (a) : (b))

namespace QualityPlay {

/* --- prototypes --- */
static void bubblesort(int *pos, double *amp, int length);
static void amplitudes_quantization(dsswork& w, double *amp, short *indices);

/* --- new MPE version --- */
void qp_mp_excitation_evaluation(dsswork& w, double *t, double *h, double *hh, double *empe, __int64_t *indexe, short *indicesa)
{
	LONG			i;
	long			j, k, n;
	double			*g;
	double			*th;
	int				*pos;
	double			*d;
	double			**m;
	double			*amp;
	double			max;
	double			max_g;
	double			max_th2;
	double			*g_p;
	double			*one_over_g_p;
	int				ibest;
	register double	accu;
	double			*p1, *p2;
	int				start;
	__int64_t			*y;
	__int64_t			int64_accu;

	/* --- initialization --- */
	if (!w.wk.qp_mp_excitation_evaluation_init) {
		w.wk.qp_mp_excitation_evaluation_N    = (int)(w.wk.excveclen_*w.wk.fs_);
		w.wk.qp_mp_excitation_evaluation_lh   = (int)(w.wk.lhm_*w.wk.fs_);
		w.wk.qp_mp_excitation_evaluation_init = 1;
	}

	/* --- allocate memory --- */
	g            = (double *)calloc(w.wk.qp_mp_excitation_evaluation_N, sizeof(double));
	th           = (double *)calloc(w.wk.qp_mp_excitation_evaluation_N, sizeof(double));
	pos          = (int    *)calloc(w.wk.nimps_, sizeof(int));
	d            = (double *)calloc(w.wk.nimps_, sizeof(double));
	g_p          = (double *)calloc(w.wk.nimps_, sizeof(double));
	one_over_g_p = (double *)calloc(w.wk.nimps_, sizeof(double));

	if (w.wk.nimps_ > 1)	{
		m = (double **)calloc(w.wk.qp_mp_excitation_evaluation_N, sizeof(double *));
		for (i=0; i<w.wk.qp_mp_excitation_evaluation_N && m!=NULL; i++)
			if ((m[i] = (double *)calloc(w.wk.nimps_-1, sizeof(double))) == NULL) m = NULL;
		if (m == NULL) {
			fprintf(ERROROUT, "\nmp_excitation_evaluation: memory allocation error!\n\n");
			exit(1);
		}
	}

	y	= (__int64_t *)calloc(w.wk.nimps_, sizeof(__int64_t)); 
	amp	= (double *)calloc(w.wk.nimps_, sizeof(double));

	if (th==NULL || g==NULL || d==NULL || pos==NULL || amp==NULL || y==NULL || g_p==NULL || one_over_g_p==NULL)	{
		fprintf(ERROROUT, "\nmp_excitation_evaluation: memory allocation error!\n\n");
		exit(1);
	}

	/* --- initialize vector y --- */
	/* done by calloc */

	/* --- initialize vector th --- */
	for (i=0; i<w.wk.qp_mp_excitation_evaluation_N; th[i++]=accu) {
		for (accu=0, p1=h, p2=t+i, n=i; n<MIN(w.wk.qp_mp_excitation_evaluation_N,i+w.wk.qp_mp_excitation_evaluation_lh); n++) {
			accu += *p1++ * *p2++;
		}
	}

	/* --- initialize vector g --- */
	for (i=0; i<w.wk.qp_mp_excitation_evaluation_N; i++) {
		g[i] = hh[0];
	}

	/* --- search first pulse --- */
	for (ibest=0, max=0, i=0; i<w.wk.qp_mp_excitation_evaluation_N; i++) {
		if ((accu=th[i]*th[i]) > max) { 
			max = accu; 
			ibest = i; 
		}
	}

	pos[0] = ibest;
	d[0]   = th[ibest];
	g_p[0] = g[ibest];
	one_over_g_p[0] = 1. / g[ibest]; /* to avoid repeated division */

	/* --- search remaining pulses --- */
	for (j=0; j<w.wk.nimps_-1;) {

		/* --- compute new column for matrix m and update g and th --- */
		for (i=0; i<w.wk.qp_mp_excitation_evaluation_N; i++) {
			for (accu=0, n=0; n<j; n++) 
				accu += m[i][n] * m[ibest][n] * g_p[n];
			accu = (hh[abs(i-ibest)] - accu);
			m[i][j] = accu * one_over_g_p[j];
		}

		for (i=0; i<w.wk.qp_mp_excitation_evaluation_N; i++) {
			g[i]  -= m[i][j] * m[i][j] * g_p[j];
			th[i] -= m[i][j] * d[j];
		}


		/* --- in to avoid repeated detection:
		force elements of th and g corresponding to already
		detected pulse positions to zero and large value, respectively --- */
		for (k=0; k<=j; k++) {
			th[pos[k]] = 0.;
			g[pos[k]]  = 1e20;
		}

		/* --- search next pulse --- */
		++j;

		for (max_th2=-1, max_g=1, i=0; i<w.wk.qp_mp_excitation_evaluation_N; i++) {
			if( (((accu = th[i] * th[i]) * max_g) > (max_th2 * g[i]) ) && g[i] != 1e20) {
				max_th2 = accu; 
				max_g   = g[i]; 
				ibest   = i;
			}
		}

		pos[j] = ibest;
		d[j]   = th[ibest];
		g_p[j] = g[ibest];
		one_over_g_p[j] = 1. / g[ibest]; /* to avoid repeated division */
	}

	/* --- finally, compute pulse amplitudes --- */
	for (j=w.wk.nimps_-1; j>=0; j--)	{
		for (accu=0, k=j+1; k<w.wk.nimps_; k++) 
			accu += m[pos[k]][j] * amp[k];
		amp[j] = d[j] * one_over_g_p[j] - accu;
	}

	/* --- sort pulses and encode pulse positions --- */
	bubblesort(pos, amp, w.wk.nimps_); // sort pulse-positions descending

	for (j=0; j<w.wk.nimps_; j++) {
		y[j] = 0;  
	}

	for (start=0, int64_accu = 0, j=w.wk.nimps_-1; j>=0; j--) {
		for (n=start; n<pos[j]; n++) {
			for (i=w.wk.nimps_-1; i>0; i--) {
				y[i] += y[i-1];
			}
			y[0] = n+1;
		}
		int64_accu += y[w.wk.nimps_-j-1];
		start = pos[j];
	}
	*indexe = int64_accu;

	/* --- quantization of pulse amplitudes --- */
	if (w.wk.ampquant_) {
		amplitudes_quantization(w, amp, indicesa);
	}

	/* --- construct multi-pulse excitation vector --- */
	for (n=0; n<w.wk.qp_mp_excitation_evaluation_N; n++) {
		empe[n] = 0;
	}

	for (j=0; j<w.wk.nimps_; j++) {
		empe[pos[j]] = amp[j]; 
	}

	/* --- free memory --- */
	free(th);
	free(g);
	free(pos);
	free(d);
	free(g_p);
	free(one_over_g_p);
	if (w.wk.nimps_ > 1)	{
		for (i=0; i<w.wk.qp_mp_excitation_evaluation_N; i++) 
			free(m[i]);
		free(m);
	}
	free(amp);
	free(y); /* --- changed by Kroener 2003.06.11 --- */ 
}


static void bubblesort(int *pos, double *amp, int length)
{
	int       i, j, aux;
	double    auxd;
	for (i=0; i<length-1; i++)
		for (j=0; j<length-1-i; j++)
			if (pos[j] < pos[j+1]) {
				aux = pos[j]; pos[j] = pos[j+1]; pos[j+1] = aux;
				auxd = amp[j]; amp[j] = amp[j+1]; amp[j+1] = auxd;
			}
}

static void amplitudes_quantization(dsswork& w, double *amp, short *indices) 
{
	static const double	QT_MPEB[64] =
	#include "QT_MPEB.h"
	static const double	QT_MPEA[8] =
	#include "QT_MPEA.h" 
	int				i;
	double			max;
	double			maxq;

	/* --- detection of maximum magnitude --- */
	for (max=0, i=0; i<w.wk.nimps_; i++) 
		if (fabs(amp[i]) > max) 
			max = fabs(amp[i]);

	/* --- quantization of block maximum --- */
	maxq = qp_quantize( max, QT_MPEB, w.wk.nbgm_, indices);

	if (maxq > 0) {
		/* --- quantization of normalized amplitudes --- */
		for (i=0; i<w.wk.nimps_; i++) {
			amp[i] = qp_quantize(amp[i]/maxq, QT_MPEA, w.wk.nbp_, indices+1+i) * maxq;
		}
	}
	else {
		for (i=0; i<w.wk.nimps_; i++) {
			indices[i+1] = 0; 
			amp[i] = 0;
		}
	}
}
     

}
