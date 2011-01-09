/*
 * Copyright (c) 2001, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#include <jni.h>
#include <jcomm.h>

#ifndef _Included_jcomm_DriverGenUnix_Serial
#define _Included_jcomm_DriverGenUnix_Serial
#ifdef __cplusplus
extern "C" {
#endif
#undef  jcomm_DriverGenUnix_Serial_DATABITS_5
#define jcomm_DriverGenUnix_Serial_DATABITS_5 5L
#undef  jcomm_DriverGenUnix_Serial_DATABITS_6
#define jcomm_DriverGenUnix_Serial_DATABITS_6 6L
#undef  jcomm_DriverGenUnix_Serial_DATABITS_7
#define jcomm_DriverGenUnix_Serial_DATABITS_7 7L
#undef  jcomm_DriverGenUnix_Serial_DATABITS_8
#define jcomm_DriverGenUnix_Serial_DATABITS_8 8L
#undef  jcomm_DriverGenUnix_Serial_FLOWCONTROL_NONE
#define jcomm_DriverGenUnix_Serial_FLOWCONTROL_NONE 0L
#undef  jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_IN
#define jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_IN 1L
#undef  jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_OUT
#define jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_OUT 2L
#undef  jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_IN
#define jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_IN 4L
#undef  jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_OUT
#define jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_OUT 8L
#undef  jcomm_DriverGenUnix_Serial_PARITY_NONE
#define jcomm_DriverGenUnix_Serial_PARITY_NONE 0L
#undef  jcomm_DriverGenUnix_Serial_PARITY_ODD
#define jcomm_DriverGenUnix_Serial_PARITY_ODD 1L
#undef  jcomm_DriverGenUnix_Serial_PARITY_EVEN
#define jcomm_DriverGenUnix_Serial_PARITY_EVEN 2L
#undef  jcomm_DriverGenUnix_Serial_PARITY_MARK
#define jcomm_DriverGenUnix_Serial_PARITY_MARK 3L
#undef  jcomm_DriverGenUnix_Serial_PARITY_SPACE
#define jcomm_DriverGenUnix_Serial_PARITY_SPACE 4L
#undef  jcomm_DriverGenUnix_Serial_STOPBITS_1
#define jcomm_DriverGenUnix_Serial_STOPBITS_1 1L
#undef  jcomm_DriverGenUnix_Serial_STOPBITS_2
#define jcomm_DriverGenUnix_Serial_STOPBITS_2 2L
#undef  jcomm_DriverGenUnix_Serial_STOPBITS_1_5
#define jcomm_DriverGenUnix_Serial_STOPBITS_1_5 3L

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    open
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_open
  (JNIEnv *, jobject, jstring);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    enableReceiveThreshold
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_enableReceiveThreshold
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    enableReceiveTimeout
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_enableReceiveTimeout
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getReceiveThreshold
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getReceiveThreshold
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getReceiveTimeout
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getReceiveTimeout
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getBaudRate
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getBaudRate
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getDataBits
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getDataBits
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getFlowControlMode
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getFlowControlMode
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getParity
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getParity
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    getStopBits
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jcomm_DriverGenUnix_00024Serial_getStopBits
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    sendBreak
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_sendBreak
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    setDTR
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_setDTR
  (JNIEnv *, jobject, jint, jboolean);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    setFlowControlMode
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_setFlowControlMode
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    setRTS
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_setRTS
  (JNIEnv *, jobject, jint, jboolean);

/*
 * Class:     jcomm_DriverGenUnix_Serial
 * Method:    setSerialPortParams
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_jcomm_DriverGenUnix_00024Serial_setSerialPortParams
  (JNIEnv *, jobject, jint, jint, jint, jint, jint);

#ifdef __cplusplus
}
#endif
#endif

