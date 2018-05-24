#include<jni.h>
#include <android/log.h>
#include<vcva.h>


CVcva VCVA;
long lCount;
bool bStatus;
short * audio;
int datalen=0;

extern "C"
{
    JNIEXPORT jlong JNICALL Java_com_olympus_dmmobile_recorder_DMAudioRecorder_init(JNIEnv * env, jobject obj,jlong rate,jlong level);
    JNIEXPORT jboolean JNICALL Java_com_olympus_dmmobile_recorder_DMAudioRecorder_isVoicedFrame(JNIEnv * env, jobject obj,jshortArray data);
};

// this function is called from DMAudioRecorder which initialize VCVA
JNIEXPORT jlong JNICALL Java_com_olympus_dmmobile_recorder_DMAudioRecorder_init(JNIEnv * env, jobject obj,jlong rate,jlong level)
{
	long sampleRate;
	long VCVALevel;

	sampleRate=rate;
	VCVALevel=level;
	VCVA.InitializeVCVA(sampleRate,&lCount) ;  //16 kHz
	VCVA.SetVCVALevel(VCVALevel);

	return lCount;
}

// this function is used to detect whether the audio contains voice or silence
JNIEXPORT jboolean JNICALL Java_com_olympus_dmmobile_recorder_DMAudioRecorder_isVoicedFrame(JNIEnv *env, jobject obj,jshortArray data)
{
	int len=(env)->GetArrayLength(data);
	audio=env->GetShortArrayElements(data,0);

	datalen=datalen+len;
	VCVA.IsVoicedFrame((short *)audio,&bStatus);
	env->ReleaseShortArrayElements(data,audio,0);

	return bStatus;
}
