//
// DssElement.h - DssElement and DssScanner derived from GDCL's parser sample, copyright as follows:
//
// Copyright (c) GDCL 2004. All Rights Reserved. 
// You are free to re-use this as the basis for your own filter development,
// provided you retain this copyright notice in the source.
// http://www.gdcl.co.uk
//
//////////////////////////////////////////////////////////////////////

#ifndef DSS_ELEMENT_H__
#define DSS_ELEMENT_H__

#include "stdafx_ff.h"
#include <vector>
#include <list>
#include <stdlib.h>

#include "DssFileFormat.h"
#include "DssCryptor.h"
#include "64bit.h"
#ifndef min
#define min(a,b)			(((a) < (b)) ? (a) : (b))
#endif


#include "AlignedMalloc.h"

// test end
namespace DssFileFormat
{
	// helper for for_each algorithm
	struct delete_object
	{
	  template <typename T>
	  void operator()(T *ptr){ delete ptr;}
	};

#pragma pack(push,16)
	// BitReader is a utility class which reads bits starting from the supplied address
	class BitReader{
	public: 
		BitReader(word_t* start, int start_bit_offset, int num_bytes, bool swap=false) 
			: source_(start)
			, total_bytes_(num_bytes)
			, offset_(0)
			, intemp_(0)
			, inbitmask_(0)
			, swap_(swap)
		{
			getbits(start_bit_offset);
		}
		BitReader(byte_t* start, int start_bit_offset, int num_bytes, bool swap=false) 
			: total_bytes_(num_bytes)
			, offset_(0)
			, intemp_(0)
			, inbitmask_(0)
			, swap_(swap)
		{
			if (((long)(start)&0x1) == 0) {			// modified for 64 bit
				source_ = (word_t *)start;
			} else {
				source_ = (word_t *)&start[-1];
				getbits(8);
			}
			ffwd(start_bit_offset);
		}
		LONG getbits(int nbits)
		{
			long		   mask;
			LONG		   result;
			int 		  n;

			int total_words = (total_bytes_)/sizeof(word_t);
			for (result=0, mask=1L<<(nbits-1), n=nbits; n>0; n--, mask>>=1)
			{
				if ((inbitmask_>>=1) == 0 && offset_ < total_words)
				{
					inbitmask_ = 0x8000;
					if (swap_) {
						byte_t *pbt = (byte_t*)(&source_[offset_]);
						intemp_ = (word_t(pbt[0]) << 8) | (word_t(pbt[1]));
						offset_++;
					} else
						intemp_ = source_[offset_++];
				}
				if (intemp_ & inbitmask_) 
					result |= mask;
			}
			return result;
		}
		void ffwd(int nbits) {
			while (nbits > 0) {
				getbits(min(nbits,sizeof(word_t)*8));
				nbits -= min(nbits,sizeof(word_t)*8);
			}
		}
		void change_source(word_t* start, int start_bit_offset, int num_bytes) {
			source_ = start;
			offset_ = 0;
			total_bytes_ = num_bytes;
			inbitmask_ = 0x1;
		}
		bool is_eof() {
			int total_words = total_bytes_/sizeof(word_t);
			return (offset_ >= total_words);
		}
	protected:
		bool			swap_;
		word_t*			source_;
		int				total_bytes_;;
		int				offset_;
		word_t			intemp_;
		word_t			inbitmask_;
	};
	// WordMover is a utility class for moving words so that they maintain their endianness
	class WordMover 
	{
	protected:
		word_t	read_word_;
		int		bytes_read_;
		byte_t* src_;
	public:
		WordMover(byte_t *pDst=0, byte_t* pSrc=0, int num_to_copy=0)
			: read_word_(0) 
			, bytes_read_(0)
			, src_(0)
		{
			if (pDst && pSrc && num_to_copy) {
				mix_to_dest(pDst, pSrc, num_to_copy);
			}
		}
		static void memmove_align(byte_t *pDst, byte_t* pSrc, int size) {
			WordMover wm;
			wm.mix_to_dest(pDst, pSrc, size);
		}
	protected:
		word_t read_word(bool swap = true) {
			if (swap)
				read_word_ = word_t(src_[0])<<8 | word_t(src_[1]);
			else
				read_word_ = word_t(src_[1])<<8 | word_t(src_[0]);
			src_ += 2;
			bytes_read_ = 2;
			return read_word_;
		}
		byte_t get_byte(bool swap = true) {
			if (bytes_read_ == 0) {
				read_word(swap);
			}
			byte_t ret = (read_word_&0xFF00) >> 8;
			read_word_ <<= 8;
			bytes_read_--;
			return ret;
		}
	public:
		void mix_to_dest(byte_t* pDst, byte_t* pSrc, int num_to_copy) {
			read_word_ = 0;
			bytes_read_ = 0;
			src_ = pSrc;

			//UNUSED VARIABLE word_t read = 0;
			bool odd_to_copy = ((num_to_copy % 2) != 0);
			bool src_aligned = (reinterpret_cast<long>(pSrc) & 0x1L) == 0;
			if (reinterpret_cast<long>(pDst) & 0x1L) {
				if (!src_aligned) {
					src_ -= 1;
					get_byte(false);
				}
				pDst[-1] = get_byte(false);
				num_to_copy--;
				pDst++;
				while (num_to_copy > 1) {
					pDst[1] = get_byte(false);
					pDst[0] = get_byte(false);
					pDst += 2;
					num_to_copy-=2;
				}
				if (num_to_copy) {
					if (reinterpret_cast<long>(pDst) & 0x1L) {
						*pDst++ = get_byte(src_aligned);
					} else {
						*pDst++; // = 0x00;
						*pDst = get_byte(src_aligned);
					}
				}
				return;
			}
			if (!src_aligned) {
				src_ -= 1;
				get_byte(src_aligned);
				while (num_to_copy > 0) {
					pDst[1] = get_byte(src_aligned);
					--num_to_copy;
					if (num_to_copy > 0) {
						pDst[0] = get_byte(src_aligned);
						--num_to_copy;
					}
					pDst += 2;
				}
				return;
			}
			while (num_to_copy > 1) {
				*pDst++ = get_byte();
				num_to_copy--;
			}
			if (odd_to_copy) {
				*pDst++;// = 0x00;
				*pDst++ = get_byte(false);
			} else {
				*pDst++ = get_byte(true);
			}
		}
	};

