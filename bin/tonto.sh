#!/bin/sh

#
# set TONTODIR to the permanent install location of Tonto
#

TONTODIR="/usr/share/tonto"

#
# set to the root directory of your preferred JDK. the JDK
# must be 1.3.x or newer. if the correct JDK is already in
# your path, you do not need to modify this.
#

JHOME="`which java`"
if [ "$JHOME" != "" ]
then
	JHOME=`dirname $JHOME`
fi

#
# normally, you do not need to make changes beyond this point
#

JBIN="$JHOME/jre/bin"
JLIB="$JHOME/jre/lib"

#
# use tonto's native comm library
#
export LD_LIBRARY_PATH="${TONTODIR}/lib"

#
# addition VM flags for added memory. may be required for
# handling of very large CCF files.
#
MEM="-Xms64M -Xmx256M"

#
# launch Tonto
#

if [ ! -f "$JBIN/java" ]
then
	echo ""
	echo "No Java JVM was found in your path or the Tonto"
	echo "startup script is incorrectly configured."
	echo ""
	exit 1
fi

"$JBIN/java" ${MEM} -jar "${TONTODIR}/bin/boot.jar" $*

