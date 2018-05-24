#include<jni.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define HEADER_SIZE_IN_BYTES (44)
#define THRESHOLD (0.50)

int file_overwrite(char *src1, char *src2, long int pos);

// this function is called from DictateActivity to perform insert or overwrite task.
jlong JNICALL Java_com_olympus_dmmobile_recorder_DictateActivity_process(JNIEnv * env, jobject obj,jstring target,jstring source,jstring temp,jlong position,jint process_type)
{
	char *target_file,*source_file,*temp_file;
	long int pos;
	int insertOrOverwrite;
	pos=position;
	insertOrOverwrite=process_type;
	long int total_len=0;

	if(pos < HEADER_SIZE_IN_BYTES){
		pos = HEADER_SIZE_IN_BYTES;		//skip header
	}

	target_file=(*env)->GetStringUTFChars(env,target,0);
	source_file=(*env)->GetStringUTFChars(env,source,0);
	if(insertOrOverwrite==1){
		temp_file=(*env)->GetStringUTFChars(env,temp,0);
	}
	if(insertOrOverwrite==0){
		// perform overwrite task
		total_len=file_overwrite(target_file, source_file, pos);
	}else {
		// perform insertion task
		total_len=file_insert(target_file, source_file, pos, temp_file);
	}

	// return the length of audio data that has been inserted or overwritten
	return total_len;
}

// this function is called to overwrite audio at the given position in the target file
int file_overwrite(char *src1, char *src2, long int pos) {
	FILE *source = NULL;
	FILE *target = NULL;

	void *buffer = NULL;
	

	long int read_count = 0;
	long int write_count = 0;

	long int src_data_length = 0;
	long int trg_data_length = 0;

	// open the source file for read operation
	source = fopen(src2, "rb");
	if(source == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "unable to open file %s in read mode\n", src2);
		return 1;
	}

	// open target file for both read and write operation
	target = fopen(src1, "r+b");
	if(target == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "unable to open file %s in read-write mode\n", src1);
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
		}
		return 1;
	}

	// seek to the position in target file
	if(fseek(target, pos, 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to seek into pos: %ld in file %s\n", pos, src1);
		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
		}
		return 2;
	}


	if(fseek(source, HEADER_SIZE_IN_BYTES, 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to seek into pos: %ld in file %s\n", HEADER_SIZE_IN_BYTES, src2);
		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
		}

		return 2;
	}

	// 4k
	buffer = malloc(4 * 1024);
	if(buffer == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to allocate buffer of size %d bytes\n", (4 * 1024));
		return 3;
	}


	// read the data from source file and write to target file at pause position
	while(!feof(source)) {
		//read from source file
		read_count = fread(buffer, 1, 4 * 1024, source);
		if(ferror(source) != 0) {
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "error while reading data from %s\n", src2);
			if(fclose(target) != 0){
				//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
			}

			return 4;
		}

		if(read_count <= 0)
			continue;
		// write to target file at pause position
		write_count = fwrite(buffer, 1, read_count, target);


		if(read_count != write_count) {
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "mismatch in read and write count\n");

			if(fclose(target) != 0){
				//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
			}

			return 4;
		}

		fflush(target);
	}

	//update header

	if(fseek(target, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to seek into pos: %ld in file %s\n", (HEADER_SIZE_IN_BYTES - 4), src1);
		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
		}

		return 2;
	}

	if(fseek(source, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to seek into pos: %ld in file %s\n", (HEADER_SIZE_IN_BYTES - 4), src2);
		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
		}

		return 2;
	}
	{
		long int byte_01 = fgetc(target);
		long int byte_02 = fgetc(target);
		long int byte_03 = fgetc(target);
		long int byte_04 = fgetc(target);

		trg_data_length = (byte_04 << 24) | (byte_03 << 16) | (byte_02 << 8) | byte_01;


		byte_01 = fgetc(source);
		byte_02 = fgetc(source);
		byte_03 = fgetc(source);
		byte_04 = fgetc(source);

		src_data_length = (byte_04 << 24) | (byte_03 << 16) | (byte_02 << 8) | byte_01;


		if((pos + src_data_length) > trg_data_length) {
			trg_data_length = pos + src_data_length;

			if(fseek(target, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
				//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to seek into pos: %ld in file %s\n", (HEADER_SIZE_IN_BYTES - 4), src1);
				if(fclose(target) != 0){
					//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
				}
				if(fclose(source) != 0){
					//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
				}

				return 2;
			}
			
			fputc(trg_data_length & 0xFF, target);
			fputc((trg_data_length >> 8) & 0xFF, target);
			fputc((trg_data_length >> 16) & 0xFF, target);
			fputc((trg_data_length >> 24) & 0xFF, target);

			if(fseek(target, 4, 0) != 0) {
				//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to seek into pos: %ld in file %s\n", 4, src1);

				if(fclose(target) != 0){
					//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
				}
				if(fclose(source) != 0){
					//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
				}
				return 2;
			}
			
			fputc((trg_data_length + 36) & 0xFF, target);
			fputc(((trg_data_length + 36) >> 8) & 0xFF, target);
			fputc(((trg_data_length + 36) >> 16) & 0xFF, target);
			fputc(((trg_data_length + 36) >> 24) & 0xFF, target);

		}
	}

	if(fclose(source) != 0){
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src2);
	}

	if(fclose(target) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_overwrite", "failed to close file %s\n", src1);
		return 1;
	}


	// return the length of data which has been overwritten
	return src_data_length;
}

