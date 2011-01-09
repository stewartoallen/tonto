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

#ifndef _Included_jcomm_DriverGenUnix
#define _Included_jcomm_DriverGenUnix
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     jcomm_DriverGenUnix
 * Method:    available
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_available
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix
 * Method:    close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_close
  (JNIEnv *, jobject, jint);

/*
 * Class:     jcomm_DriverGenUnix
 * Method:    read
 * Signature: (I[BIII)I
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_read
  (JNIEnv *, jobject, jint, jbyteArray, jint, jint, jint);

/*
 * Class:     jcomm_DriverGenUnix
 * Method:    write
 * Signature: (I[BII)V
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_write
  (JNIEnv *, jobject, jint, jbyteArray, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
