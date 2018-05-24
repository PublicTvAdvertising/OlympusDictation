
#ifndef _DSSWORK_H
#define _DSSWORK_H

#include <list>
#include "platform.linux.h"

#define qp_readspch_BLK 1024
#define qp_wrtspch_BLK 256
#define readspch_BLK 1024
#define wrtspch_BLK 256

struct WK {
	/* common */
	int fs_;                 /* sampling frequency (kHz)             */
	int synfrmlen_;          /* synthesis frame size (ms)            */

	/* input */
	int hdrlen_;             /* speech file header length in bytes   */
	int notchfilter_;        /* DC compensation flag                 */
	double emphasiscoeff_;   /* coefficient for pre-/deemphasis      */

	/* LPC */
	int anlfrmlen_;          /* analysis frame size                  */
	int np_;                 /* LPC order                            */
	double stab_;            /* stabilization factor                 */
	double bwe_;             /* bandwidth expansion (Hz)             */

	/* excitation */
	double excveclen_;       /* excitation vector length             */

	/* adaptive excitation */
	int minlag_;             /* minimum pitch lag (sampling periods) */
	int maxlag_;             /* maximum pitch lag (sampling periods) */
	int difflagrange_;       /* lag search range for differential .. */
	double alphaa_;          /* weighting factor                     */
	double lha_;             /* length of truncated impulse response */

	/* innovation */
	double alpham_;          /* weighting factor                     */
	double lhm_;             /* length of truncated impulse response */
	int nimps_;              /* number of excitat. pulses per vector */

	/* silence compression */
	int scon_;
	int NoiseCnt_;
	double Pn_;
	double Pmin_;
	long frm_cnt_;
	int no_silence_;
	double LowerNoiseLimit_;
	double UpperNoiseLimit_;
	int VadHangCnt_;
	int reset_memories_;
	short seed_;
	int inv_filt_;
	double Noise_k_[20];

	/* quantization */
	int lpcquant_;           /* LPC parameter quantization flag      */
	int ltpquant_;           /* LTP parameter quantization flag      */
	int ampquant_;           /* exc. amplitudes quantization flag    */
	int *nblpc_;             /* numbers of bits for LPC quantization */
	char rcqtblsflnm_[80];   /* refl.coef. quantiz. tables file name */
	int nbga_;               /* number of bits for ga quantization   */
	char gaqtblflnm_[80];    /* ga quantization table file name      */
	int nbgm_;               /* number of bits for MPE gain quantiz. */
	char gmqtblflnm_[80];    /* gm quantization table file name      */
	int nbp_;                /* no. of bits for pulse amplitudes     */
	char pqtblflnm_[80];     /* pulse amplit. quant. table file name */
	int nbgn_;               /* number of bits for noise level quant.*/

	// qp_abs_anl()
	int		qp_abs_anl_init;
	double	*qp_abs_anl_memb_if;
	double	*qp_abs_anl_e;
	double	*qp_abs_anl_esave;
	int		qp_abs_anl_A;
	int		qp_abs_anl_S;
	int		qp_abs_anl_N;

	// qp_abs_anl_syn()
	int		qp_abs_anl_syn_init;
	double	*qp_abs_anl_syn_memb_if;      /* inverse filter memory                */
	double	*qp_abs_anl_syn_mema_sf;      /* synthesis filter memory              */
	double	*qp_abs_anl_syn_e;
	double	*qp_abs_anl_syn_esave;
	int		qp_abs_anl_syn_A;
	int		qp_abs_anl_syn_S;
	int		qp_abs_anl_syn_N;

	// qp_abs_syn()
	int		qp_abs_syn_init;
	double	*qp_abs_syn_mema_sf;      /* synthesis filter memory */
	int		qp_abs_syn_S;
	int		qp_abs_syn_N;

	// qp_adaptive_codebook_search()
	int		qp_adaptive_codebook_search_init;
	double	*qp_adaptive_codebook_search_acdbk;

	// qp_excitation_evaluation()
	int		qp_excitation_evaluation_init;
	double	*qp_excitation_evaluation_mema_wf;	/* weighting filter memory              */
	double	*qp_excitation_evaluation_mema_swf;	/* memory of synthesis/weighting        */
								/*                       filter cascade */
	int		qp_excitation_evaluation_N;			/* length of excitation frame           */

	// qp_excitation_generation()
	int		qp_excitation_generation_init;
	double	*qp_excitation_generation_excdel;       /* excitation delay line           */
	double	*qp_excitation_generation_ed;           /* pointer to end of delay line    */
	int		qp_excitation_generation_N;             /* length of excitation frame      */

	// qp_getbits()
	unsigned short	qp_getbits_intemp;
	unsigned short	qp_getbits_inbitmask;
	unsigned long	qp_getbits_bits_read;
	unsigned long	qp_getbits_bits_from_stream;
	int				qp_getbits_eof;