	// DssElement is the most basic structure on which DssFiles are formed.  
	// Otherwise known as a sector in DSS documentation.
    
    
    // test
    
    

	class DssElement
	{
	public:
		DssElement(DssCryptor& cryptor);
		virtual ~DssElement();

		virtual bool	Parse(const byte_t* pData, LONG cBytes, bool is_encrypted = false);
		virtual void	UpdateRaw(){;}
		LONG			Length();
		const byte_t*	Data();
		const byte_t*	Payload();
		long			PayloadLength();
		DSS_ELEMENT_ID	GetElementType();
	protected:
		DssCryptor&		m_Cryptor;
		DSS_ELEMENT_ID m_elementId;
		word_t m_psItem[SIZE_DSS_SECTOR/2+1];		//fairly portable way of making the byte array to be word aligned while still leaving it on the stack
		byte_t *m_pItem;
		LONG m_cBytes;
		long m_cHdr;
	};

	class DssHeader : public DssElement
	{
	public:
		DssHeader(DssCryptor &cryptor, DSS_HEADER_TYPE htype = DSS_UNKNOWN_HEADER);// : DssElement(), header_type_(htype) {m_cBytes = (SIZE_DSS_SECTOR);}
		virtual ~DssHeader();//{;}

		const DSS_HEADER_TYPE GetHeaderType() { 
			return header_type_;
		}
	protected:
#ifdef _DEBUG
		int	serial_no;
#endif
		DSS_HEADER_TYPE header_type_;
	};

	class DssCommonHeader : public DssHeader
	{
	public:
		DssCommonHeader(DssCryptor &cryptor);
		virtual	~DssCommonHeader();

