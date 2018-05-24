#include "stdafx_ff.h"
#include <algorithm>
#include "DssElement.h"
#include "DssCryptor.h"
#include <math.h>

// The order of header includes has been changed

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif



namespace DssFileFormat {
    
    // -- buffer scanning ----------------------
    
    DssScanner::DssScanner()
    : m_cValid(0),
    m_idxCurrent(0)
    ,m_fsCopied(0)
    ,m_fsOverflow(0)
    ,m_hasReadHeader(false)
    ,blocks_processed_(0)
    ,cumulative_time_(0)
    ,mode_present_flag_(0)
    ,ignore_headers_(false)
    ,blockframebuf(reinterpret_cast<byte_t *>(&blockframebuf_w[0]))
    ,compression_mode_(-1)
    {
        timekeys_.push_back(timekey(0,0));
    }
    
    // copies incoming data to the parse buffer.
    long
    DssScanner::Fill(const BYTE* pData, long cBytes)
    {
        // discard the scanned data
        m_cValid -= m_idxCurrent;
        if (m_cValid > 0)
        {
            // copy remaining data to buffer head
            ::memmove(m_ParseBuffer, m_ParseBuffer + m_idxCurrent, m_cValid);
        } else if (m_cValid < 0)
        {
            //		  DbgLog((LOG_TRACE, 0, "buffer size error"));
            m_cValid = 0;
        }
        m_idxCurrent = 0;
        
        // copy in as much data as will fit
        long cThis = min(cBytes, (ParseBufferSize - m_cValid));
        //long cThis = min(0, 1);
        if (cThis > 0)
        {
            ::memcpy(m_ParseBuffer+m_cValid, pData, cThis);
            m_cValid += cThis;
        }
        
        // returns the number of bytes used
        return cThis;
    }
    
    //bool
    //DssScanner::SyncFill(IAsyncReader* pRdr, LONGLONG* pllPos)
    //{
    //	  // copy up buffer to free up processed space
    //	  Fill(NULL, 0);
    //
    //	  LONGLONG pos = *pllPos;
    //	  long cThis = ParseBufferSize - m_cValid;
    //
    //	  // validate read length against file size
    //	  LONGLONG total, actual;
    //	  pRdr->Length(&total, &actual);
    //	  if ((total - pos) < cThis)
    //	  {
    //		  cThis = long(total - pos);
    //	  }
    //	  if (cThis <= 0)
    //	  {
    //		  return false;
    //	  }
    //
    //	  HRESULT hr = pRdr->SyncRead(pos, cThis, &m_ParseBuffer[m_cValid]);
    //	  if (hr != S_OK)
    //	  {
    //		  return false;
    //	  }
    //
    //	file_pos_ = pos;
    //	  // update current position
    //	  *pllPos = pos + cThis;
    //
    //	  m_cValid += cThis;
    //	  return (cThis > 0);
    //}
    
