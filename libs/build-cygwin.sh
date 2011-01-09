javahome=/cygdrive/c/java/1.3.1-sun
buildlib=jnijcomm.dll
buildtmp=jcommdll.o
buildc=jcomm.c

rm -f ${buildtmp} ${buildlib} 

gcc \
 -c \
 -DBUILDING_DLL=1 \
 -D_DLL=1 \
 -D_REENTRANT \
 -DCYGWIN \
 -I. \
 -I"${javahome}/include" \
 -I"${javahome}/include/win32" \
 -g \
 -Wall \
 -O2 \
 -o \
 ${buildtmp} \
 ${buildc}

# -I/usr/include \

dllwrap \
 --add-stdcall-alias \
 --driver-name gcc \
 -shared \
 -o \
 ${buildlib} \
 ${buildtmp} \
 -Wl 

# --output-def jcommdll.def \

#gcc \
# -shared \
# -o \
# ${buildjni} \
# ${buildc} \
# -I. \
# -I${javahome}/include \
# -L${javahome}/jre/bin \
# -D_REENTRANT \
# -ljava \
# -ljvm \
# -lhpi \
# -lpthread 

rm -f ${buildtmp}