		virtual bool			Parse(const byte_t* pData, LONG cBytes, bool is_encrypted = false);
		virtual void			UpdateRaw();
		const LONGLONG			GetLengthReferenceTime();
		bool					IsEncrypted();
		void					InitEncryption(const char* szPassword, const char* szSalt = 0, const word_t encVersion = 0);
		const file_header_t&	GetFileHeader();

	protected:
		file_header_t		header_;
		file_header_ds2_t	header_ds2_;
		file_header_enc_t	header_enc_;

		std::string			self_id_;
		std::string			author_id_;
		std::string			rec_start_date_;
		std::string			rec_start_time_;
		std::string			rec_end_date_;
		std::string			rec_end_time_;
		std::string			rec_length_;
		std::string			typist_id_;
	public:
		// === accessors
		const byte_t		get_HeaderBlockNum();
		void				set_HeaderBlockNum(byte_t);
		const std::string&	get_SelfIdentifier();
		void				set_SelfIdentifier(const std::string &);
		const word_t		get_VersionId();
		void				set_VersionId(word_t);
		const word_t		get_ReleaseId();
		void				set_ReleaseId(word_t);
		const dword_t		get_LicenseeId();
		const std::string&	get_AuthorId();							//from here
		void				set_AuthorId(const std::string &);
		const dword_t		get_JobNumber();
		void				set_JobNumber(dword_t);
		const word_t		get_ObjectWord();
		void				set_ObjectWord(word_t);
		const word_t		get_ProcessWord();
		void				set_ProcessWord(word_t);
		const word_t		get_StatusWord();
		void				set_StatusWord(word_t);					//to here must be taken from original file
		const std::string&	get_RecordingStartDate();
		void				set_RecordingStartDate(const std::string &);
		const std::string&	get_RecordingStartTime();
		void				set_RecordingStartTime(const std::string &);
		const std::string&	get_RecordingEndDate();
		void				set_RecordingEndDate(const std::string &);
		const std::string&	get_RecordingEndTime();
		void				set_RecordingEndTime(const std::string &);
		const std::string&	get_LengthOfRecording();
		void				set_Length(const std::string &);
		const byte_t		get_AttributeFlag();					//from here
		void				set_AttributeFlag(byte_t);
		const byte_t		get_PriorityLevel();
		void				set_PriorityLevel(byte_t);
		const std::string&	get_TypistId();
		void				set_TypistId(const std::string &);		//to here
		const byte_t*		get_IMark();
		void				set_IMark(const byte_t*);
		const byte_t*		get_AdditionalIMark();
		void				set_AdditionalIMark(const byte_t*);
		const word_t		get_EncryptionVersion();
		void				set_EncryptionVersion(word_t);
		const byte_t*		get_SaltValue();
		void				set_SaltValue(byte_t *);
		const byte_t*		get_VerificationValue();
		void				set_VerificationValue(byte_t *);

	};

	class IDssOptionalHeader
	{
	public:
		virtual bool					Parse(const byte_t* pData, LONG cBytes, bool is_encrypted) = 0;
		virtual void					UpdateRaw() = 0;

		virtual bool					IsDssProHeader(void) = 0;

		virtual const dword_t			get_ExRecLength() = 0;
		virtual void					set_ExRecLength(dword_t v) = 0;
		virtual const dword_t			get_PlaybackPosition() = 0;
		virtual void					set_PlaybackPosition(dword_t) = 0;
		virtual const byte_t			get_Quality() = 0;
		virtual void					set_Quality(byte_t) = 0;
		virtual const byte_t			get_PriorityStatus() = 0;
		virtual void					set_PriorityStatus(byte_t v) = 0;
		virtual const std::string&		get_WorkTypeId() = 0;
		virtual void					set_WorkTypeId(const std::string &v) = 0;
		virtual const std::string&		get_Notes() = 0;
		virtual void					set_Notes(const std::string& v) = 0;
	} ;

