#include "qp_abs.h"


#define ERROR_WRITING_OUTPUTFILE  {fprintf(stderr,"Error writing output file");return(1);}
#define ERROR_REPOS_OUTPUTFILE  {fprintf(stderr,"Error repositioning output file");return(1);}
#define WRITE_ADDITIONAL_HEADER

namespace QualityPlay {

static void init_wave_header(wave_header_type *wavhdr)
{
wavhdr->main_chunk   = 0x46464952;    // "RIFF"
wavhdr->total_length = 44 - 8;        // file length in bytes - 8
wavhdr->chunk_type   = 0x45564157;    // "WAVE"
return;
}

void init_format_chunk(DWORD fs, format_chunk_type *fmtchk)
{
fmtchk->sub_chunk     = 0x20746d66;              // "fmt "
fmtchk->format_length = 16;                      // length of format chunk is 16 bytes
fmtchk->format        = 1;                       // 1 for PCM data
fmtchk->modus         = 1;                       // 1 for mono
fmtchk->sample_fq     = fs;                      // sampling frequency in Hz
fmtchk->byte_p_spl    = 2;                       // 16 bit samples take 2 bytes, each
fmtchk->byte_p_sec    = fs * fmtchk->byte_p_spl; // bytes per second
fmtchk->bit_p_spl     = fmtchk->byte_p_spl * 8;  // 16 bit samples
return;
}

void init_data_chunk_header(DWORD dtalen, data_chunk_header_type *dtachkhdr)
{
dtachkhdr->data_chunk  = 0x61746164;  // "data"
dtachkhdr->data_length = dtalen;      // data length in bytes
return;
}



int read_wave_file(FILE *fp, wavfile_chunk_type *wct)
{
  /* read wav header to ensure proper input format and to correctly set sampling rate */
  if (fread(&(wct->wave_hdr), sizeof(wave_header_type), 1, fp) != 1)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": error reading header!\n\n", wct->WaveFileName);
    return(1);
    }

  if (wct->wave_hdr.main_chunk != 0x46464952 || wct->wave_hdr.chunk_type != 0x45564157)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": incompatible format!\n\n", wct->WaveFileName);
    return(1);
    }

  if (fread(&(wct->format_chk), sizeof(format_chunk_type), 1, fp) != 1)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": error reading format chunk!\n\n", wct->WaveFileName);
    return(1);
    }
  if (wct->format_chk.sub_chunk != 0x20746d66)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": incompatible format chunk!\n\n", wct->WaveFileName);
    return(1);
    }

  if (wct->format_chk.format != 1)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": must contain PCM data!\n\n", wct->WaveFileName);
    return(1);
    }
  
  if (wct->format_chk.modus != 1)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": must contain single channel data!\n\n", wct->WaveFileName);
    return(1);
    }
  
  if (wct->format_chk.byte_p_spl != 2 || wct->format_chk.bit_p_spl != 16)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": must contain 16-bit samples!\n\n", wct->WaveFileName);
    return(1);
    }

  switch(wct->format_chk.sample_fq)
    {
      #if 0
       case 8000:  break;
       case 11025: break;
       case 12000: break;
       case 22050: break;
      #endif
      case 16000: break;
      default: 
         {

            fprintf(stderr,"\nread_wave_file: input wav file \"%s\": sampling frequency must be 16 kHz!\n\n", wct->WaveFileName);
         #if 0
            fprintf(stderr,"\nread_wave_file: input wav file \"%s\": sampling frequency out of set!\n\n", wct->WaveFileName);
         #endif
         return(1);
         }
    }

  if (fread(&(wct->data_chk_hdr), sizeof(data_chunk_header_type), 1, fp) != 1)
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": error reading data chunk header!\n\n", wct->WaveFileName);
    return(1);
    }
  if (wct->data_chk_hdr.data_chunk != 0x61746164) /* "data" */
    {
    fprintf(ERROROUT,"\nread_wave_file: input wav file \"%s\": incompatible data chunk!\n\n", wct->WaveFileName);
    return(1);
    }

  return(0);
}



