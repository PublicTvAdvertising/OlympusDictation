//
// Portions of this file are derived from GDCL's parser sample, copyright follows
//
// Copyright (c) GDCL 2004. All Rights Reserved. 
// You are free to re-use this as the basis for your own filter development,
// provided you retain this copyright notice in the source.
// http://www.gdcl.co.uk
//
//////////////////////////////////////////////////////////////////////

#include "stdafx_ff.h"
#include "DssElement.h"

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

namespace DssFileFormat {

DssElement::DssElement(DssCryptor& cryptor)
: m_cBytes(0)
  , m_Cryptor(cryptor)
  , m_pItem(reinterpret_cast<byte_t *>(&m_psItem[0]))
  , m_cHdr(0)
  , m_elementId(DSS_UNKNOWN_ELEMENT)
{
	::memset(m_pItem,0xff,DssFileFormat::SIZE_DSS_SECTOR);
}

DssElement::~DssElement() {
}

LONG DssElement::Length()
{ 
	return m_cBytes;
}
const byte_t* DssElement::Data()
{
	return m_pItem;
}
const byte_t* DssElement::Payload()
{
	return m_pItem + m_cHdr;
}
long DssElement::PayloadLength()
{
	return m_cBytes - m_cHdr;
}

bool 
DssElement::Parse(const byte_t* pData, LONG cBytes, bool is_encrypted)
{
	if (cBytes < SIZE_FRAMEBLOCK_HEADER)
	{
		return false;
	}
	m_cBytes = min(cBytes,SIZE_DSS_SECTOR);
	::memcpy((void *)(&m_pItem[0]),pData,m_cBytes);
	return true;
}
DSS_ELEMENT_ID
DssElement::GetElementType() {
	return m_elementId;
}
};
