#ifndef I_DSS_MODE_ENCODER_H__
#define I_DSS_MODE_ENCODER_H__

#include "DssModeCodecCommon.h"

class IDssModeEncoder{
public:
	IDssModeEncoder() {;}
	virtual ~IDssModeEncoder() {}

	virtual bool	init() = 0;
	virtual int		encode_frames(short *psInput, int cInSamples, byte_t *pbOut, int& cbOutBytes, int mode) = 0;
	virtual long	get_remaining() = 0;
	virtual void	finish() = 0;
	virtual void	flush() = 0;
protected:
};

IDssModeEncoder* createDssEncoder(dss_mode_t mode);

#endif
