@echo off

rem
rem A Sample Windows batch file. The preferred method for launching
rem Tonto is to associate javaw.exe with jar files to make them
rem double-clickable. One reason to use a batch file over directly
rem launching a jar is the ability to add additional startup parameters
rem (i.e. for using a larger JVM memory space).
rem

rem
rem set TONTODIR to Tonto's permanent installed location
rem it should be the directory containg Tonto, not the bin directory
rem

set TONTODIR=c:\program files\tonto

rem
rem set JHOME to the root directory of an installed JDK or JRE
rem the Java Runtime must me 1.3.x or newer. this must be the base
rem install directory for the JVM, not the bin directory.
rem

set JHOME=%TONTODIR%

rem
rem you should not normall have to modify anything beyond this point.
rem Tonto requires the JavaComm libraries
rem

set JBIN=%JHOME%\jre\bin
set JLIB=%JHOME%\jre\lib

rem
rem launch tonto
rem

"%JBIN%\javaw.exe" -Xmx256M -Xms64M -jar "%TONTODIR%\bin\boot.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9

