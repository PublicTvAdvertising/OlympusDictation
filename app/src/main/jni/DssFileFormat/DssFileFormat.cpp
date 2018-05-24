#include "stdafx_ff.h"
#include "DssElement.h"
#include "DssCryptor.h"
#include "64bit.h"

#ifdef DEBUG_NEW

#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


// modified

#if TARGET_RT_BIG_ENDIAN
 #define EndianS16_BtoN(value)               (value)
    #define EndianS16_NtoB(value)               (value)
    #define EndianU16_BtoN(value)               (value)
    #define EndianU16_NtoB(value)               (value)
    #define EndianS32_BtoN(value)               (value)
    #define EndianS32_NtoB(value)               (value)
    #define EndianU32_BtoN(value)               (value)
    #define EndianU32_NtoB(value)               (value)
    #define EndianS64_BtoN(value)               (value)
    #define EndianS64_NtoB(value)               (value)
    #define EndianU64_BtoN(value)               (value)
    #define EndianU64_NtoB(value)               (value)
#else
  #define EndianS16_LtoN(value)               (value)
    #define EndianS16_NtoL(value)               (value)
    #define EndianU16_LtoN(value)               (value)
    #define EndianU16_NtoL(value)               (value)
    #define EndianS32_LtoN(value)               (value)
    #define EndianS32_NtoL(value)               (value)
    #define EndianU32_LtoN(value)               (value)
    #define EndianU32_NtoL(value)               (value)
    #define EndianS64_LtoN(value)               (value)
    #define EndianS64_NtoL(value)               (value)
    #define EndianU64_LtoN(value)               (value)
    #define EndianU64_NtoL(value)               (value)
#endif

/*
    Map native to actual
*/
#if TARGET_RT_BIG_ENDIAN
   #define EndianS16_LtoN(value)               EndianS16_LtoB(value)
  #define EndianS16_NtoL(value)               EndianS16_BtoL(value)
  #define EndianU16_LtoN(value)               EndianU16_LtoB(value)
  #define EndianU16_NtoL(value)               EndianU16_BtoL(value)
  #define EndianS32_LtoN(value)               EndianS32_LtoB(value)
  #define EndianS32_NtoL(value)               EndianS32_BtoL(value)
  #define EndianU32_LtoN(value)               EndianU32_LtoB(value)
  #define EndianU32_NtoL(value)               EndianU32_BtoL(value)
  #define EndianS64_LtoN(value)               EndianS64_LtoB(value)
  #define EndianS64_NtoL(value)               EndianS64_BtoL(value)
  #define EndianU64_LtoN(value)               EndianU64_LtoB(value)
  #define EndianU64_NtoL(value)               EndianU64_BtoL(value)
#else
    #define EndianS16_BtoN(value)               EndianS16_BtoL(value)
  #define EndianS16_NtoB(value)               EndianS16_LtoB(value)
  #define EndianU16_BtoN(value)               EndianU16_BtoL(value)
  #define EndianU16_NtoB(value)               EndianU16_LtoB(value)
  #define EndianS32_BtoN(value)               EndianS32_BtoL(value)
  #define EndianS32_NtoB(value)               EndianS32_LtoB(value)
  #define EndianU32_BtoN(value)               EndianU32_BtoL(value)
  #define EndianU32_NtoB(value)               EndianU32_LtoB(value)
  #define EndianS64_BtoN(value)               EndianS64_BtoL(value)
  #define EndianS64_NtoB(value)               EndianS64_LtoB(value)
  #define EndianU64_BtoN(value)               EndianU64_BtoL(value)
  #define EndianU64_NtoB(value)               EndianU64_LtoB(value)
#endif



namespace DssFileFormat 
{
	//================== helper functions

	bool is_element_common_header(DssElement *pel) {
		if (pel->GetElementType() == DSS_FILE_HEADER && static_cast<DssHeader *>(pel)->GetHeaderType() == DSS_COMMON_HEADER)
			return true;
		return false;
	}

	bool is_element_block_header(DssElement *pel) {
		if (pel->GetElementType() == DSS_BLOCK_HEADER) 
			return true;
		return false;
	}

