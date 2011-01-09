export java_home="/cygdrive/c/Program Files/Java/jdk1.6.0_03"

gcc -Wall -mno-cygwin -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -I . -I "${java_home}/include" -I "${java_home}/include/win32" -shared jcommwin.c -o jnijcomm.dll

