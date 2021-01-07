/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_example_touch_HidrawManager */

#ifndef _Included_com_example_touch_HidrawManager
#define _Included_com_example_touch_HidrawManager
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_example_touch_HidrawManager
 * Method:    openHidraw
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_example_touch_HidrawManager_openHidraw
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_example_touch_HidrawManager
 * Method:    writeToHidraw
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_example_touch_HidrawManager_writeToHidraw
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_example_touch_HidrawManager
 * Method:    readForHidraw
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_touch_HidrawManager_readForHidraw
  (JNIEnv *, jclass);

/*
 * Class:     com_example_touch_HidrawManager
 * Method:    readWrite
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_touch_HidrawManager_readWrite
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_example_touch_HidrawManager
 * Method:    removeHidRaw
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_example_touch_HidrawManager_removeHidRaw
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     com_example_touch_HidrawManager
 * Method:    closeFd
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_example_touch_HidrawManager_closeFd
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     com_example_touch_HidrawManager
 * Method:    exist
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_example_touch_HidrawManager_exist
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