    // finds the next sector in the buffer
    // Returns false if no more complete items present.
    DssElement*
    DssScanner::NextItem(int skip_bytes, LONGLONG time_reference, bool is_enabled_framestream)
    {
        DssElement *pLastEl = 0;
        while(m_idxCurrent + SIZE_FRAMEBLOCK_HEADER <= m_cValid)		// a sector must be at least long enough to sector header
        {
            if (!ignore_headers_ && headers_.size() == 0)
            {
                DssCommonHeader* pDssHeader =  new DssCommonHeader(m_Cryptor);
                
                if (pDssHeader->Parse(m_ParseBuffer+m_idxCurrent, m_cValid - m_idxCurrent))
                {
                    headers_.push_back(pDssHeader);
                    m_idxCurrent += pDssHeader->Length();
                    return static_cast<DssElement*>(pDssHeader);
                }
                else {
                    delete pDssHeader;
                    m_idxCurrent++;
                }
            }
            else if (!ignore_headers_ && headers_[0]->GetHeaderType() == DSS_COMMON_HEADER && static_cast<DssCommonHeader *>(headers_[0])->get_HeaderBlockNum() > headers_.size())
            {
                DssHeader* pDssHeader = 0;
                // If the common header says it's regular DSS file, process the optional header as a DSS optional header
                if (static_cast<DssCommonHeader *>(headers_[0])->get_VersionId() < 2)
                {
                    pDssHeader = new DssOptionalHeader(m_Cryptor);
                }
                // Otherwise process it as a DSS Pro optional header (or 3rd sector "verbal comment" header)
                else
                {
                    if (headers_.size() == 1) {
                        pDssHeader = new DssProOptionalHeader(m_Cryptor);
                    } else if (headers_.size() == 2) {
                        pDssHeader = new DssProOptionalHeader2(m_Cryptor);
                    }
                }
                
                // Try out the optional header created above
                if (pDssHeader && pDssHeader->Parse(m_ParseBuffer+m_idxCurrent, m_cValid - m_idxCurrent))
                {
                    headers_.push_back(pDssHeader);
                    m_idxCurrent += pDssHeader->Length();
                    return static_cast<DssElement*>(pDssHeader);
                }
                // If it doesn't parse, then just parse as a plain "unknown" header type and store that
                else {
                    delete pDssHeader;
                    pDssHeader = new DssHeader(m_Cryptor);
                    if (pDssHeader->Parse(m_ParseBuffer+m_idxCurrent, m_cValid - m_idxCurrent)) {
                        headers_.push_back(pDssHeader);
                        m_idxCurrent += pDssHeader->Length();
                        return static_cast<DssElement*>(pDssHeader);
                    }
                    // If for some reason that doesn't parse, just don't use the data at all
                    else {
                        delete pDssHeader;
                        pDssHeader = 0;
                        m_idxCurrent++;
                    }
                }
            }
            else {
                current_time_ = time_reference;
                DssBlock* pDssBlock = new DssBlock(m_Cryptor, blocks_processed_++, current_time_);
                // Try parsing/decrypting the sector as a data block
#ifdef _DEBUG
                if (m_cValid - m_idxCurrent < SIZE_DSS_SECTOR) {
                    bool debug_stop = true;
                }
#endif
                if (pDssBlock->Parse(	m_ParseBuffer+m_idxCurrent,
                                     m_cValid-m_idxCurrent,
                                     GetCommonHeader()->IsEncrypted(),
                                     is_enabled_framestream || (blocks_processed_ == 1)
                                     ))
                {
                    // If it is a block, store it in our block list
                    blocks_.push_back(pDssBlock);
                    // Mark the compression mode used as present in the stream
                    compression_mode_ = (int)pDssBlock->get_CompressionMode();
                    mode_present_flag_ |= (1 << compression_mode_);
                    // keep track of the overflow
                    int fragment = m_fsOverflow;
                    if (is_enabled_framestream)
                    {
                        // read all the block frames from the current sector into a temporary buffer
                        word_t rfres = pDssBlock->ReadBlockFrames(blockframebuf, SIZE_DSS_SECTOR, m_fsCopied, m_fsOverflow, skip_bytes);
                        
                        if (rfres&DssBlock::BLOCK_IS_DISCONTINUOUS)	{
                            // the discontinuity occurs before the current block, thus cauterize(-1)
                            frame_stream_.CauterizePayload();
                            fragment = 0;
                        }
                        if (rfres&DssBlock::BLOCK_HAS_DATA)
                            // and then pump them into the block-aligned framestream
                        {
                            frame_stream_.Fill(blockframebuf, m_fsCopied, fragment, pDssBlock->get_CompressionMode(), pDssBlock->get_BlockFrameBytes(), pDssBlock->GetPartialFrameOffset(), pDssBlock->GetTimeOffset(), pDssBlock->GetActualNumberOfBlockFrames());
                        }
                    }
                    m_idxCurrent += pDssBlock->Length();
                    return static_cast<DssElement *>(pDssBlock);
                }
            }
        }
        return 0;
    }
    
