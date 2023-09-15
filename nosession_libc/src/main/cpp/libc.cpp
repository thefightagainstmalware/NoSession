#include <jni.h>
#include "unistd.h"
#include <fcntl.h>
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     gq_malwarefight_nosession_linux_libc_Libc
 * Method:    geteuid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_gq_malwarefight_nosession_linux_libc_Libc_geteuid
        (JNIEnv *, jclass) {
    return (int) geteuid();
}

/*
 * Class:     gq_malwarefight_nosession_linux_libc_Libc
 * Method:    unlink
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT void JNICALL Java_gq_malwarefight_nosession_linux_libc_Libc_unlink
        (JNIEnv* env, jclass, jstring string) {
    const char* path =  env->GetStringUTFChars(string, NULL);
    unlink(path);
    env->ReleaseStringUTFChars(string, path);
}

#ifdef __cplusplus
}
#endif
