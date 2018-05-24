//
//  AlignedMalloc.h
//  DSSFileFormatPorted
//
//  Created by Bilal on 3/19/13.
//  Copyright (c) 2013 Bilal. All rights reserved.
//


#ifndef DSSFileFormatPorted_AlignedMalloc_h
#define DSSFileFormatPorted_AlignedMalloc_h

#include <stdio.h>

// Alignment must be power of 2 (1,2,4,8,16...2^15)
void* aligned_malloc1(size_t size, size_t alignment) ;
void aligned_free1(void* p) ;

#endif