    void DssScanner::SetIgnoreHeaders(bool ignore)
    {
        ignore_headers_ = ignore;
        if (ignore_headers_ == true && headers_.size() == 0)
        {
            DssCommonHeader *fh = new DssCommonHeader(m_Cryptor);
            fh->set_HeaderBlockNum(0);
            headers_.push_back(fh);
        }
    }
    int DssScanner::GetFileHeaderBytes() {
        int bytes = 0;
        if (!ignore_headers_)
        {
            std::vector<DssHeader *>::iterator ih = headers_.begin();
            while (ih != headers_.end()) {
                bytes += (*ih)->Length();
                ++ih;
            }
        }
        return bytes;
    }
    
    DssBlock* DssScanner::GetBlock() {
        if (blocks_.size() > 0) {
            return blocks_.front();
        }
        return 0;
    }
    DssCommonHeader* DssScanner::GetCommonHeader() {
        if (headers_.size() > 0) {
            return static_cast<DssCommonHeader *>(headers_[0]);
        }
        return 0;
    }
    unsigned char* DssScanner::GetRawHeader(unsigned int i) {
        if (headers_.size() > i) {
            return (unsigned char *)(headers_[i]->Data());
        }
        return 0;
    }
    DssHeader* DssScanner::GetIndexedHeader(unsigned int i) {
        if (headers_.size() > i) {
            return static_cast<DssHeader *>(headers_[i]);
        }
        return 0;
    }
    DssScanner::~DssScanner() {
        Empty();
    }
    
    void
    DssScanner::FlushBlocks(bool reset_timer)
    {
        m_cValid = m_idxCurrent = m_fsOverflow = 0;
#if 0
        if (blocks_.size() > 0) {
            std::for_each(blocks_.begin(), blocks_.end(), delete_object());
            blocks_.clear();
        }
#else
        std::list<DssBlock *>::iterator ib = blocks_.begin();
        while (ib != blocks_.end()) {
            delete *ib;
            ++ib;
        }
        blocks_.clear();
#endif
        frame_stream_.Clear();
        if (reset_timer) {
            blocks_processed_ = 0;
            current_time_ = 0;
        }
    }
    
    // discards all data
    void
    DssScanner::Empty()
    {
        m_cValid = m_idxCurrent = m_fsOverflow = blocks_processed_ = 0;
        m_hasReadHeader = false;
        
        std::vector<DssHeader *>::iterator ih = headers_.begin();
        while (ih != headers_.end()) {
            delete *ih;
            ++ih;
        }
        headers_.clear();
        std::list<DssBlock *>::iterator ib = blocks_.begin();
        while (ib != blocks_.end()) {
            delete *ib;
            ++ib;
        }
        blocks_.clear();
        frame_stream_.Clear();
        timekeys_.clear();
        timekeys_.push_back(timekey(0,0));
        cumulative_time_ = 0;
        current_time_ = 0;
        mode_present_flag_ = 0;
    }
    
