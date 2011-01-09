/*
 * Copyright (c) 2001, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#ifndef WIN32            // -- !windows begin

#include <sys/ioctl.h>
#include <termios.h>

#ifdef CYGWIN            // ---- cygwin begin
#include <asm/socket.h>
#include <string.h>
#endif                   // ---- cygwin end

#else                    // -- windows else

#include "win32termios.h"
#include <winsock.h>

#endif                   // -- end windows

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <errno.h>

// ---( START JCOMM )-----------------------------------
#ifndef JCOMM
#define JCOMM

// ---( CYGWIN port )-----------------------------------
#ifdef CYGWIN
typedef long long __int64;
#endif // CYGWIN

// ---( for missing java defs )-------------------------
#ifdef NEEDDEF

typedef int jint;
/*
#ifndef jintx
#define jint int
#endif
*/

typedef long jlong;
/*
#ifndef jlong
#define jlong long
#endif
*/

#ifndef JNIEXPORT
#define JNIEXPORT
#endif

#ifndef JNICALL
#define JNICALL
#endif

#endif // NEEDDEF

#endif // JCOMM

