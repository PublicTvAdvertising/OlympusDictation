#ifndef DSS_FILE_FORMAT_H__
#define DSS_FILE_FORMAT_H__

#include <string>

#ifdef WIN32

//// {5F3816B8-9F73-4998-8704-5F90E76967E2}
//DEFINE_GUID(FORMAT_DssRawFormat,
//0x5f3816b8, 0x9f73, 0x4998, 0x87, 0x4, 0x5f, 0x90, 0xe7, 0x69, 0x67, 0xe2);
//
//// {427032AF-D08A-4a73-90A8-4D4B6ED707C7}
//DEFINE_GUID(FORMAT_DssStreamFormat, 
//0x427032af, 0xd08a, 0x4a73, 0x90, 0xa8, 0x4d, 0x4b, 0x6e, 0xd7, 0x7, 0xc7);


#else
# define ULONG64 uint64_t
#endif

struct DSSSTREAMFORMAT {
	int		nSamplesPerSec;
	int		nBitrate;
	int		nMode;
} ;

namespace DssFileFormat 
{

	typedef	char			ascii_t;
	typedef unsigned char	byte_t;
	typedef unsigned short	word_t;
	typedef int				dword_t;
	typedef unsigned int	udword_t;

	enum {
		VERSION_UNK			= 0x0000
		, VERSION_DSS		= 0x0001	
		, VERSION_DSS_PRO	= 0x0002
	};

	enum {
		SIZE_DSS_SECTOR			= 512,
		SIZE_DSS_WMA_COMMON_HEADER = SIZE_DSS_SECTOR-30,
		SIZE_DSS_HEADER			= 134,
		SIZE_DS2_HEADER			= 192,
		SIZE_ENC_HEADER			= 22,
		SIZE_FRAMEBLOCK_HEADER	= 6,
		SIZE_INSTRUCTION_MARK	= 48,
		SIZE_ADDITIONAL_INSTRUCTION_MARK = 192,
	};

	typedef enum 
	{
		SP_NO_SCVA = 0,
		SP_WITH_SCVA,
		LP_NO_SCVA,
		LP_WITH_SCVA,
		MLP_NO_SCVA,
		MLP_WITH_SCVA,
		QP_NO_SCVA,
		QP_WITH_SCVA,
	} compression_mode_t;

	typedef enum
	{
		DSS_SP_SCVA_OFF		= 0x0,
		DSS_SP_SCVA_ON		= 0x1,
		DSS_LP_SCVA_OFF		= 0x2,
		DSS_LP_SCVA_ON		= 0x3,
		DSS_QP_SCVA_OFF		= 0x6,
		DSS_QP_SCVA_ON		= 0x7,
		WMA_LP				= 0x14,
		WMA_SP				= 0x16,
		WMA_HQ				= 0x10,
		WMA_SSP				= 0x17,
		WMA_SHQ				= 0x11,
		WMA_SXQ				= 0x13,
	} ex_quality_t;

	typedef enum 
	{
		DSS_FILE_HEADER				= 0,
		DSS_BLOCK_HEADER,
		DSS_FRAME_HEADER,
		DSS_UNKNOWN_ELEMENT
	} DSS_ELEMENT_ID;

	typedef enum {
		DSS_COMMON_HEADER			= 0,
		DSS_OPTIONAL_HEADER,
		DSS_PRO_OPTIONAL_HEADER,
		DSS_PRO_OPTIONAL_HEADER2,
		DSS_UNKNOWN_HEADER
	} DSS_HEADER_TYPE;

	enum 
	{
		SP_BITRATE		= 13700,
		QP_BITRATE		= 28000,
		LP_BITRATE		= 6300,
		LP_FS			= 8000,
		SP_FS			= 12000,
		QP_FS			= 16000,
		LP_PCM_SAMPLES	= 240,
		SP_PCM_SAMPLES	= 288,
		QP_PCM_SAMPLES	= 256,
	};

#pragma pack(push,1)

	struct blocksector_imark_t {
		word_t		block_offset;
		byte_t		frame_offset;
	};
	struct blocksector_additionalimark_t {
		dword_t		block_offset;
		word_t		frame_offset;
	};
	struct file_header_t {
		byte_t		number_of_header_blocks;
		byte_t		self_identifier					[3];
		word_t		version_id_dss;
		word_t		release_id_dss;
		dword_t		licensee_id;
		ascii_t		author_id						[16];
		dword_t		job_number;
		word_t		object_word;
		word_t		process_word;				
		word_t		status_word;
		ascii_t		record_start_date				[6];
		ascii_t		record_start_time				[6];
		ascii_t		record_end_date					[6];
		ascii_t		record_end_time					[6];
		ascii_t		length_of_recording				[6];
		byte_t		attribute_flag;
		byte_t		priority_level;
		ascii_t		transcriptionist_id				[16];
		union {
			byte_t		instruction_mark			[48];		//16 x 3 byte entries
			blocksector_imark_t	blocksector_marks	[16];
		};
	} ;

