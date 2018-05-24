//
//  AlignedMalloc.cpp
//  DSSFileFormatPorted
//
//  Created by Bilal on 3/20/13.
//  Copyright (c) 2013 Bilal. All rights reserved.
//

#include "AlignedMalloc.h"
#include <assert.h>
#include <malloc.h>

// Alignment must be power of 2 (1,2,4,8,16...2^15)
void* aligned_malloc1(size_t size, size_t alignment) {
    assert(alignment <= 0x8000);
    uintptr_t r = (uintptr_t)malloc(size + --alignment + 2);
    uintptr_t o = (r + 2 + alignment) & ~(uintptr_t)alignment;
    if (!r) return NULL;
    ((uint16_t*)o)[-1] = (uint16_t)(o-r);
    return (void*)o;
}

void aligned_free1(void* p) {
    if (!p) return;
    free((void*)((uintptr_t)p-((uint16_t*)p)[-1]));
}
