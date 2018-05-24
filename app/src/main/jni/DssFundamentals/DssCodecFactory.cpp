#include "DssModeEncoderImpl.h"

IDssModeEncoder* createDssEncoder(dss_mode_t mode)
{
	switch(mode) {
		//case mode_lp:
		//	return new DssLpEnc();
		case mode_sp:
			return new DssSpEnc();
		case mode_qp:
			return new DssQpEnc();
		default:
			return 0;
	}
}