	class DssOptionalHeader : public DssHeader, public IDssOptionalHeader
	{
	public:
		DssOptionalHeader(DssCryptor &cryptor);
		virtual ~DssOptionalHeader(){;}
		virtual bool			Parse(const byte_t* pData, LONG cBytes, bool is_encrypted = false);
		virtual void			UpdateRaw();
		bool IsDssProHeader() { 
			return false; 
		}
	protected:
		optional_header_t	optional_header_;

		std::string		work_type_id_;
		std::string		option_item_name1_;
		std::string		option_item_id1_;
		std::string		option_item_name2_;
		std::string		option_item_id2_;
		std::string		option_item_name3_;
		std::string		option_item_id3_;
		std::string		notes_;

	public:
		// === accessors
		const dword_t			get_ExRecLength();												// we change this
		void					set_ExRecLength(dword_t v);										// ...
		extended_index_mark_t&	get_ExtendedIndexMark(int idx);									// ...
		void					set_ExtendedIndexMark(int idx, extended_index_mark_t& mark);	// ...
		udword_t				get_WMAIndexMark(int idx);										// ---
		const byte_t			get_Quality();													// ...
		void					set_Quality(byte_t);											// ...
		const std::string&		get_WorkTypeId();												// ...
		void					set_WorkTypeId(const std::string &v);									// ...
		const std::string&		get_OptionItemName1();
		void					set_OptionItemName1(const std::string &v);
		const std::string&		get_OptionItemId1();
		void					set_OptionItemId1(const std::string &v);
		const std::string&		get_OptionItemName2();
		void					set_OptionItemName2(const std::string &v);
		const std::string&		get_OptionItemId2();
		void					set_OptionItemId2(const std::string &v);
		const std::string&		get_OptionItemName3();
		void					set_OptionItemName3(const std::string &v);
		const std::string&		get_OptionItemId3();
		void					set_OptionItemId3(const std::string &v);
		const byte_t			get_DssStatus();
		void					set_DssStatus(byte_t v);
		const byte_t			get_PriorityStatus();
		void					set_PriorityStatus(byte_t v);
		const dword_t			get_PlaybackPosition();
		void					set_PlaybackPosition(dword_t);
		const std::string&		get_Notes();
		void					set_Notes(const std::string& v);
	};

	class DssProOptionalHeader : public DssHeader, public IDssOptionalHeader
	{
	public:
		DssProOptionalHeader(DssCryptor &cryptor);
		virtual ~DssProOptionalHeader(){;}
		virtual bool			Parse(const byte_t* pData, LONG cBytes, bool is_encrypted = false);
		virtual void			UpdateRaw();
		bool IsDssProHeader() { 
			return true; 
		}
	protected:
		pro_optional_header_t	optional_header_;

		std::string		work_type_id_;
		std::string		option_item_name_[10];
		std::string		option_item_id_[10];
		std::string		notes_;

	public:
		// === accessors
        void                    set_QualityAndWorkType(int mode,std::string workType); /* Accesor to set quality  and worktype   */


		const dword_t			get_ExRecLength();
		void					set_ExRecLength(dword_t v);
		const dword_t			get_PlaybackPosition();
		void					set_PlaybackPosition(dword_t);
		const byte_t			get_Quality();
		void					set_Quality(byte_t);
		const byte_t			get_PriorityStatus();
		void					set_PriorityStatus(byte_t v);
		const std::string&		get_WorkTypeId();
		void					set_WorkTypeId(const std::string &v);
		const std::string&		get_OptionItemName(size_t idx);
		void					set_OptionItemName(size_t idx, const std::string &v);
		const std::string&		get_OptionItemId(size_t idx);
		void					set_OptionItemId(size_t idx, const std::string &v);
		const std::string&		get_Notes();
		void					set_Notes(const std::string &notes);
	};

	class DssProOptionalHeader2 : public DssHeader
	{
	public:
		DssProOptionalHeader2(DssCryptor &cryptor);
		virtual ~DssProOptionalHeader2(){;}
		virtual bool			Parse(const byte_t* pData, LONG cBytes, bool is_encrypted = false);
		virtual void			UpdateRaw();
	protected:
		pro_optional_header2_t	optional_header_2_;
	public:
		const pro_verbal_comment_t&		get_VerbalComment(size_t idx);
		void							set_VerbalComment(size_t idx, const pro_verbal_comment_t& comment);
		void							set_VerbalComment(size_t idx, int start_sector = -1, int start_frame = -1, int end_sector = -1, int end_frame = -1);
	} ;

