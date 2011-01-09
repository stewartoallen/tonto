javahome=${JAVA_HOME}
buildjni=libjnijcomm.so
buildc=jcomm.c

cc \
 -O9 \
 -shared \
 -o \
 ${buildjni} \
 ${buildc} \
 -I. \
 -I${javahome}/include \
 -L${javahome}/jre/lib/i386 \
 -L${javahome}/jre/bin \
 -L${javahome}/jre/bin/classic \
 -D_REENTRANT \
 -ljava \
 -lpthread 
# -ljvm \