// construct temp file name to perform insertion
char *construct_temp_file_name(char *temp_dir) {
	// temporary file name
	char file_name[6] = ".temp";

	// under the assumption that temp_dir will end with the FILE_SEPARATOR
	char *temp_file_name = (char *) malloc((strlen(temp_dir) + strlen(file_name) + 1) * sizeof(char));

	strcpy(temp_file_name, temp_dir);
	strcat(temp_file_name, file_name);

	return temp_file_name;
}

// this function is called to insert the source file at a given position in the target file
// the insertion is performed in two ways depending on the pause position at target file.
// if the pause position is in second half, more efficient technique is used, else normal insertion technique is followed
int file_insert(char *src1, char *src2, long int pos, char *temp_dir) {
	FILE *source = NULL;
	FILE *target = NULL;

	// temporary file
	FILE *temp = NULL;
	char *temp_file_name = construct_temp_file_name(temp_dir);

	void *buffer = NULL;
	
	long int read_count = 0;
	long int write_count = 0;

	long int src_data_length = 0;
	long int trg_data_length = 0;

	source = fopen(src2, "rb");
	if(source == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "unable to open file %s in read mode\n", src2);
		return 1;
	}

	target = fopen(src1, "r+b");
	if(target == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "unable to open file %s in read-write mode\n", src1);
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
		}

		return 1;
	}

	temp = fopen(temp_file_name, "w+b");
	if(temp == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "unable to open file %s in read-write mode\n", temp_file_name);
		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
		}

		return 1;
	}
	if(fseek(target, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to seek into pos: %d in file %s\n", (HEADER_SIZE_IN_BYTES - 4), src1);
		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", " failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
		}
		if(fclose(temp) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
		}

		return 2;
	}

	if(fseek(source, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to seek into pos: %d in file %s\n", (HEADER_SIZE_IN_BYTES - 4), src2);

		if(fclose(target) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", " failed to close file %s\n", src2);
		}
		if(fclose(temp) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
		}

		return 2;
	}

	{
		long int byte_01 = fgetc(target);
		long int byte_02 = fgetc(target);
		long int byte_03 = fgetc(target);
		long int byte_04 = fgetc(target);

		trg_data_length = (byte_04 << 24) | (byte_03 << 16) | (byte_02 << 8) | byte_01;

		byte_01 = fgetc(source);
		byte_02 = fgetc(source);
		byte_03 = fgetc(source);
		byte_04 = fgetc(source);

		src_data_length = (byte_04 << 24) | (byte_03 << 16) | (byte_02 << 8) | byte_01;

	}

	long int buffer_size=4*1024;				// 4k
	buffer = malloc(buffer_size);
	if(buffer == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to allocate buffer of size %d bytes\n", (4 * 1024));
		return 3;
	}

	/*	Efficient Insertion Technique
	 *
	 * If the insert position is in the second half, the data from pause position to end of file is copied to temp file.
	 * This data read is smaller in size than reading the other part.
	 *
	 * */

	if(pos >= (trg_data_length * THRESHOLD)) {
		// seek to pause position
		if(fseek(target, pos, 0) != 0) {
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to seek into pos: %ld in file %s\n", pos, src1);
			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}

			return 2;
		}
	// read data from pause position and till end of audio file
	while(!feof(target)) {
		read_count = fread(buffer, 1,buffer_size, target);
		if(ferror(target) != 0) {
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "error while reading data from %s\n", src1);

			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}

			return 4;
		}

		if(read_count <= 0)
			continue;

		// write the buffer to temp file
		write_count = fwrite(buffer, 1, read_count, temp);
		if(read_count != write_count) {
			//__android_log_print(ANDROID_LOG_VERBOSE, "file process: insertion", "failed while writing to temp file, read count: %d - write count: %d\n", read_count,write_count);

			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}

			return 4;
		}

		fflush(temp);
	}
	// seek to pause position in target file
	if(fseek(target, pos, 0) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to seek into pos: %ld in file %s\n", pos, src1);
		if(fclose(target) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		}
		if(fclose(source) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
		}
		if(fclose(temp) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
		}

		return 2;
	}

	// read the data from source file and write at pause position in target file
	while(!feof(source)) {
		read_count = fread(buffer, 1,buffer_size, source);
		if(ferror(source) != 0) {
			//printf("Error: in method file_insert: error while reading data from %s\n", src2);

			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}

			return 4;
		}

		if(read_count <= 0)
			continue;

		write_count = fwrite(buffer, 1, read_count, target);

		if(read_count != write_count) {
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: insertion", "failed while writing source file to target, read count: %d - write count: %d\n", read_count,write_count);
			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}

			return 4;
		}

		fflush(target);
	}
	if(fclose(source) != 0){
		__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src2);
	}
	// seek to start position of temp file
	if(fseek(temp, 0, 0) != 0) {
		//printf("Error: in method file_insert: failed to seek into pos: %d in file %s\n", 0, temp_file_name);

		if(fclose(target) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		}
		if(fclose(temp) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
		}

		return 2;
	}
	// read the data from temp file and write at the end of target file
	while(!feof(temp)) {
		read_count = fread(buffer, 1,buffer_size, temp);
		if(ferror(temp) != 0) {
			//printf("Error: in method file_insert: error while reading data from %s\n", temp_file_name);

			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}
			return 4;
		}

		if(read_count <= 0)
			continue;

		write_count = fwrite(buffer, 1, read_count, target);

		if(read_count != write_count) {

			if(fclose(target) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
			}
			if(fclose(temp) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
			}
			return 4;
		}

		fflush(target);
	}


	if(fclose(temp) != 0){
	 // __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", temp_file_name);
	}

	// delete the temp file
	if(remove(temp_file_name) != 0) {
		// printf("Error: in method file_insert: failed to delete file %s\n", temp_file_name);
		return 5;
	}

	// seek to header position 40 and update the header
	if(fseek(target, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
		//printf("Error: in method file_insert: failed to seek into pos: %d in file %s\n", (HEADER_SIZE_IN_BYTES - 4), src1);

		if(fclose(target) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		}

		return 2;
	}
			
	trg_data_length += src_data_length;

	fputc(trg_data_length & 0xFF, target);
	fputc((trg_data_length >> 8) & 0xFF, target);
	fputc((trg_data_length >> 16) & 0xFF, target);
	fputc((trg_data_length >> 24) & 0xFF, target);

	if(fseek(target, 4, 0) != 0) {
		//printf("Error: in method file_insert: failed to seek into pos: %d in file %s\n", 4, src1);

		if(fclose(target) != 0){
			// __android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		}
		return 2;
	}
			
	fputc((trg_data_length + 36) & 0xFF, target);
	fputc(((trg_data_length + 36) >> 8) & 0xFF, target);
	fputc(((trg_data_length + 36) >> 16) & 0xFF, target);
	fputc(((trg_data_length + 36) >> 24) & 0xFF, target);


	if(fclose(target) != 0){
		//__android_log_print(ANDROID_LOG_VERBOSE, "file process: file_insert", "failed to close file %s\n", src1);
		return 1;
	}

	/* return the length of data that has been inserted to target file*/
	//return src_data_length;

	}
	else {

		/***************************
		/ Normal Insertion Technique
		/***************************/


		long int read_limit = 0;
		long int curr_pos = 0;

		if(fseek(target, 0, 0) != 0) {

			if(fclose(target) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
			}

			return 2;
		}

		// Read data from target file upto pause position
		while(!feof(target) && (curr_pos = ftell(target)) < pos) {
			if(curr_pos + (4 * 1024) > pos)
				read_limit = pos - curr_pos;
			else
				read_limit = (4 * 1024);

			read_count = fread(buffer, 1, read_limit, target);
			if(ferror(target) != 0) {
				// printf("Error: in method file_insert: error while reading data from %s\n", src1);

				if(fclose(target) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", src1);
				}
				if(fclose(source) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", src2);
				}
				if(fclose(temp) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
				}

				return 4;
			}

			if(read_count <= 0)
				continue;

			// write the data read from target file upto pause position
			write_count = fwrite(buffer, 1, read_count, temp);

			if(read_count != write_count) {
				//printf("Error: in method file_insert: mismatch in read and write count\n");

				if(fclose(target) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", src1);
				}
				if(fclose(source) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", src2);
				}
				if(fclose(temp) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
				}

				return 4;
			}

			fflush(temp);
	}
	

		// Read data from source file till end of file
		while(!feof(source)) {
		read_count = fread(buffer, 1, buffer_size, source);
		if(ferror(source) != 0) {
			//printf("Error: in method file_insert: error while reading data from %s\n", src2);

			if(fclose(target) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
			}

			return 4;
		}

		if(read_count <= 0)
			continue;

		// Write the data read from source file
		write_count = fwrite(buffer, 1, read_count, temp);

		if(read_count != write_count) {
			//printf("Error: in method file_insert: mismatch in read and write count\n");

			if(fclose(target) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", src1);
			}
			if(fclose(source) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", src2);
			}
			if(fclose(temp) != 0){
				// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
			}

			return 4;
		}

		fflush(temp);
	}
	if(fclose(source) != 0){
		 printf("Error: in method file_insert: failed to close file %s\n", src2);
	}

	// Read the remaining data in target file from pause position to end of file
		while(!feof(target)) {
			read_count = fread(buffer, 1, buffer_size, target);
			if(ferror(target) != 0) {
				// printf("Error: in method file_insert: error while reading data from %s\n", src1);

				if(fclose(target) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", src1);
				}
				if(fclose(temp) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
				}

				return 4;
			}

			if(read_count <= 0)
				continue;

		// Write the remaming data read from target file*/
			write_count = fwrite(buffer, 1, read_count, temp);
			//__android_log_print(ANDROID_LOG_VERBOSE, "NORMAL insertion technique ********", " DEB POINT - writing last part ");
			if(read_count != write_count) {
				//printf("Error: in method file_insert: mismatch in read and write count\n");

				if(fclose(target) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", src1);
				}
				if(fclose(temp) != 0){
					// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
				}

				return 4;
			}

			fflush(temp);
	}
	if(fclose(target) != 0) {
		// printf("Error: in method file_insert: failed to close file %s\n", src1);
		return 1;
	}

	// Delete the original wav file
	if(remove(src1) != 0) {
		// printf("Error: in method file_insert: failed to delete file %s\n", src1);
		return 5;
	}

	// Update the header of wav file

	if(fseek(temp, (HEADER_SIZE_IN_BYTES - 4), 0) != 0) {
		//printf("Error: in method file_insert: failed to seek into pos: %d in file %s\n", (HEADER_SIZE_IN_BYTES - 4), temp_file_name);

		if(fclose(temp) != 0){
			// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
		}

		return 2;
	}
			
	trg_data_length += src_data_length;

	int reminder=trg_data_length % 2;
	if(reminder>0){
		trg_data_length=trg_data_length-reminder;
	}

	fputc(trg_data_length & 0xFF, temp);
	fputc((trg_data_length >> 8) & 0xFF, temp);
	fputc((trg_data_length >> 16) & 0xFF, temp);
	fputc((trg_data_length >> 24) & 0xFF, temp);

	if(fseek(temp, 4, 0) != 0) {
		// printf("Error: in method file_insert: failed to seek into pos: %d in file %s\n", 4, temp_file_name);

		if(fclose(temp) != 0){
			// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);
		}
		return 2;
	}
			
	fputc((trg_data_length + 36) & 0xFF, temp);
	fputc(((trg_data_length + 36) >> 8) & 0xFF, temp);
	fputc(((trg_data_length + 36) >> 16) & 0xFF, temp);
	fputc(((trg_data_length + 36) >> 24) & 0xFF, temp);

	// Rename the temp file with the name of original file
	if(rename(temp_file_name, src1) != 0) {
		// printf("Error: in method file_insert: failed to rename file %s to %s\n", temp_file_name, src1);

		return 5;
	}

	if(fclose(temp) != 0) {
		// printf("Error: in method file_insert: failed to close file %s\n", temp_file_name);

		return 1;
	}
}
	// return the length of data that has been inserted in the target file
	return src_data_length;
}