	class DssBlock : public DssElement
	{
	public:
		enum analysis_result_flags {
			BLOCK_HAS_DATA = 0x1,
			BLOCK_IS_DISCONTINUOUS = 0x2
		};
		DssBlock(DssCryptor &cryptor, int serial_num = -1, LONGLONG time_ = -1); 
		virtual ~DssBlock();

		// find an elementary start code within the payload
		virtual bool			Parse(const byte_t* pData, LONG cBytes, bool is_encrypted = false, bool enable_decryption = true);
		virtual void			UpdateRaw();
		
		// for encoder
		void					ForceFillPayload(const byte_t *pData, int cBytes  = SIZE_DSS_SECTOR-SIZE_FRAMEBLOCK_HEADER);
		int						GetSpillOverToNext();

		// decoder/general purpose
		void					AnalyzeBlockFrames(int overflow = 0, int skip_bytes = 0, int actual_length = SIZE_DSS_SECTOR);
		const block_header_t&	GetBlockHeader();
		word_t					ReadBlockFrames(byte_t* pDest, int dest_length, int& copied, int& overflow, int skip_bytes = 0);
		byte_t					GetActualNumberOfBlockFrames();
		word_t					GetActualBlockFramePointer();
		const LONGLONG			GetTimeOffset();
		LONGLONG				GetDurationRefTime();
		int						GetSampleRate();
		void					OffsetStartByTime(double fTimeInSeconds);
		bool					FailedDecryption();
		int						GetPartialFrameOffset();
		void					ReduceDurationBy(int samples);
		void					ReduceDurationBy(LONGLONG llRefTime);
		bool					Encrypt(DssCryptor *pCryptor = 0);
	protected:
		int						serial_num_;
		LONGLONG				time_offset_;
		int						block_frame_bytes_;
		word_t					raw_header_[3];
		byte_t					payload_[506];
		block_header_t			block_header_;
		int						partial_offset_;
		bool					failed_decryption_;

		LONGLONG m_pts;
		word_t	GetBitsForFrame(byte_t *, BitReader& breader);
	public:	
		// === accessors
		const word_t					get_FirstBlockFramePointer();
		void							set_FirstBlockFramePointer(word_t v);
		const word_t					get_FirstBlockFramePointerFW();
		void							set_FirstBlockFramePointerFW(word_t v);
		const byte_t					get_NumberOfBlockFrames();
		void							set_NumberOfBlockFrames(byte_t v);
		const byte_t					get_NumberOfBlockFramesFW();
		void							set_NumberOfBlockFramesFW(byte_t v);
		const compression_mode_t		get_CompressionMode();
		void							set_CompressionMode(compression_mode_t v);
		int								get_SerialNum() {return serial_num_;}
		int								get_BlockFrameBytes() {return block_frame_bytes_; }
	};

	using namespace std;

	class DssFrameStream
	{
	protected:
		enum {BUFFER_SIZE = 512*4,};

