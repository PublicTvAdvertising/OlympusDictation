#include <stdio.h>
#include <stdlib.h>

#define DWORD long
#define WORD short

namespace QualityPlay {

typedef struct {
   DWORD    main_chunk;
   DWORD    total_length;
   DWORD    chunk_type;
} wave_header_type;

typedef struct {
   DWORD    sub_chunk;
   DWORD    format_length;
   WORD     format;
   WORD     modus;
   DWORD    sample_fq;
   DWORD    byte_p_sec;
   WORD     byte_p_spl;
   WORD     bit_p_spl;
} format_chunk_type;


typedef struct {
   DWORD    data_chunk;
   DWORD    data_length;
} data_chunk_header_type;


typedef struct {
   DWORD    list_chunk;
   DWORD    list_length;
} list_chunk_type;

typedef struct {
   DWORD    info_chunk;
   //DWORD    info_length;
} info_chunk_type;

typedef struct {
   DWORD    icop_chunk;
   DWORD    icop_length;
   char     icop_data[256];
} icop_chunk_type;

typedef struct {
   DWORD    isft_chunk;
   DWORD    isft_length;
   char     isft_data[256];
} isft_chunk_type;


typedef struct {
  wave_header_type        wave_hdr;
  format_chunk_type       format_chk;
  data_chunk_header_type  data_chk_hdr;
  list_chunk_type         list_chk;
  info_chunk_type         info_chk;
  icop_chunk_type         icop_chk;
  isft_chunk_type         isft_chk;
  char                    WaveFileName[256];
} wavfile_chunk_type;



/* --- prototypes ----------------------------------------------------- */
//void init_wave_header(wave_header_type *wavhdr);
void init_format_chunk(DWORD fs, format_chunk_type *fmtchk);
void init_data_chunk_header(DWORD dtalen, data_chunk_header_type *dtachkhdr);

int init_wave_file ( FILE *fp, wavfile_chunk_type *wct);
int read_wave_file ( FILE *fp, wavfile_chunk_type *wct);
int write_wave_file ( FILE *fp, wavfile_chunk_type *wct);

}