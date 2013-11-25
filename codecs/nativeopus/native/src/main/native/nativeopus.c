#include "jni.h"
#include "./libopus/include/opus.h"
#include "./libopus/include/opus_types.h"
#include "./libopus/include/opus_defines.h"

#define JNI_COPY  0

typedef struct {
	OpusEncoder* encoder;
	OpusDecoder* decoder;
} Codec;

JNIEXPORT jlong Java_ua_mobius_media_server_impl_dsp_audio_nativeopus_Codec_createCodec(JNIEnv *env, jobject this,jint bandwidth) {
	Codec* codec=(Codec*)malloc(sizeof(Codec));
	int error;
	codec->encoder=opus_encoder_create(bandwidth,1,OPUS_APPLICATION_VOIP,&error);
	opus_encoder_ctl(codec->encoder,OPUS_SET_INBAND_FEC(1));
	codec->decoder=opus_decoder_create(bandwidth,1,&error);
    return (long)codec;
}

JNIEXPORT void Java_ua_mobius_media_server_impl_dsp_audio_nativeopus_Codec_releaseCodec(JNIEnv *env, jobject this,jlong linkReference) {
	Codec* current=(Codec*)linkReference;
	opus_encoder_destroy(current->encoder);
	opus_decoder_destroy(current->decoder);
	free(current);
}

JNIEXPORT void Java_ua_mobius_media_server_impl_dsp_audio_nativeopus_Codec_resetCodec(JNIEnv *env, jobject this,jlong linkReference,jint bandwidth) {
	Codec* current=(Codec*)linkReference;
	OpusEncoder* encoder=current->encoder;
	OpusDecoder* decoder=current->decoder;

	opus_encoder_init(encoder,bandwidth,1,OPUS_APPLICATION_VOIP);
	opus_decoder_init(decoder,bandwidth,1);
}

JNIEXPORT jint Java_ua_mobius_media_server_impl_dsp_audio_nativeopus_Codec_encode(JNIEnv *env, jobject this,jshortArray src,jbyteArray destination,jlong linkReference) {	
	Codec* current=(Codec*)linkReference;
	OpusEncoder* encoder=current->encoder;
	jshort *src_array = (*env)->GetShortArrayElements(env, src, NULL);
	jbyte *dest_bytes=(*env)->GetByteArrayElements(env, destination, NULL);
	jsize src_size = (*env)->GetArrayLength(env, src);
	jsize dest_size = (*env)->GetArrayLength(env, destination);
	int result=opus_encode(encoder, (opus_int16*)src_array, src_size, (unsigned char*)dest_bytes,dest_size);        
	(*env)->ReleaseShortArrayElements(env, src, src_array, JNI_COPY);
    (*env)->ReleaseByteArrayElements(env, destination, dest_bytes, JNI_COPY);
	return result;
}

JNIEXPORT jint Java_ua_mobius_media_server_impl_dsp_audio_nativeopus_Codec_decode(JNIEnv *env, jobject this,jbyteArray src,jshortArray destination,jlong linkReference) {	
	Codec* current=(Codec*)linkReference;
	OpusDecoder* decoder=current->decoder;	
	jbyte *src_array=(*env)->GetByteArrayElements(env, src, NULL);
	jshort *dst_array = (*env)->GetShortArrayElements(env, destination, NULL);
	jsize src_size = (*env)->GetArrayLength(env, src);
	jsize destination_size = (*env)->GetArrayLength(env, destination);
	int result=opus_decode(decoder, (unsigned char*)src_array, src_size, (opus_int16*)dst_array, destination_size, 0);
	(*env)->ReleaseShortArrayElements(env, destination, dst_array, JNI_COPY);
    (*env)->ReleaseByteArrayElements(env, src, src_array, JNI_COPY);
	return result;
}