	int get_samplerate_for_mode(int mode) {
		int sr = SP_FS;
		if (compression_mode_t(mode) >= QP_NO_SCVA) {
			sr = QP_FS;
		}
		else if (compression_mode_t(mode) >= LP_NO_SCVA) {
			sr = LP_FS;
		}
		return sr;
	}
	int get_bitrate_for_mode(int mode) {
		switch(compression_mode_t(mode)) {
			case SP_NO_SCVA:
			case SP_WITH_SCVA:
				return int(SP_BITRATE);
				break;
			case LP_NO_SCVA:
			case LP_WITH_SCVA:
			case MLP_NO_SCVA:
			case MLP_WITH_SCVA:
				return int(LP_BITRATE);
				break;
			case QP_NO_SCVA:
			case QP_WITH_SCVA:
				return int(QP_BITRATE);
				break;
			default:
				return 1;
		}
	}
	int get_framelength_for_mode(int mode) {
		switch(compression_mode_t(mode)) {
			case SP_NO_SCVA:
			case SP_WITH_SCVA:
				return int(SP_PCM_SAMPLES);
				break;
			case LP_NO_SCVA:
			case LP_WITH_SCVA:
			case MLP_NO_SCVA:
			case MLP_WITH_SCVA:
				return int(LP_PCM_SAMPLES);
				break;
			case QP_NO_SCVA:
			case QP_WITH_SCVA:
				return int(QP_PCM_SAMPLES);
				break;
			default:
				return 1;
		}
	}
	int reduce_block_duration_by(byte_t *pData, int cbLength, int samples_to_reduce_by)
	{
		DssFileFormat::DssCryptor dssDummyCryptor;
		DssFileFormat::DssBlock dssBlock(dssDummyCryptor);
		dssBlock.Parse(pData, (LONG)min(DssFileFormat::SIZE_DSS_SECTOR, cbLength), false, false);
		dssBlock.ReduceDurationBy(int(samples_to_reduce_by));
		::memcpy(pData, dssBlock.Data(), min(SIZE_DSS_SECTOR, cbLength));
		return (DssFileFormat::get_framelength_for_mode(dssBlock.get_CompressionMode()) * (int)dssBlock.GetActualNumberOfBlockFrames());
	}
	LONGLONG reduce_block_duration_by(byte_t *pData, int cbLength, LONGLONG llRefTime)
	{
		DssFileFormat::DssCryptor dssDummyCryptor;
		DssFileFormat::DssBlock dssBlock(dssDummyCryptor);
		dssBlock.Parse(pData, (LONG)min(DssFileFormat::SIZE_DSS_SECTOR, cbLength), false, false);
		dssBlock.ReduceDurationBy(LONGLONG(llRefTime));
		::memcpy(pData, dssBlock.Data(), min(SIZE_DSS_SECTOR, cbLength));
		return dssBlock.GetDurationRefTime();

	}

	const LONGLONG convert_hhmmss_to_reftime(const ascii_t* szt) {
		ascii_t hh[3],mm[3],ss[3];
		LONGLONG h,m,s;
		::memset(hh,0,3);
		::memset(mm,0,3);
		::memset(ss,0,3);
		::memcpy(hh,static_cast<const void *>(&szt[0]),2);
		::memcpy(mm,static_cast<const void *>(&szt[2]),2);
		::memcpy(ss,static_cast<const void *>(&szt[4]),2);
		h = atoi(hh);
		m = atoi(mm);
		s = atoi(ss);
		return (h*3600L + m*60L + s)*UNITS;
	}
	void convert_reftime_to_hhmmss(ascii_t* szt, size_t lenstr, REFERENCE_TIME time) {
		LONGLONG h,m,s;
		s = time/UNITS;
#ifdef DSS_100H_SEC
		if (s >= DSS_100H_SEC){
			s = DSS_100H_SEC - 1;
		}
#endif
		h = s/3600L;
		m = (s - h*3600L)/60L;
		s -= h*3600L + m*60L;

		if ((h+m+s) == 0 && time > 0) { //always roudn up the seconds to 1 -- no zero lengths allowed
			s = 1;
		}
		sprintf((char *)szt, "%02d%02d%02d", (int)h, (int)m, (int)s);
	}

	typedef  struct   {
	   int		AcLg;
	   int		AcGn;
	   int		Mamp;
	   int		Grid;
	   int		Tran;
	   int		Pamp;
	   int		 Ppos;
	} SFSDEF;

	typedef  struct   {
	   word_t	Crc;
	   long		LspId;
	   int		Olp[4/2];
	   SFSDEF	Sfs[4];
	   int		mode;
	} LINEDEF;

