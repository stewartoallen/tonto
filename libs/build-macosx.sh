#!/bin/sh

buildjni=libjnijcomm.jnilib
buildc=jcomm.c

mkdir -p osx/{x86,ppc}

# create x86 library
cc \
 -fPIC \
 -dynamic \
 -bundle \
 -shared \
 -o \
 osx/x86/${buildjni} \
 ${buildc} \
 -I. \
 -I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers \
 -L/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Libraries \
 -D_REENTRANT \
 -lpthread 

# create ppc library
cc \
 -arch ppc \
 -fPIC \
 -dynamic \
 -bundle \
 -shared \
 -o \
 osx/ppc/${buildjni} \
 ${buildc} \
 -I. \
 -I/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers \
 -L/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Libraries \
 -D_REENTRANT \
 -lpthread 

# create universal binary
lipo -create osx/*/*jnilib -output libjnijcomm.jnilib

