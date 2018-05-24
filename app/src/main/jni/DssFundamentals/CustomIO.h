
#ifndef _CUSTOMIO_H
#define _CUSTOMIO_H

/* --- prototypes ----------------------------------------------------- */
int  IO_IsEOF(dsswork& w, FILE *fp);
long IO_GetOffsetFromStart(FILE *fp);
long IO_GetTotalInputBytes(dsswork& w, FILE *fp);
LONG IO_Read(dsswork& w, FILE *fp, short *dst, unsigned int cwSize, unsigned int cwCount);
long IO_Write(dsswork& w, FILE *fp, short *src, unsigned int cwSize, unsigned int cwCount);
void IO_Flush(dsswork& w);
void IO_PushToReadQueue(dsswork& w, unsigned char *src, unsigned int bytes);
#ifdef DSHOW_IO
int IO_SaveOutQueue(dsswork& w, short *pbOutput, int outlen);
int IO_SaveOutQueueB(dsswork& w, char *pbOutput, int outlen);
#else
void IO_SaveOutQueue(dsswork& w, FILE *fp);
#endif


#endif // #ifndef _CUSTOMIO_H