    void
    DssScanner::MakeTimeIndex(bool isEOF)
    {
        LONGLONG cumultime = 0;
        LONGLONG cumultime_m1 = 0;
        int		nFramesPrev = 0;
        int		nBlocksInBuffer = (int)blocks_.size();
        int		nInnerBlocks = 0;
        while (blocks_.size() > 0) {
            if (isEOF && blocks_.size() == 1)
            {
                byte_t szWhatever[512];
                int iCopied = 0, iOverflowed =0;
                blocks_.front()->ReadBlockFrames(szWhatever, 512, iCopied, iOverflowed);
                if (iOverflowed > 0) {
                    blocks_.front()->ReduceDurationBy(DssFileFormat::get_framelength_for_mode(blocks_.front()->get_CompressionMode()));
                }
            }
            DssBlock *pb = blocks_.front();
            cumultime_m1 = cumultime;
            cumultime += pb->GetDurationRefTime();
            // It may happen, particularly at the end of the file, that the number of frames per block has changed suddenly.
            // In this case we want to mark the point where this has happened (e.g. on the second to last block) before saving
            // the time marker on the final block. Otherwise estimates will be incorrectly biased high
            int nFramesNow = pb->GetActualNumberOfBlockFrames();
            if (nFramesPrev != 0 && fabs(nFramesPrev - nFramesNow) > 1)
            {
                timekey tkp(blocks_processed_-nBlocksInBuffer + nInnerBlocks, cumulative_time_ + cumultime_m1);
                if (timekeys_.size() > 0 && timekeys_.back().block_number_ != tkp.block_number_) {
                    timekeys_.push_back(tkp);
                }
            }
            nFramesPrev = nFramesNow;
            delete pb;
            blocks_.pop_front();
            ++nInnerBlocks;
        }
        cumulative_time_ += cumultime;
        timekey tk(blocks_processed_, cumulative_time_);
        if (timekeys_.size() > 0 && timekeys_.back().block_number_ != tk.block_number_) {
            timekeys_.push_back(tk);
        }
    }
    
    void
    DssScanner::MakeTimeIndex(LONGLONG llForcedTime, LONGLONG llBytes)
    {
        if (llForcedTime > cumulative_time_) {
            cumulative_time_ = llForcedTime;
        }
        timekey tk(int((llBytes - (LONGLONG)this->GetFileHeaderBytes())/SIZE_DSS_SECTOR), cumulative_time_);
        timekeys_.push_back(tk);
    }
    int
    DssScanner::ExportTimeIndices(byte_t *pBuf, size_t bufsize)
    {
        size_t bytes_required = timekeys_.size()*sizeof(timekey);
        if (0 == pBuf) {
            return (int)bytes_required;
        }
        else if (bufsize >= bytes_required)
        {
            timekey *pKeys = reinterpret_cast<timekey *>(pBuf);
            for (size_t k=0; k < timekeys_.size(); ++k)
            {
                pKeys[k].block_number_ = timekeys_[k].block_number_;
                pKeys[k].time_ = timekeys_[k].time_;
            }
            return (int)bytes_required;
        }
        return 0;
    }
    
    void
    DssScanner::ImportTimeIndices(byte_t *pBuf, size_t bufsize)
    {
        timekeys_.clear();
        size_t num_indices = bufsize/sizeof(timekey);
        timekey *pKeys = reinterpret_cast<timekey *>(pBuf);
        for (size_t k=0; k < num_indices; ++k)
        {
            // verify pKeys[k]'s validity?
            timekeys_.push_back(pKeys[k]);
        }
        if (timekeys_.size() > 0) {
            cumulative_time_ = timekeys_[timekeys_.size()-1].time_;
        } else {
            cumulative_time_ = 0;
        }
    }
    
    // functor used to order timekeys by time stamp
    struct TimekeyTimeLess :
	public binary_function<DssFileFormat::DssScanner::timekey, DssFileFormat::DssScanner::timekey, bool>
    {
        bool operator()(const DssFileFormat::DssScanner::timekey& lhs, const DssFileFormat::DssScanner::timekey& rhs) const
        {
            return lhs.time_ < rhs.time_;
        }
    };
    float
    DssScanner::GetBlockOffsetForTime(LONGLONG time)
    {
        timekey testtime(0,time);
        std::vector<timekey>::iterator i = upper_bound(timekeys_.begin(), timekeys_.end(), testtime, TimekeyTimeLess());
        
        if (timekeys_.size() <=1 ) {
            return 0;
        }
        else if (i == timekeys_.begin()) {
            double del = double(time)/double((i+1)->time_) * ((i+1)->block_number_);
            return float(del);
        } else {
            if (i == timekeys_.end()) {
                --i;
            }
            else if (i != timekeys_.begin() && (i-1)->time_ == time) {
                return (float)((i-1)->block_number_);
            }
            double alpha_time = (double)(i->time_ - (i-1)->time_);
            double alpha_block = (double)(i->block_number_ - (i-1)->block_number_);
            double delta_time = (double)(time - (i-1)->time_);
            double delta_block = (delta_time * alpha_block / alpha_time);
            return (float((i-1)->block_number_) + float(delta_block));
        }
        return 0;
    }
    