	static long Ser2Par( word_t **Pnt, int Count )
	{
		int 	i;
		long  Rez = 0L;

		for ( i = 0 ; i < Count ; i ++ ) {
			Rez += (long) **Pnt << i ;
			(*Pnt) ++ ;
		}
		return Rez ;
	}
	static LINEDEF	Line_Unpk(char *Vinp, word_t *Ftyp, word_t Crc)
	{
		int 	i  ;
		word_t	BitStream[192] ;
		word_t	*Bsp = BitStream ;
		LINEDEF Line ;
		word_t	Info ;

		Line.Crc = Crc;
		if (Crc != 0)
			return Line;

		/* Unpack the byte info to BitStream vector */
		for ( i = 0 ; i < 16 ; i ++ )
			BitStream[i] = (word_t) (( Vinp[i>>3] >> (i & (word_t)0x0007) ) & 1);

		/* Decode the first two bits */
		Info = (word_t)Ser2Par( &Bsp, 2 ) ;
		Line.mode = Info;
		if (Info == 3) {
			*Ftyp = 0;
			Line.LspId = 0L;	/* Dummy : to avoid Borland C3.1 warning */
			return Line;
		}

		/* Decode the LspId */
		Line.LspId = Ser2Par( &Bsp, 24 ) ;

		if (Info == 2) {
			/* Decode the Noise Gain */
			Line.Sfs[0].Mamp = (word_t)Ser2Par( &Bsp, 6);
			*Ftyp = 2;
			return Line ;
		}

		/*
		 * Decode the common information to both rates
		 */

		*Ftyp = 1;

		return Line;
	}
	int lps_mode_get_framelength_bytes(char *Vinp) 
	{
		LINEDEF  Line;
		word_t	 Ftyp;

		/*
		 * Decode the packed bitstream for the frame.  (Text: Section 4;
		 * pars of sectio,d 2.17, 2.18)
		 */
		Line = Line_Unpk(Vinp, &Ftyp, 0);

		switch (Line.mode) {
			case 3:
				return 1;
			case 2:
				return 4;
			case 1:
				return 20;
			default:
			case 0:
				return 24;
		}
	}

	void init_decryption(DssCryptor& cryptor, const char *szpassword16, const byte_t *szsalt16, const byte_t *verification_key4)
	{
		cryptor.init_decryption((const unsigned char *)szsalt16, (const unsigned char *)verification_key4, szpassword16);
	}

	namespace endian {
#if defined(WIN32) || TARGET_RT_LITTLE_ENDIAN
		void* N2L(void* raw, size_t bytes) {
			return raw;
		}
		void* L2N(void *raw, size_t bytes) {
			return raw;
		}
		std::vector<byte_t> N2L(const std::vector<byte_t>& v) {
			return v;
		}
		std::vector<byte_t> L2N(const std::vector<byte_t>& v) {
			return v;
		}
		inline DssFileFormat::v2::common_header_t N2L(const DssFileFormat::v2::common_header_t& v) {
			return v;
		}
		inline DssFileFormat::v2::common_header_t L2N(const DssFileFormat::v2::common_header_t& v) {
			return v;
		}
#else
		void* N2L(void* raw, size_t bytes) {
			short *ps = static_cast<short *>(raw);
			for (size_t k=0; k < bytes/2; ++k) {
				ps[k] = EndianS16_NtoL(ps[k]);
			}
			return raw;
		}
		void* L2N(void *raw, size_t bytes) {
			short *ps = static_cast<short *>(raw);
			for (size_t k=0; k < bytes/2; ++k) {
				ps[k] = EndianS16_LtoN(ps[k]);
			}
			return raw;
		}
		template<>
		void SwapL2N(DssFileFormat::word_t& v) {
			v = EndianU16_LtoN(v);
		}
		template<>
		void SwapL2N(DssFileFormat::dword_t& v) {
			v = EndianU32_LtoN(v);
		}
		template<>
		void SwapL2N(DssFileFormat::byte_t& v) {
		}
		template<>
		void SwapL2N(DssFileFormat::v2::waveformat_ex_t& pwfx) {
			SwapL2N(pwfx.wFormatTag);
			SwapL2N(pwfx.nChannels);
			SwapL2N(pwfx.nSamplesPerSec);
			SwapL2N(pwfx.nAvgBytesPerSec);
			SwapL2N(pwfx.nBlockAlign);
			SwapL2N(pwfx.wBitsPerSample);
		}
		template<>
		void SwapN2L(DssFileFormat::word_t& v) {
			v = EndianU16_NtoL(v);
		}
		template<>
		void SwapN2L(DssFileFormat::dword_t& v) {
			v = EndianU32_NtoL(v);
		}
		template<>
		void SwapN2L(DssFileFormat::byte_t& v) {
		}
		template<>
		void SwapN2L(DssFileFormat::v2::waveformat_ex_t& pwfx) {
			SwapN2L(pwfx.wFormatTag);
			SwapN2L(pwfx.nChannels);
			SwapN2L(pwfx.nSamplesPerSec);
			SwapN2L(pwfx.nAvgBytesPerSec);
			SwapN2L(pwfx.nBlockAlign);
			SwapN2L(pwfx.wBitsPerSample);
		}
		template<>
		void SwapL2N(v1::header_map_t& v) {
			size_t sttest = sizeof(v.common_header);
			size_t offset_of_optional = offsetof(v1_header_map_t, optional_header);
			SwapL2N(v.common_header.version_id_dss);
			SwapL2N(v.common_header.release_id_dss);
			SwapL2N(v.common_header.licensee_id);
			SwapL2N(v.common_header.job_number);
			SwapL2N(v.common_header.object_word);
			SwapL2N(v.common_header.process_word);
			SwapL2N(v.common_header.status_word);
			for (size_t k=0; k < 16; ++k) {
				SwapL2N(v.optional_header.ex_index_mark[k].dw_sector_offset);
				SwapL2N(v.optional_header.ex_index_mark[k].w_frame_offset);
				SwapL2N(v.optional_header.ex_index_mark[k].dw_position);
			}
			SwapL2N(v.optional_header.ex_rec_length);
			SwapL2N(v.optional_header.playback_position);
		}
		template<>
		void SwapN2L(v1::header_map_t& v) {
			SwapN2L(v.common_header.version_id_dss);
			SwapN2L(v.common_header.release_id_dss);
			SwapN2L(v.common_header.licensee_id);
			SwapN2L(v.common_header.job_number);
			SwapN2L(v.common_header.object_word);
			SwapN2L(v.common_header.process_word);
			SwapN2L(v.common_header.status_word);
			for (size_t k=0; k < 16; ++k) {
				SwapN2L(v.optional_header.ex_index_mark[k].dw_sector_offset);
				SwapN2L(v.optional_header.ex_index_mark[k].w_frame_offset);
				SwapN2L(v.optional_header.ex_index_mark[k].dw_position);
			}
			SwapN2L(v.optional_header.ex_rec_length);
			SwapN2L(v.optional_header.playback_position);
		}

