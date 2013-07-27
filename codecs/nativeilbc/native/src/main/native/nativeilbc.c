#include "jni.h"
#include "ilbc.h"

#define JNI_COPY  0

typedef struct {
	iLBC_encinst_t* encoder;
	iLBC_decinst_t* decoder;
} Codec;

JNIEXPORT jlong Java_ua_mobius_media_server_impl_dsp_audio_nativeilbc_Codec_createCodec(JNIEnv *env, jobject this) {
	Codec* codec=(Codec*)malloc(sizeof(Codec));
	WebRtcIlbcfix_EncoderCreate(&(codec->encoder));
	WebRtcIlbcfix_DecoderCreate(&(codec->decoder));
    return (long)codec;
}

JNIEXPORT void Java_ua_mobius_media_server_impl_dsp_audio_nativeilbc_Codec_releaseCodec(JNIEnv *env, jobject this,jlong linkReference) {
	Codec* current=(Codec*)linkReference;
	WebRtcIlbcfix_EncoderFree(current->encoder);
	WebRtcIlbcfix_DecoderFree(current->decoder);
	free(current);
}

JNIEXPORT void Java_ua_mobius_media_server_impl_dsp_audio_nativeilbc_Codec_resetCodec(JNIEnv *env, jobject this,jlong linkReference,jint currentMode) {
	Codec* current=(Codec*)linkReference;
	iLBC_encinst_t* Enc_Inst=current->encoder;
	iLBC_decinst_t* Dec_Inst=current->decoder;

	//should be 20ms since mms sends data in 20ms only
	WebRtcIlbcfix_InitEncode(Enc_Inst,20);
	//last param set to 1 to use enhancer
	WebRtcIlbcfix_InitDecode(Dec_Inst,currentMode,1);
}

JNIEXPORT void Java_ua_mobius_media_server_impl_dsp_audio_nativeilbc_Codec_encode(JNIEnv *env, jobject this,jshortArray src,jbyteArray destination,jlong linkReference) {	
	Codec* current=(Codec*)linkReference;
	iLBC_encinst_t* Enc_Inst=current->encoder;
	jshort *src_array = (*env)->GetShortArrayElements(env, src, NULL);
	jbyte *dest_bytes=(*env)->GetByteArrayElements(env, destination, NULL);
	jsize src_size = (*env)->GetArrayLength(env, src);    
	WebRtcIlbcfix_Encode(Enc_Inst, src_array, src_size, (int16_t*)dest_bytes);        
	(*env)->ReleaseShortArrayElements(env, src, src_array, JNI_COPY);
    	(*env)->ReleaseByteArrayElements(env, destination, dest_bytes, JNI_COPY);
}

JNIEXPORT void Java_ua_mobius_media_server_impl_dsp_audio_nativeilbc_Codec_decode(JNIEnv *env, jobject this,jshortArray src,jshortArray destination,jlong linkReference) {	
	Codec* current=(Codec*)linkReference;
	iLBC_decinst_t* Dec_Inst=current->decoder;	
	jshort *src_array=(*env)->GetShortArrayElements(env, src, NULL);
	jshort *dst_array = (*env)->GetShortArrayElements(env, destination, NULL);
	jsize src_size = (*env)->GetArrayLength(env, src);
	short speechType = 0;
	WebRtcIlbcfix_Decode(Dec_Inst, src_array, src_size*2, dst_array, &speechType);
	(*env)->ReleaseShortArrayElements(env, destination, dst_array, JNI_COPY);
    	(*env)->ReleaseShortArrayElements(env, src, src_array, JNI_COPY);
}
