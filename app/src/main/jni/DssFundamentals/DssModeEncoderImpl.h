#ifndef DSS_MODE_ENCODER_IMPL_H
#define DSS_MODE_ENCODER_IMPL_H

#include "IDssModeEncoder.h"

class DssSpEnc : public IDssModeEncoder{
public:
	DssSpEnc() : IDssModeEncoder(), lpci(0), Mi(0), betai(0), ei(0), ai(0), s(0), is_inited_(false) 	{;}
	virtual ~DssSpEnc(){
		flush();
		finish();
	}

	bool	init();
	int		encode_frames(short *psInput, int cInSamples, byte_t *pbOut, int& cbOutBytes, int mode);
	long	get_remaining();
	void	finish();
	void	flush();
protected:
	int				block_num_;
	int				get_frame_length(byte_t* frame_header, int mode);

	/* --- compute array lengths --- */
	int				A;
	int				S;
	int				E;
	int				R;
	int				lens;
	int				lsp;
	int				nsbfrms;

	/* --- allocate memory --- */
	double			*s;
	short			*lpci;
	short			*Mi;
	short			*betai;
	unsigned long	*ei;
	short			**ai;
	bool			is_inited_;
	dsswork			w;
};

class DssQpEnc : public IDssModeEncoder{
public:
	DssQpEnc() : IDssModeEncoder(), lpci(0), Mi(0), betai(0), ei(0), ai(0), s(0),is_inited_(false) 	{;}
	virtual ~DssQpEnc(){
		flush();
		finish();
	}

	bool	init();
	int		encode_frames(short *psInput, int cInSamples, byte_t *pbOut, int& cbOutBytes, int mode);
	long	get_remaining();
	void	finish();
	void	flush();
protected:
	bool			is_inited_;
	int				block_num_;
	int				get_frame_length(byte_t* frame_header, int mode);

	/* --- compute array lengths --- */
	int				A;
	int				S;
	int				E;
	int				R;
	int				lens;
	int				lsp;
	int				nsbfrms;

	/* --- allocate memory --- */
	double			*s;
	short			*lpci;
	short			*Mi;
	short			*betai;
	__int64_t			*ei;
	short			**ai;
	dsswork			w;
};

class DssLpEnc : public IDssModeEncoder{
public:
	DssLpEnc() : IDssModeEncoder(), is_inited(false), frames_encoded_(0), overflow_in_num_(0) {;}
	virtual ~DssLpEnc(){
	}

	bool	init();
	int		encode_frames(short *psInput, int cbInSamples, byte_t *output, int& cbOutSamples, int mode);
	long	get_remaining();
	void	finish();
	void	flush() {;}
protected:
	int		get_frame_length(byte_t* frame_header, int mode);

	short	overflow_in_[4096];
	int		overflow_in_num_;
	int		frames_encoded_;
	bool	is_inited;
	LongPlay::longwork		w;
};

#endif