		struct processed_block {
			processed_block(byte_t *in, int mode, int len, int partial_offset, int total, LONGLONG start_time, int num_frames, bool is_mode_prefixed) 
				: start_time_(start_time)
				, mode_(mode)
				, num_frames_(num_frames)
				, partial_offset_(partial_offset+2)
				, is_cauterized_(false)
			{
//				data_ = static_cast<byte_t *>(::aligned_malloc(1026,2));
                data_ = static_cast<byte_t *>(::aligned_malloc1(1026,2));

				memset(data_, 0, sizeof(byte_t)*1026);
				if (is_mode_prefixed) {
					reinterpret_cast<word_t *>(&data_[0])[0] = mode;
					WordMover::memmove_align(&data_[2],in,min(1024,len+(len%2)));
					len_ = 2+len;
					total_ = 2+total;
				} else {
					WordMover::memmove_align(&data_[0],in,min(1024,len));
					len_ = len;
					total_ = total;
				}
			}
			~processed_block() {
//				::aligned_free(data_);
                ::aligned_free1(data_);

			}
			void append_overflow(byte_t *in, int len) {
				if (!is_cauterized_) {
					WordMover::memmove_align(&data_[len_], in, min(1024-len_,len));
					len_ += min(1024-len_,len);
					total_ = len_;
				}
			}
			bool remove_from_front(int len) {
				WordMover::memmove_align(data_, &data_[len], min(1024-len, len_-len));
				len_ -= len;
				total_ = len_;
				return (len_ <= 0);
			}
			bool remove_from_frontb(int len) {
				WordMover::memmove_align(data_, &data_[len], min(1024-len, len_-len));
				len_ -= len;
				total_ -= len;
				return (len_ <= 0);
			}
			void cauterize() {
				is_cauterized_ = true;
				if (mode_ == 0 || mode_ == 1) {
					if (0 == mode_) {
						int r = (len_-2)/41; //must be a multiple of 41
						len_ = 2 + r*41;
					}
					if (len_%2 == 1) {
						len_++;
					}
				}
				total_ = partial_offset_ = len_;
			}
			const bool is_cauterized() {
				return is_cauterized_;
			}
			bool is_ready() {
				return (len_ == total_);
			}
			bool		is_cauterized_;
			byte_t*		data_; //[1026];
			int			len_;
			int			partial_offset_;
			int			total_;
			int			mode_;
			int			num_frames_;
			LONGLONG	start_time_;
		};
	public:
		DssFrameStream(); 
		virtual ~DssFrameStream(){;}

		bool			Fill(byte_t *pV, word_t size, word_t fragment_length, int compression_mode, int total_length, int partial_offset, LONGLONG start_time, int num_frames, bool is_mode_prefixed = true);
		bool			IsPayloadReady();
		bool			IsPartialPayloadReady();
		int				PayloadAvailable();
		int				GetPayloadLength();
		int				GetPartialPayloadLength();
		const byte_t*	GetPayload();
		const byte_t*	GetPartialPayload();
		const int		GetPayloadCompressionMode();
		const LONGLONG	GetPayloadStartTime();
		void			ReleasePayload();
		void			ReleasePartialPayload();
		void			CauterizePayload();
		void			Clear();

		int				GetCumulativeBytes();
		void			Compact(bool is_eos = false);
	protected:
		std::list<processed_block *>	blocks_;
		std::list<LONGLONG>				block_start_times_;
		int								total_inputs_;
		int								total_outputs_;
	};

	class DssScanner
	{
	public:
		DssScanner();
		virtual ~DssScanner();

		long				Fill(const byte_t* pData, long cBytes);
//#ifndef DSS_NO_DSHOW_DEPENDENCY
//		bool				SyncFill(IAsyncReader* pRdr, LONGLONG* pllPos);
//#endif
		long				GetValidRemaining() const { return m_cValid - m_idxCurrent; };
		DssElement*			NextItem(int want_offset = 0, LONGLONG time_reference = 0, bool is_enabled_framestream = true);

		int					GetFileHeaderBytes();
		DssCommonHeader		*GetCommonHeader();
		unsigned char		*GetRawHeader(unsigned int);
		DssHeader			*GetIndexedHeader(unsigned int);
		DssBlock			*GetBlock();
		bool				GetHasMode(int mode);
		unsigned short		GetModePresentFlag();
		int					GetCompressionMode() {return compression_mode_;}

		const byte_t*		GetPayload();
		int					GetPayloadLength();
		const LONGLONG		GetPayloadStartTime();
		int					GetPayloadCompressionMode();
		void				CauterizePayload();
		bool				IsPayloadReady();
		void				ReleasePayload();
		bool				IsPartialPayloadReady();
		const byte_t*		GetPartialPayload();
		int					GetPartialPayloadLength();
		void				ReleasePartialPayload();
		void				SetIgnoreHeaders(bool ignore);

