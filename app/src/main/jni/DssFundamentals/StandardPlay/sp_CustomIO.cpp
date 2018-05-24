//#include <windows.h>
#include "sp_Abs.h"
#include "list"

#ifndef MIN
#define MIN(a,b) (((a)<(b)) ? (a) : (b))
#endif

struct dss_block {
	unsigned int block_length;
	short	*sraw_data;
	unsigned char *raw_data;
	unsigned int read_idx;

	dss_block(unsigned char *src, unsigned int cbLength = 512) : read_idx(0), raw_data(0), block_length(cbLength) {
		sraw_data = new short[cbLength/sizeof(short)];
		raw_data = (unsigned char *)&sraw_data[0];
		::memcpy(raw_data, src, cbLength);
	}
	~dss_block() {
		delete[] sraw_data;
	}
	int read(short *dst, unsigned int cwsize, unsigned int cwcount) {
		int num_read = 0;//MIN(cwsize*cwcount, 512-read_idx);
		if (cwsize == 2) {
			while (cwcount > 0 && num_read < block_length) {
				if (read_idx+2 <= block_length) {
					*dst++ = short(raw_data[read_idx]) | (short(raw_data[read_idx+1])<<8);
					read_idx += 2;
					num_read += 2;
				} else if (read_idx+1 <= block_length){
					*dst = short(raw_data[read_idx])<<8;
					read_idx++;
					num_read++;
				}
				--cwcount;
			}
		}
		else {
			num_read = MIN(cwsize*cwcount, 512-read_idx);
			::memcpy(dst, &raw_data[read_idx], num_read);
			if (cwsize*cwcount != num_read) {
				int break_condition = 1;
			}
			read_idx += num_read;
		}
		return num_read;
	}
	int remaining() {
		return block_length - read_idx;
	}
	const unsigned int length() {
		return block_length;
	}
	bool eof() {
		return (read_idx >= block_length);
	}
};


void IO_SetEOF(dsswork& w) {
	w.is_eof = true;
}
int IO_IsEOF(dsswork& w, FILE *fp) {
#ifndef DSHOW_IO
	int is_eof = 0;
	long aux = ftell(fp); 
	fseek(fp, 0, SEEK_END);
	is_eof = (aux == ftell(fp));
	fseek(fp, aux, SEEK_SET);
	return (is_eof); 
#else
	// are all input queues gone and the bit reservoir empty?
	return (w.in_queue.size() == 0 && StandardPlay::getbits(w, 0,-4) == 1);
#endif
}

void IO_Flush(dsswork& w) {
	std::list<dss_block *>::iterator i;

	i = w.out_queue.begin();
	while (i != w.out_queue.end()) {
		delete (*i);
		w.out_queue.pop_front();
		i = w.out_queue.begin();
	}
	i = w.in_queue.begin();
	while (i != w.in_queue.end()) {
		delete (*i);
		w.in_queue.pop_front();
		i = w.in_queue.begin();
	}
}
long IO_GetOffsetFromStart(FILE *fp) {
#ifdef DSHOW_IO
	return ftell(fp);
#else
	return 0;
#endif
}

long IO_GetTotalInputBytes(dsswork& w, FILE *fp) {
#ifndef DSHOW_IO
	long len;
	long aux = ftell(fp); 
	fseek(fp, 0, SEEK_END);
	len = ftell(fp);
	fseek(fp, aux, SEEK_SET);
	return len;
#else
	if (w.in_queue.size() > 0) {
		long total_bytes = 0;
		for (std::list<dss_block *>::iterator i = w.in_queue.begin(); i != w.in_queue.end(); ++i) {
			total_bytes += (*i)->remaining();
		}
		return total_bytes;
	}
	else 
		return 0;
#endif
}

LONG IO_Read(dsswork& w, FILE *fp, short *dst, unsigned int cwSize, unsigned int cwCount) {
#ifdef DSHOW_IO
	LONG bread = 0;
	long bytes_desired = cwSize*cwCount;
	while (w.in_queue.size() > 0 && bread < bytes_desired) {
		long bytes_read = (long)w.in_queue.front()->read(&dst[bread/2], cwSize, cwCount);
		bread += bytes_read;
		if (w.in_queue.front()->eof()) {
			delete w.in_queue.front();
			w.in_queue.pop_front();
		}
		cwCount -= bytes_read/cwSize;
	}
	return bread/cwSize;
#else
	fread(dst, cwSize, cwCount, fp);
#endif
}
void IO_PushToReadQueue(dsswork& w, unsigned char *src, unsigned int bytes) {
	w.in_queue.push_back(new dss_block(src, bytes));
}
long IO_Write(dsswork& w, FILE *fp, short *src, unsigned int cwSize, unsigned int cwCount) {
#ifdef DSHOW_IO
	w.out_queue.push_back(new dss_block((unsigned char*)(&src[0]), cwSize*cwCount));
	return w.out_queue.back()->length()/cwSize;
#else
	return fwrite(src, cwSize, cwCount, fp);
#endif
}

#ifdef DSHOW_IO
int IO_SaveOutQueue(dsswork& w, short *pbOutput, int outlen) {
	unsigned int offset = 0;
#else
extern "C" void IO_SaveOutQueue(FILE *fp) {
#endif
	std::list<dss_block *>::iterator i = w.out_queue.begin();

	while (i != w.out_queue.end()) {
		unsigned short *data = (unsigned short*)(&((*i)->raw_data[0]));
		unsigned int len = (*i)->length()/2;
#ifdef DSHOW_IO
		if (offset + len < (unsigned int)outlen) {
			::memcpy(&pbOutput[offset],data,len*2);
			offset += len;
		}
		else {
			break;
		}
#else
		fwrite(data, sizeof(short), len, fp);
#endif
		delete (*i);
		w.out_queue.pop_front();
		i = w.out_queue.begin();
	}
	return offset;
//	out_queue.clear();
}

#ifdef DSHOW_IO
int IO_SaveOutQueueB(dsswork& w, char *pbOutput, int outlen) {
	unsigned int offset = 0;
	std::list<dss_block *>::iterator i = w.out_queue.begin();

	while (i != w.out_queue.end()) {
		char *data = (char *)(&((*i)->raw_data[0]));
		unsigned int len = (*i)->length();
		if (offset + len < (unsigned int)outlen) {
			::memcpy(&pbOutput[offset],data,len);
			offset += len;
		}
		else {
			break;
		}
		delete (*i);
		w.out_queue.pop_front();
		i = w.out_queue.begin();
	}
	return offset;
//	out_queue.clear();
}
#endif