	struct file_header_ds2_t {
		union {
			byte_t		additional_instruction_mark		[192];		//32 x 6 byte entries
			blocksector_additionalimark_t blocksector_marks[32];
		};
	} ;
	
	struct file_header_enc_t {
		word_t		encryption_version;
		byte_t		encryption_salt					[16];
		byte_t		verification_value				[4];
	} ;

	struct extended_index_mark_t {
		union {
			byte_t		sector_offset[4];
			dword_t		dw_sector_offset;
		};
		union {
			byte_t		frame_offset[2];
			word_t		w_frame_offset;
		};
		union {
			byte_t		position[4];
			dword_t		dw_position;
		};
	} ;

	struct optional_header_t {
		dword_t	ex_rec_length;
		extended_index_mark_t	ex_index_mark			[16];
		byte_t		quality;
		byte_t		reserved_1							[15];
		ascii_t		work_type_id						[16];
		ascii_t		option_item_name1					[8];
		ascii_t		option_item_id1						[20];
		ascii_t		option_item_name2					[8];
		ascii_t		option_item_id2						[20];
		ascii_t		option_item_name3					[8];
		ascii_t		option_item_id3						[20];
		byte_t		dss_status;
		byte_t		priority_status;
		dword_t		playback_position;
		ascii_t		notes								[100];
	} ;

	struct pro_option_item_t {
		ascii_t		option_item_name					[16];
		ascii_t		option_item_id						[20];
	};

	struct pro_optional_header_t {
		dword_t		ex_rec_length;
		dword_t		playback_position;
		byte_t		quality;
		byte_t		priority_status;
		ascii_t		work_type_id						[16];
		pro_option_item_t optional_items				[10];
		ascii_t		notes								[100];
		byte_t		reserved							[26];
	} ;

	struct pro_offset_t {
		udword_t		sector;
		word_t			frame;
	} ;
	struct pro_verbal_comment_t {
		pro_offset_t	start_offset;
		pro_offset_t	end_offset;
	} ;
	struct pro_optional_header2_t {
		pro_verbal_comment_t	verbal_comment			[32];
		byte_t					reserved				[128];
	} ;

	struct block_header_t{
		word_t				first_block_frame_pointer;
		word_t				fw_first_block_frame_pointer;
		byte_t				number_of_block_frames;
		byte_t				fw_number_of_block_frames;
		compression_mode_t	compression_id;
	} ;
#pragma pack(pop)

	int				get_samplerate_for_mode(int mode);
	int				get_bitrate_for_mode(int mode);
	int				get_framelength_for_mode(int mode);
	int				reduce_block_duration_by(byte_t *pData, int cbLength, int samples_to_reduce_by);
	LONGLONG		reduce_block_duration_by(byte_t *pData, int cbLength, LONGLONG llRefTime);

#pragma pack(push,1)
	template<long LENGTH, unsigned char FILL_CHAR>
	struct ascii_n {
		ascii_n() {
			memset(value,FILL_CHAR, sizeof(value));
		}
		void set(const std::string& instr, char fillChar=FILL_CHAR) {
			memset(value, fillChar, sizeof(char)*LENGTH);
			memcpy(value, instr.c_str(), std::min<size_t>(sizeof(value),instr.length()));
		}
		std::string operator=(const std::string& rhs) {
			set(rhs);
			return get();
		}
		std::string get() const {
			return std::string(&value[0],&value[sizeof(value)]);
		}
		std::string operator()() const {
			return get();
		}
		char value[LENGTH];
	};

	typedef ascii_n<3,0> ascii3_t;
	typedef ascii_n<4,0> ascii4_t;
	typedef ascii_n<6,0> ascii6_t;
	typedef ascii_n<8,0> ascii8_t;
	typedef ascii_n<10,0> ascii10_t;
	typedef ascii_n<12,0> ascii12_t;
	typedef ascii_n<16,0> ascii16_t;
	typedef ascii_n<20,0> ascii20_t;
	typedef ascii_n<100,0> ascii100_t;
	typedef ascii_n<16,0xff> ascii16xff_t;
	typedef ascii_n<16,' '> ascii16s_t;
	typedef ascii_n<100,' '> ascii100s_t;
	
	template<long COUNT, unsigned char FILL>
	struct rbyte_t : public ascii_n<COUNT, FILL> {};

#pragma pack(pop)