		template<>
		void SwapL2N(DssFileFormat::v2::common_header_t& v) {
			DssFileFormat::word_t version = v.version_id;
			SwapL2N(version);
			if (1 == version) {
				SwapL2N(*(v1::header_map_t* )&v);
				return;
			}
			SwapL2N(v.attribute_flag);
			SwapL2N(v.dss_status.flags);
			SwapL2N(v.ex_rec_length);
			SwapL2N(v.header_count);
			SwapL2N(v.job_number);
			SwapL2N(v.licensee_id);
			SwapL2N(v.num_of_index_mark);
			SwapL2N(v.object.word);
			SwapL2N(v.playback_position);
			SwapL2N(v.priority_level);
			SwapL2N(v.priority_status);
			SwapL2N(v.process.word);
			SwapL2N(v.quality);
			SwapL2N(v.release_id);
			SwapL2N(v.status.word);
			SwapL2N(v.version_id);
			for (size_t k=0; k <  ( sizeof(v.index_marks_1) +
								   sizeof(v.index_marks_2) +
								   sizeof(v.index_marks_3) ) / sizeof(dword_t); ++k) 
			{
				SwapL2N( v.index_marks_1[k]); 
			}
		}
		template<>
		void SwapN2L(DssFileFormat::v2::common_header_t& v) {
			if (1 == v.version_id) {
				SwapN2L(* (v1::header_map_t *)&v );
				return;
			}
			SwapN2L(v.attribute_flag);
			SwapN2L(v.dss_status.flags);
			SwapN2L(v.ex_rec_length);
			SwapN2L(v.header_count);
			SwapN2L(v.job_number);
			SwapN2L(v.licensee_id);
			SwapN2L(v.num_of_index_mark);
			SwapN2L(v.object.word);
			SwapN2L(v.playback_position);
			SwapN2L(v.priority_level);
			SwapN2L(v.priority_status);
			SwapN2L(v.process.word);
			SwapN2L(v.quality);
			SwapN2L(v.release_id);
			SwapN2L(v.status.word);
			SwapN2L(v.version_id);
			for (size_t k=0; k <  ( sizeof(v.index_marks_1) +
								   sizeof(v.index_marks_2) +
								   sizeof(v.index_marks_3) ) / sizeof(dword_t); ++k) 
			{
				SwapN2L( v.index_marks_1[k]); 
			}
		}
		

		template<> void SwapN2L(v2::wav_common_header_t& v)
		{
			SwapN2L(v.riff.ckSize);
			SwapN2L(v.fmt.waveformat_ex);
			SwapN2L(v.fmt.ckSize);
			SwapN2L(v.olym.ckSize);
			if (v.olym.common_header.version_id == 1) {
				SwapN2L(*(v1::header_map_t *)&v.olym.common_header);
			}
			else {
				SwapN2L(v.olym.common_header);
			}
		}
		template<> void SwapL2N(v2::wav_common_header_t& v)
		{
			SwapL2N(v.riff.ckSize);
			SwapL2N(v.fmt.waveformat_ex);
			SwapL2N(v.fmt.ckSize);
			SwapL2N(v.olym.ckSize);
			DssFileFormat::word_t version = v.olym.common_header.version_id;
			SwapL2N(version);
			if (version == 1) {
				SwapL2N(*(v1::header_map_t *)&v.olym.common_header);
			}
			else {
				SwapL2N(v.olym.common_header);
			}
		}
#endif
	}

};