	// qp_LTP_analysis()
	int		qp_LTP_analysis_init;
	double	*qp_LTP_analysis_mema_wf;		/* adaptive excitation evaluation       */
									/*              weighting filter memory */
	double	*qp_LTP_analysis_mema_swf;	/* adaptive excitation evaluation       */
									/*    synthesis/weighting filter memory */
	int		qp_LTP_analysis_N;
	double	*qp_LTP_analysis_kltp_prev;

	// qp_mp_excitation_evaluation()
	int		qp_mp_excitation_evaluation_init;
	int		qp_mp_excitation_evaluation_N;
	int		qp_mp_excitation_evaluation_lh;

	// qp_putbits()
	unsigned short	qp_putbits_outtemp;
	unsigned short	qp_putbits_outbitmask;

	// qp_readspch()
	int     qp_readspch_first;
	short   qp_readspch_buf[qp_readspch_BLK];
	double  qp_readspch_buf1[qp_readspch_BLK];
	short   qp_readspch_xold;
	double  qp_readspch_yold;
	double  qp_readspch_y1old;
	double  qp_readspch_alpha;
	int     qp_readspch_endoffile;

	// qp_snr()
	double	qp_snr_Stot;
	double	qp_snr_Ntot;
	double	qp_snr_av_segSNR;
	int		qp_snr_frmno;
	int		qp_snr_nfrmsig;

	// qp_vad()
	double  qp_vad_mem[20];

	// qp_wrtspch()
	double  qp_wrtspch_buf[qp_wrtspch_BLK];
	short   qp_wrtspch_obuf[qp_wrtspch_BLK];
	double  qp_wrtspch_yold;

	// abs_anl()
	int		abs_anl_init;
	double	*abs_anl_memb_if;
	double	*abs_anl_e;
	double	*abs_anl_esave;
	int		abs_anl_A;
	int		abs_anl_S;
	int		abs_anl_N;

	// abs_anl_syn()
	int		abs_anl_syn_init;
	double	*abs_anl_syn_memb_if;      /* inverse filter memory                */
	double	*abs_anl_syn_mema_sf;      /* synthesis filter memory              */
	double	*abs_anl_syn_e;
	double	*abs_anl_syn_esave;
	int		abs_anl_syn_A;
	int		abs_anl_syn_S;
	int		abs_anl_syn_N;

	// abs_syn()
	int		abs_syn_init;
	double	*abs_syn_mema_sf;      /* synthesis filter memory              */
	int		abs_syn_S;
	int		abs_syn_N;

	// adaptive_codebook_search()
	int		adaptive_codebook_search_init;
	double	*adaptive_codebook_search_acdbk;

	// excitation_evaluation()
	int		excitation_evaluation_init;
	double	*excitation_evaluation_mema_wf;      /* weighting filter memory              */
	double	*excitation_evaluation_mema_swf;     /* memory of synthesis/weighting        */
									 /*                       filter cascade */
	int		excitation_evaluation_N;             /* length of excitation frame           */

	// excitation_generation()
	int		excitation_generation_init;
	double	*excitation_generation_excdel;       /* excitation delay line           */
	double	*excitation_generation_ed;           /* pointer to end of delay line    */
	int		excitation_generation_N;             /* length of excitation frame      */

	// LTP_analysis()
	int		LTP_analysis_init;
	double	*LTP_analysis_mema_wf;      /* adaptive excitation evaluation       */
									  /*              weighting filter memory */
	double	*LTP_analysis_mema_swf;     /* adaptive excitation evaluation       */
									  /*    synthesis/weighting filter memory */
	int		LTP_analysis_N;
	double	*LTP_analysis_kltp_prev;

	// mp_excitation_evaluation()
	int		mp_excitation_evaluation_init;
	int		mp_excitation_evaluation_N;
	int		mp_excitation_evaluation_lh;

	// putbits()
	unsigned short   putbits_outtemp;
	unsigned short   putbits_outbitmask;

	// readspch()
	int		readspch_first;
	short	readspch_buf[readspch_BLK];
	double	readspch_buf1[readspch_BLK];
	short	readspch_xold;
	double	readspch_yold;
	double	readspch_y1old;
	double	readspch_alpha;
	int		readspch_endoffile;

	// snr()
	double	snr_Stot;
	double	snr_Ntot;
	double	snr_av_segSNR;
	int		snr_frmno;
	int		snr_nfrmsig;

	// vad()
	double  vad_mem[20];

	// wrtspch()
	double  wrtspch_buf[wrtspch_BLK];
	short   wrtspch_obuf[wrtspch_BLK];
	double  wrtspch_yold;
};

struct dss_block;

class dsswork
{
public:
	WK wk;

	std::list<dss_block *> in_queue;
	std::list<dss_block *> out_queue;
	int is_eof;

	dsswork(){
		memset(&wk, 0, sizeof(wk));
		wk.qp_putbits_outbitmask = 0x8000;
		wk.qp_readspch_first = 1;
		wk.qp_readspch_alpha = 32767./32768.;

		wk. putbits_outbitmask = 0x8000;

		wk.readspch_first = 1;
		wk.readspch_alpha = 32767./32768.;

		is_eof = false;
	}
};

#endif // #ifndef _DSSWORK_H
