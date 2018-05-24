#include "stdafx_ff.h"

#include "DssElement.h"


#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//silence error about truncating (void *) to int with reinterpret_cast<int>. in this case
//we really don't care because we actually only want to know whether the address is odd or even
#pragma warning(disable : 4311)			

namespace DssFileFormat {

	DssFrameStream::DssFrameStream()			
		: total_inputs_(0)
		, total_outputs_(0) 
	{
	}
	bool DssFrameStream::Fill(byte_t *pV, word_t size, word_t fragment_length, int compression_mode, int total_length, int partial_offset, LONGLONG start_time, int num_frames, bool is_mode_prefixed) 
	{
		DbgFramestreamLog(_T("DssFrameStream::Fill() -- start\n"));
		if (blocks_.size() > 0 && fragment_length > 0) {
			blocks_.back()->append_overflow(pV, fragment_length);
		}
		if (size-fragment_length > 0) 
		{
			blocks_.push_back(new processed_block(&pV[fragment_length], compression_mode, size-fragment_length, partial_offset, total_length, start_time, num_frames, is_mode_prefixed));
			++total_inputs_;
		}
		DbgFramestreamLog(_T("DssFrameStream::Fill() -- stop\n"));
		return true;
	}
	bool DssFrameStream::IsPayloadReady() 
	{
		if (blocks_.size() > 0 && blocks_.front()->is_ready()){
			return true;
		}
		return false;
	}
	bool DssFrameStream::IsPartialPayloadReady()
	{
		if (blocks_.size() > 0 && blocks_.front()->len_ > get_framelength_for_mode(blocks_.front()->mode_)) {
			return true;
		}
		return false;
	}
	int	 DssFrameStream::PayloadAvailable() 
	{
		if (blocks_.size() > 0) { //IsPayloadReady()) {
			return blocks_.front()->total_;
		}
		return 0;
	}
	int DssFrameStream::GetPayloadLength() {
		int avail = PayloadAvailable();
		if (avail > 0)
			return avail;
		else
			return 0;
	}
	int DssFrameStream::GetPartialPayloadLength() {
		if (blocks_.size() > 0) {
			return blocks_.front()->partial_offset_;
		}
		return 0;
	}
	const byte_t* DssFrameStream::GetPayload() 
	{
		return blocks_.front()->data_;
	}
	const byte_t* DssFrameStream::GetPartialPayload()
	{
		return blocks_.front()->data_;
	}
	const int DssFrameStream::GetPayloadCompressionMode() 
	{
		DbgFramestreamLog(_T("DssFrameStream::GetPayloadCompressionMode().\n"));
		if (blocks_.size() > 0) {
			return blocks_.front()->mode_;
		}
		return 0;
	}
	const LONGLONG DssFrameStream::GetPayloadStartTime() {
		DbgFramestreamLog(_T("DssFrameStream::GetPayloadStartTime().\n"));
		if (blocks_.size() > 0) {
			return blocks_.front()->start_time_;
		}
		return 0;
	}
	void DssFrameStream::ReleasePayload() 
	{
		DbgFramestreamLog(_T("DssFrameStream::ReleasePayload() -- start\n"));
		++total_outputs_;
		delete blocks_.front();
		blocks_.pop_front();
		DbgFramestreamLog(_T("DssFrameStream::ReleasePayload() -- stop\n"));
	}
	void DssFrameStream::ReleasePartialPayload()
	{
		DbgFramestreamLog(_T("DssFrameStream::ReleasePayload() -- start\n"));
		++total_outputs_;
		blocks_.front()->remove_from_frontb(blocks_.front()->partial_offset_ - 2);
		blocks_.front()->partial_offset_ = 2;
		DbgFramestreamLog(_T("DssFrameStream::ReleasePayload() -- stop\n"));
	}
	void DssFrameStream::CauterizePayload() {
		DbgFramestreamLog(_T("DssFrameStream::CauterizePayload() -- start\n"));
		if (blocks_.size() > 0) {
			blocks_.back()->cauterize();
		}
		DbgFramestreamLog(_T("DssFrameStream::CauterizePayload() -- stop\n"));
	}

	void DssFrameStream::Clear() 
	{
//		DbgFramestreamLog(_T("DssFrameStream::Clear() -- start\n"));
#if 0
		std::for_each(blocks_.begin(), blocks_.end(), delete_object());
		blocks_.clear();
#else
		while (blocks_.size() > 0) {
			delete blocks_.back();
			blocks_.pop_back();
		}
#endif
		total_inputs_ = 0;
		total_outputs_= 0;
//		DbgFramestreamLog(_T("DssFrameStream::Clear() -- stop\n"));
	}

	int	DssFrameStream::GetCumulativeBytes() {
		DbgFramestreamLog(_T("DssFrameStream::GetCumulativeBytes() -- start\n"));
		int nBytes = 0;
		for (std::list<processed_block *>::iterator i = blocks_.begin(); blocks_.end() != i; ++i) {
			nBytes += (*i)->len_;
		}
		DbgFramestreamLog(_T("DssFrameStream::GetCumulativeBytes() -- stop\n"));
		return nBytes;
	}
	// make the first element in the frame stream 506 bytes long...
	void DssFrameStream::Compact(bool is_eos) {
		DbgFramestreamLog(_T("DssFrameStream::Compact() -- start\n"));
		std::list<processed_block *>::iterator i = blocks_.begin();
		i++;
		for (;(blocks_.front()->len_ != SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER) && 
			  (i != blocks_.end()); i++) {
			int addition = min(SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER - blocks_.front()->len_, (*i)->len_);
			blocks_.front()->append_overflow((*i)->data_, addition);
			if ((*i)->remove_from_front(addition)) {
				processed_block *kill_me_now = (*i);
				delete kill_me_now;
				blocks_.erase(i);
				i = blocks_.begin();
			}
		}
		if (is_eos && blocks_.size() == 1 && blocks_.front()->len_ != SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER) 
		{
			blocks_.front()->len_ = SIZE_DSS_SECTOR - SIZE_FRAMEBLOCK_HEADER;
		}
		DbgFramestreamLog(_T("DssFrameStream::Compact() -- stop\n"));
	}
};
