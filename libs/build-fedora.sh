javahome=/etc/alternatives/java_sdk
arch=$(ls $javahome/jre/lib/*/libjava.so | \
           sed -e 's|.*lib/||' -e 's|/libjava.*||')

CFLAGS="$CFLAGS -D_REENTRANT"
CFLAGS="$CFLAGS -I. -I${javahome}/include -I${javahome}/include/linux"

LDFLAGS="$LDFLAGS -L${javahome}/jre/lib/${arch}"
LDFLAGS="$LDFLAGS -L${javahome}/jre/lib/${arch}/server"

set -x
cc $CFLAGS $LDFLAGS -shared -fPIC  \
    -o libjnijcomm.so  jcomm.c -ljava -ljvm -lpthread
