#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include<stdlib.h>

// called from DMActivity to copy a dictation from sent list for editing
jint Java_com_olympus_dmmobile_DMActivity_editCopy(JNIEnv * env, jobject jobj,jlong allocateSize,jstring src,jstring dest)
{

	FILE *fin,*writer;
	long buffer_size;

	buffer_size=10*1024;					// 10 kb

	const char *inFile= (*env)->GetStringUTFChars(env,src,0);				//path of source file to copy
	const char *destFile= (*env)->GetStringUTFChars(env,dest,0);			//path of destination file

	// open the source file in read mode
	fin = fopen(inFile, "r");
	if(fin == NULL) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "unable to open file %s in read mode\n", inFile);
		return 0;
	}

	// open the destination file in write mode
	writer=fopen(destFile,"w");
	if(writer == NULL) {
		__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "unable to open file %s in write mode\n", destFile);
		return 0;
	}

	// seek to the end of file
	if(fseek(fin, 0, SEEK_END) != 0) {
		//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to seek into end in file %s\n",inFile);

		if(fclose(fin) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", inFile);
		}
		if(fclose(writer) != 0){
			//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", destFile);
		}

		return 0;
	}
	// reads the byte position
	 long size = ftell(fin);

	 // seek to the beginning of file
	 if(fseek(fin, 0, SEEK_SET) != 0) {
	 	//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to seek begining in file %s\n",inFile);

	 	if(fclose(fin) != 0){
	 		//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", inFile);
	 	}
	 	if(fclose(writer) != 0){
	 		//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", destFile);
	 	}

	 	return 0;
	 }

	//set the buffer size
	long charCnt = 0;
	if(size<buffer_size){
		buffer_size=size;
	}

	 void *buffer = NULL;
	 long int read_count = 0;
	 long int write_count = 0;

	 buffer = malloc(buffer_size);
	 if(buffer == NULL) {
		// __android_log_print("Copy file"," failed to allocate buffer of size %d bytes\n",buffer_size);
		 return 0;
	 }

	 while(!feof(fin)) {
		 // read the data from source file
		 read_count = fread(buffer, 1,buffer_size, fin);
		 if(ferror(fin) != 0) {
			 //__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "error while reading data from  %s\n", inFile);
			 if(fclose(fin) != 0){
				 // __android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", inFile);
			 }
			 if(fclose(writer) != 0){
				 // __android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", destFile);
			 }

			 return 0;
		 }

		if(read_count <= 0)
			continue;

		// write the read data to destination file
		write_count = fwrite(buffer, 1, read_count, writer);

		if(read_count != write_count) {
			//__android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "mismatch in read and write count");

			if(fclose(fin) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", inFile);
			}
			if(fclose(writer) != 0){
				// __android_log_print(ANDROID_LOG_VERBOSE, "Copy file",  "failed to close file %s\n", destFile);
			}

			return 0;
		}

		fflush(writer);
	}

	 // release the pointers
	(*env)->ReleaseStringUTFChars(env,src,inFile);
	(*env)->ReleaseStringUTFChars(env,dest,destFile);

	free(buffer);
	fclose(fin);
	fclose(writer);

	return 1; //success

}
