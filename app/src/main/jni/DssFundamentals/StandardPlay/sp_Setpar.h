/* --- set parameters --- */

static void setpar(dsswork& w)
{
	/* common */
	w.wk.fs_ = 12;
	w.wk.synfrmlen_ = 24;

	/* input */
	w.wk.hdrlen_ = 0;
	w.wk.notchfilter_ = 1;
	w.wk.emphasiscoeff_ = 0.1;

	/* LPC */
	w.wk.anlfrmlen_ = 27;
	w.wk.np_ = 14;
	w.wk.stab_ = 0.0004;
	w.wk.bwe_ = 20.;

	/* excitation */
	w.wk.excveclen_ = 6.;

	/* adaptive excitation */
	w.wk.minlag_ = 36;
	w.wk.maxlag_ = 186;
	w.wk.difflagrange_ = 48;
	w.wk.alphaa_ = 0.7;
	w.wk.lha_ = 3.;

	/* innovation */
	w.wk.lhm_ = 3.5;
	w.wk.alpham_ = 0.9;
	w.wk.nimps_ = 7;

	/* silence compression */
	w.wk.reset_memories_ = 0;
	if (w.wk.scon_)
	   {
	   w.wk.NoiseCnt_ = 0;
	   w.wk.Pn_ = 128.;
	   w.wk.Pmin_ = 1.e200;
	   w.wk.frm_cnt_ = 0;
	   w.wk.no_silence_ = 1;
	   w.wk.LowerNoiseLimit_ = 128.;
	   w.wk.UpperNoiseLimit_ = 262144.;
	   w.wk.VadHangCnt_ = 5;
	   w.wk.seed_ = 0;
	   w.wk.inv_filt_ = 0;
	   for (int i=0 ; i<w.wk.np_ ; i++) w.wk.Noise_k_[i] = 0;
	   }

	/* quantization */
	w.wk.lpcquant_ = 1;
	w.wk.ltpquant_ = 1;
	w.wk.ampquant_ = 1;
	if (w.wk.lpcquant_)
	   {
	   w.wk.nblpc_ = (int *)calloc(w.wk.np_, sizeof(int));
	   if (w.wk.nblpc_==NULL)
		  {
		  printf("\nabs_init: memory allocation error!\n\n");
		  exit(1);
		  }
	   w.wk.nblpc_[0]  = 5;
	   w.wk.nblpc_[1]  = 5;
	   w.wk.nblpc_[2]  = 4;
	   w.wk.nblpc_[3]  = 4;
	   w.wk.nblpc_[4]  = 4;
	   w.wk.nblpc_[5]  = 4;
	   w.wk.nblpc_[6]  = 4;
	   w.wk.nblpc_[7]  = 4;
	   w.wk.nblpc_[8]  = 3;
	   w.wk.nblpc_[9]  = 3;
	   w.wk.nblpc_[10] = 3;
	   w.wk.nblpc_[11] = 3;
	   w.wk.nblpc_[12] = 3;
	   if (w.wk.scon_)
		  w.wk.nblpc_[13] = 2;
	   else
		  w.wk.nblpc_[13] = 3;
	   }
	if (w.wk.ltpquant_)
	   w.wk.nbga_ = 5;
	if (w.wk.ampquant_)
	   {
	   w.wk.nbgm_ = 6;
	   w.wk.nbp_ = 3;
	   }
	if (w.wk.scon_)
	   w.wk.nbgn_ = 5;
}