    LONGLONG 
    DssScanner::GetCalculatedPlaytime(){
        return cumulative_time_;
    }
    
    LONGLONG 
    DssScanner::GetCurrentTime() {
        return current_time_;
    }
    
    // Functor returns true of the given timekey has a block number greater than target_block_, false otherwise
    struct timekey_block_gt {
        timekey_block_gt(LONGLONG target_block_) : target_block_(target_block_)
        {
        }
        bool operator () (const DssFileFormat::DssScanner::timekey& key) {
            if (key.block_number_ > target_block_) {
                return true;
            }
            return false;
        }
        LONGLONG target_block_;
    };
    
    LONGLONG 
    DssScanner::GetTimeForFileOffset(LONGLONG offset)
    {
        if (offset < 0 || timekeys_.size() <= 1) {
            return offset;
        }
        LONGLONG offset_as_block = offset/SIZE_DSS_SECTOR;
        std::vector<timekey>::iterator i = std::find_if(timekeys_.begin(), timekeys_.end(), timekey_block_gt(offset_as_block));
        
        if (i == timekeys_.end() || i == timekeys_.begin()) {
            double del = double(offset_as_block)/double(timekeys_.back().block_number_) * (timekeys_.back().time_);
            return LONGLONG(del);
        }
        else 
        {
            double alpha_time = (double)(i->time_ - (i-1)->time_);
            double alpha_block = (double)(i->block_number_ - (i-1)->block_number_);
            double delta_block = (double)(offset_as_block - (i-1)->block_number_);
            double delta_time = delta_block * alpha_time / alpha_block;;
            return (i-1)->time_ + LONGLONG(delta_time);
        }
        return 0;
        
    }
    
    void 
    DssScanner::SetPassword(const char *szpassword) {
        password_ = std::string(szpassword);
        m_Cryptor.set_password(password_.c_str(), password_.length()+1);
    }
    
    bool 
    DssScanner::GetHasMode(int mode) {
        return (0 != (mode_present_flag_ & (1 << mode)));
    }
    unsigned short 
    DssScanner::GetModePresentFlag() {
        return mode_present_flag_;
    }
    
    // accessing the framestream
    const byte_t*
    DssScanner::GetPayload() {
        return frame_stream_.GetPayload();
    }
    int
    DssScanner::GetPayloadLength() {
        return frame_stream_.GetPayloadLength();
    }
    const LONGLONG
    DssScanner::GetPayloadStartTime() {
        return frame_stream_.GetPayloadStartTime();
    }
    int	
    DssScanner::GetPayloadCompressionMode() {
        return frame_stream_.GetPayloadCompressionMode();
    }
    void
    DssScanner::CauterizePayload(){
        m_fsOverflow = 0;
        frame_stream_.CauterizePayload();
    }
    bool
    DssScanner::IsPayloadReady() {
        return frame_stream_.IsPayloadReady();
    }
    void
    DssScanner::ReleasePayload() {
        frame_stream_.ReleasePayload();
        if (blocks_.size()) {
            delete blocks_.front();
            blocks_.pop_front();
        }
    }
    bool
    DssScanner::IsPartialPayloadReady()
    {
        return frame_stream_.IsPartialPayloadReady();
    }
    const byte_t*
    DssScanner::GetPartialPayload()
    {
        return frame_stream_.GetPartialPayload();
    }
    int
    DssScanner::GetPartialPayloadLength()
    {
        return frame_stream_.GetPartialPayloadLength();
    }
    void
    DssScanner::ReleasePartialPayload() {
        frame_stream_.ReleasePartialPayload();
    }
};