	namespace v2 {
		enum {
			DSS_MAX_INDEX_MARKS_R1 = 99
		};
#pragma pack(push,1)
		typedef struct common_header_t {
			byte_t			header_count;
			ascii3_t		self_identifier;
			word_t			version_id;
			word_t			release_id;
			dword_t			licensee_id;
			ascii16s_t		author_id;
			dword_t			job_number;
			union			
			{
				word_t			word;
				struct {
					bool		dictation:1;
					bool		voice_message:1;
				};
			}				object;
			union
			{
				word_t			word;
				struct {
					bool		recording:1;
					bool		playing:1;
					bool		typing:1;
					bool		autotranscribing:1;
					bool		archiving:1;
				};
			}				process;
			union			
			{
				word_t			word;
				struct		 
				{
					bool		recording:1;
					bool		playing:1;
					bool		typing:1;
					bool		autotranscribing:1;
				};
			}				status;
			ascii6_t		rec_start_date;
			ascii6_t		rec_start_time;
			ascii6_t		rec_end_date;
			ascii6_t		rec_end_time;
			ascii6_t		length;
			byte_t			attribute_flag;
			byte_t			priority_level;
			ascii16xff_t	typist_id;
			dword_t			ex_rec_length;
			byte_t			quality;
			rbyte_t<15,0xff> x_reserved_1;
			ascii16_t		work_type_id;
			ascii8_t		option_item_name1;
			ascii20_t		option_item_id1;
			ascii8_t		option_item_name2;
			ascii20_t		option_item_id2;
			ascii8_t		option_item_name3;
			ascii20_t		option_item_id3;
			union			 
			{
				byte_t			flags;
				struct {
					bool		author_download:1;
					bool		typist_download:1;
					bool		remote_dictation:1;
					bool		author_import:1;
					bool		typist_import:1;
					bool		send:1;
				};
			}				dss_status;
			byte_t			priority_status;
			dword_t			playback_position;
			ascii100_t		notes;
			dword_t			num_of_index_mark;
			dword_t			index_marks_1			[49];
			dword_t			index_marks_2			[128];
			dword_t			index_marks_3			[115];
		} common_header_t;

		//struct wma_common_header_t {
		//	struct {
		//		GUID			uuid;
		//		ULONG64			object_size;
		//		word_t			content_descriptor_count;
		//		word_t			descriptor_name_length;
		//		ascii_t			descriptor_name					[16];
		//		word_t			descriptor_value_data_type;
		//		word_t			descriptor_value_length;
		//	} extended_content_description;
		//	common_header_t		common_header;
		//	rbyte_t<22,0xff>	x_reserved_2;
		//} ;

		struct mp3_common_header_t {
			struct {
				ascii_t			id[3];
				word_t			version;
				byte_t			flags;
				dword_t			size;
			} id3;
			struct	{
				ascii_t			id[4];	// 'XOLY'
				dword_t			size;
				word_t			flags;
				common_header_t		common_header;
				rbyte_t<32,0xff>	x_reserved_2;
			} frame;
		} ;

		typedef struct waveformat_ex_t{
			word_t			wFormatTag;
			word_t			nChannels;
			dword_t			nSamplesPerSec;
			dword_t			nAvgBytesPerSec;
			word_t			nBlockAlign;
			word_t			wBitsPerSample;
		} waveformat_ex_t;
		
		
		struct wav_common_header_t {
			struct {
				ascii_t			ckID		[4];
				dword_t			ckSize;
				ascii_t			WAVE		[4];
			} riff;
			struct {
				ascii_t			ckID		[4];
				dword_t			ckSize;
				waveformat_ex_t waveformat_ex;
			} fmt;
			struct {
				ascii_t			ckID		[4];
				dword_t			ckSize;		
				common_header_t		common_header;
			} olym;
			struct {
				ascii_t			ckID		[4];
				dword_t			ckSize;
			} data;
		} ;

#pragma pack(pop)
	}
	namespace v1 {
		
#pragma pack(push,1)
		typedef struct header_map_t {
			union {
				DssFileFormat::file_header_t		common_header;
				DssFileFormat::byte_t				block_0[512];
			};
			union {
				DssFileFormat::optional_header_t	optional_header;
				DssFileFormat::byte_t				block_1[512];
			};
		} header_map_t;
#pragma pack(pop)
	}
	typedef v1::header_map_t v1_header_map_t;

	namespace endian {
		template<typename T>
		void SwapL2N(T& v) {
		}
		template<typename T>
		void SwapN2L(T& v) {
		}
#if defined(TARGET_RT_BIG_ENDIAN) && TARGET_RT_BIG_ENDIAN
		template<> void SwapN2L(byte_t& v);
		template<> void SwapN2L(word_t& v);
		template<> void SwapN2L(dword_t& v);
		template<> void SwapL2N(byte_t& v);
		template<> void SwapL2N(word_t& v);
		template<> void SwapL2N(dword_t& v);

		template<> void SwapN2L(v2::waveformat_ex_t& v);
		template<> void SwapL2N(v2::waveformat_ex_t& v);

		template<> void SwapN2L(v1::header_map_t& v);
		template<> void SwapL2N(v1::header_map_t& v);
		template<> void SwapN2L(v2::common_header_t& v);
		template<> void SwapL2N(v2::common_header_t& v);
		
		template<> void SwapN2L(v2::wav_common_header_t& v);
		template<> void SwapL2N(v2::wav_common_header_t& v);
#endif
	}	
};

#endif