int init_wave_file(FILE *fp, wavfile_chunk_type *wct)
{
  long id;

  if (fseek(fp, 0L, SEEK_SET)) ERROR_REPOS_OUTPUTFILE

  init_wave_header(&(wct->wave_hdr));
  if (fwrite(&(wct->wave_hdr), sizeof(wave_header_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  init_format_chunk(16000L, &(wct->format_chk));
  if (fwrite(&(wct->format_chk), sizeof(format_chunk_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  init_data_chunk_header(0, &(wct->data_chk_hdr));
  if (fwrite(&(wct->data_chk_hdr), sizeof(data_chunk_header_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE


#ifdef WRITE_ADDITIONAL_HEADER
  wct->list_chk.list_chunk  = 0x5453494c;           // "LIST"
  wct->list_chk.list_length = 0;
  wct->info_chk.info_chunk  = 0x4f464e49;           // "INFO"

  /* copyright string */
  wct->icop_chk.icop_chunk  = 0x504f4349;           // "ICOP"
  strcpy(wct->icop_chk.icop_data, "2003 GRUNDIG");
  id = strlen(wct->icop_chk.icop_data) + 1;         // length
  if (id%2) 
    {
    id++; 
    wct->icop_chk.icop_data[id-1] = 0;
    }
  wct->icop_chk.icop_length=id;

  /* software package */
  wct->isft_chk.isft_chunk = 0x54465349;             // "ISFT"
  strcpy(wct->isft_chk.isft_data, "GRUNDIG CODER");
  id = strlen(wct->isft_chk.isft_data) + 1;         // length
  if (id%2) 
    {
    id++; 
    wct->isft_chk.isft_data[id-1] = 0;
    }
  wct->isft_chk.isft_length=id;
#endif
  return(0);
}







int write_wave_file(FILE *fp, wavfile_chunk_type *wct)
{
  long pos;

  pos = ftell(fp);
  wct->data_chk_hdr.data_length = pos - 44L;     // save end of speech data position

#ifdef WRITE_ADDITIONAL_HEADER
  if (fwrite(&(wct->list_chk), sizeof(list_chunk_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(&(wct->info_chk), sizeof(info_chunk_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE

  if (fwrite(&(wct->icop_chk.icop_chunk),  sizeof(long), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(&(wct->icop_chk.icop_length), sizeof(long), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(wct->icop_chk.icop_data,      sizeof(char), wct->icop_chk.icop_length, fp) != (size_t)wct->icop_chk.icop_length) ERROR_WRITING_OUTPUTFILE

  if (fwrite(&(wct->isft_chk.isft_chunk),  sizeof(long), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(&(wct->isft_chk.isft_length), sizeof(long), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(wct->isft_chk.isft_data,      sizeof(char), wct->isft_chk.isft_length, fp)!= (size_t)wct->isft_chk.isft_length) ERROR_WRITING_OUTPUTFILE

  /* write length to LIST chunk header */
  wct->list_chk.list_length = ftell(fp) - pos - sizeof(list_chunk_type);

  if (fseek(fp, pos, SEEK_SET)) ERROR_REPOS_OUTPUTFILE
  if (fwrite(&(wct->list_chk), sizeof(list_chunk_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
#endif

  /* update WAV header and DATA chunk header */
  if (fseek(fp, 0L, SEEK_END)) ERROR_REPOS_OUTPUTFILE
  wct->wave_hdr.total_length = ftell(fp) - 8;

  if (fseek(fp, 0L, SEEK_SET)) ERROR_REPOS_OUTPUTFILE
  if (fwrite(&(wct->wave_hdr), sizeof(wave_header_type), 1, fp)           != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(&(wct->format_chk), sizeof(format_chunk_type), 1, fp)        != (size_t)1) ERROR_WRITING_OUTPUTFILE
  if (fwrite(&(wct->data_chk_hdr), sizeof(data_chunk_header_type), 1, fp) != (size_t)1) ERROR_WRITING_OUTPUTFILE
  
  return(0);
}

}