		void				SetPassword(const char *password);
		void				MakeTimeIndex(bool isEOF=false);
		void				MakeTimeIndex(LONGLONG llForcedTime, LONGLONG llFileSizeBytes);
		int					ExportTimeIndices(byte_t *pBuf, size_t bufsize);
		void				ImportTimeIndices(byte_t *pBuf, size_t bufsize);
		float				GetBlockOffsetForTime(LONGLONG time);
		LONGLONG			GetCalculatedPlaytime();
		LONGLONG			GetCurrentTime();
		LONGLONG			GetTimeForFileOffset(LONGLONG offset);

		// discards blocks in queue
		void				FlushBlocks(bool reset_timer = true);
		// discards all data
		void				Empty();

		DssCryptor&			GetCryptor() { return m_Cryptor; }

		struct timekey {
			timekey(int block_number, LONGLONG time) : block_number_(block_number) , time_(time) {;}
			int	block_number_;
			LONGLONG time_;
		};

	protected:
	private:
		enum
		{
			ParseBufferSize = 80 * 1024,
		};
		DssCryptor				m_Cryptor;
		byte_t m_ParseBuffer[ParseBufferSize];
		std::vector<DssHeader *> headers_;
		std::list<DssBlock *> blocks_;
		DssFrameStream frame_stream_;
		word_t blockframebuf_w[1012/2];
		byte_t *blockframebuf;
		int  m_fsCopied;
		int  m_fsOverflow;
		LONG m_cValid;
		LONG m_idxCurrent;
		bool m_hasReadHeader;
		int  blocks_processed_;

		LONGLONG				file_pos_;
		LONGLONG				cumulative_time_;
		LONGLONG				current_time_;
		std::vector<timekey>	timekeys_;
		unsigned short			mode_present_flag_;
		std::string				password_;
		bool					ignore_headers_;
		int						compression_mode_;
	};

	class DssBuilder {
	public:
		DssBuilder();
		virtual ~DssBuilder();

		bool				LoadHeadersFromRaw(const byte_t *pData, LONG cBytes);
		void				BuildHeaders(int num_headers, double version = 1.);
        void   BuildHeadersNew(int num_headers, double version ,int mode, std::string workType);//shaheen
		long				Fill(const byte_t* pData, LONG cBytes, int mode, LONGLONG start_time, bool is_eos);
		
		int					GetFileHeaderBytes();
		DssCommonHeader		*GetCommonHeader();
		DssOptionalHeader	*GetOptionalHeader();
		DssHeader			*GetHeaderByIndex(int num);
		const REFERENCE_TIME GetCumulativeTime();
		const int			GetCompressionMode();
		void				SetCompressionMode(compression_mode_t v);

		DssElement*			front();
		void				pop_front();

		void				SetPassword(const char *password);
		void				SetEncryptionVersion(const WORD version);
		DssCryptor&			GetCryptor() { return m_Cryptor; }

		// discards blocks in queue
		void FlushBlocks();
		// discards all data
		void Empty();

	protected:
	private:
		DssCryptor				m_Cryptor;
		int						m_fbOverflow;
		std::vector<DssHeader *> headers_;
		std::list<DssElement *> blocks_;
		DssFrameStream			frame_stream_;

		size_t					blocks_sent_;
		REFERENCE_TIME			cumulative_time_;
		LONGLONG				current_time_;
		std::string				password_;
		int						compression_mode_;
		int						frames_received_;
	};

	bool			is_element_common_header(DssElement *pel);
	bool			is_element_block_header(DssElement *pel);
	const LONGLONG	convert_hhmmss_to_reftime(const ascii_t* szt);
	void			convert_reftime_to_hhmmss(ascii_t* szt, size_t lenstr, REFERENCE_TIME time);
	int				lps_mode_get_framelength_bytes(char *input);
	void			init_decryption(const char *szpassword16, const byte_t *szsalt16, const byte_t *verification_key4, const unsigned int crypt_version = 1);

	#pragma pack(pop)

};

#endif // DSS_ELEMENT_H
