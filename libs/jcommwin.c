/*
 * Copyright (c) 2001, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#ifndef WINVER				// Allow use of features specific to Windows XP or later.
#define WINVER 0x0501		// Change this to the appropriate value to target other versions of Windows.
#endif
#ifndef _WIN32_WINNT		// Allow use of features specific to Windows XP or later.                   
#define _WIN32_WINNT 0x0501	// Change this to the appropriate value to target other versions of Windows.
#endif						
#ifndef _WIN32_WINDOWS		// Allow use of features specific to Windows 98 or later.
#define _WIN32_WINDOWS 0x0410 // Change this to the appropriate value to target Windows Me or later.
#endif
#ifndef _WIN32_IE			// Allow use of features specific to IE 6.0 or later.
#define _WIN32_IE 0x0600	// Change this to the appropriate value to target other versions of IE.
#endif
#define WIN32_LEAN_AND_MEAN	// Exclude rarely-used stuff from Windows headers

#include <windows.h>
#include <jcommwin.h>
#include <jcommwin2.h>

#define IO_EXCEPTION "java/io/IOException"
#define ILLEGAL_EXCEPTION "java/lang/IllegalArgumentException"

extern int errno;

// ---( utility functions )------------------------------------------
/**
 * Expand the given error code into a string format.
 * @return a pointer to the message, if it does not manage it returns null.
 * NOTE that the message MUST be cleared using LocalFree.
 */
static LPVOID expandLastError(DWORD lastError) 
{ 
	LPVOID lpMsgBuf;

	if (lastError <= ERROR_SUCCESS)
	{
		return NULL;
	}

	FormatMessage(
		FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
		NULL,
		lastError,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		(LPTSTR) &lpMsgBuf,
		0, NULL );

	return lpMsgBuf;
}

static void throw_exception(JNIEnv *env, char *exc, char *func, char *msg, DWORD lastError)
{
	char buf[1000];
	LPVOID errorStr;
	int len;

	jclass javaclass = (*env)->FindClass( env, exc );
//printf("throw_exception %s %s %s %d\n", exc, func, msg, lastError);

	if (javaclass == NULL) 
	{
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		printf ("throw_exception: cannot get javaclass\n");
		return;
	}

	errorStr = expandLastError(lastError);
	
	if (errorStr != NULL)
	{
//printf("fun(%s) msg(%s) err(%d:%s)\n",func,msg,lastError,errorStr);
		len = _snprintf( buf,sizeof(buf)-2, "fun(%s) msg(%s) err(%d:%s)",func,msg,lastError,errorStr);
		LocalFree(errorStr);
	}
	else
	{
//printf("fun(%s) msg(%s)\n",func,msg);
		len = _snprintf( buf,sizeof(buf)-2, "fun(%s) msg(%s)",func,msg);
	}
	// terminate string
	buf[len]=0; 
	
	(*env)->ThrowNew( env, javaclass, buf );
	(*env)->DeleteLocalRef( env, javaclass );
}

/**
 * return true if all is fine, FALSE othervise
 */
static int convParityToWin(DCB *dcb, int parity)
{
	switch (parity)
	{
		case com_engidea_comm_SerialPort_PARITY_NONE:
			dcb->fParity = FALSE;
			dcb->Parity = NOPARITY;
			break;
		case com_engidea_comm_SerialPort_PARITY_MARK:
			dcb->fParity = TRUE;
			dcb->Parity = MARKPARITY;
			break;
		case com_engidea_comm_SerialPort_PARITY_EVEN:
			dcb->fParity = TRUE;
			dcb->Parity = EVENPARITY;
			break;
		case com_engidea_comm_SerialPort_PARITY_ODD:
			dcb->fParity = TRUE;
			dcb->Parity = ODDPARITY;
			break;
		case com_engidea_comm_SerialPort_PARITY_SPACE:
			dcb->fParity = TRUE;
			dcb->Parity = SPACEPARITY;
			break;
		default:
			return FALSE;
	}
	return TRUE;
}

/**
 * Convert stopbit parameter to windows format
 * @return TRUE if all if fine, false othervise.
 */
static int convStopbitToWin(DCB *dcb, int stopbit)
{
	switch (stopbit)
	{
		case com_engidea_comm_SerialPort_STOPBITS_1:
			dcb->StopBits = ONESTOPBIT;
			break;
		case com_engidea_comm_SerialPort_STOPBITS_2:
			dcb->StopBits = TWOSTOPBITS;
			break;
		default:
			return FALSE;
	}
	return TRUE;
}

// ---( javax.comm.DriverGenUnix native functions )------------------

/**
 * TODO
 * available()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_available
  (JNIEnv *env, jobject obj, jint fd)
{
	HANDLE hComm = (HANDLE)fd;
	DWORD error;
	COMSTAT comStat;
	if (!ClearCommError(hComm, &error, &comStat))
	{
		throw_exception(env,IO_EXCEPTION,"nativeClearCommError","ClearCommError FAIL", GetLastError());
		return 0;
	}
//printf("available %d\n",comStat.cbInQue);
	return comStat.cbInQue;
}

/**
 * read(byte[]. int off, int len)
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_read
  (JNIEnv *env, jobject jobj, jint jhandle, jbyteArray jarray, jint joffset, jint jlen)
{
	HANDLE hComm = (HANDLE)jhandle;
	DWORD  letti,ret;
	jbyte *dataP;

	if (jlen == 0) {
		return 0;
	}

	dataP = (*env)->GetByteArrayElements( env, jarray, 0 );

	ret = ReadFile(hComm, dataP+joffset, jlen, &letti, NULL);
	if (ret == 0) {
printf("read fail %d %d %d %d\n", dataP+joffset, jlen, ret, letti);
		throw_exception(env,IO_EXCEPTION,"read","read FAIL",GetLastError());
	}
	if (letti == 1) {
//printf(" --> [%c] [%d]\n", dataP[joffset], dataP[joffset]);
	}
	(*env)->ReleaseByteArrayElements( env, jarray, dataP, 0 );
	return letti;
}

/**
 * write(byte[]. int off, int len)
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_write
  (JNIEnv *env, jobject jobj, jint jfd, jbyteArray jarray, jint joffset, jint jlen)
{
	HANDLE hComm = (HANDLE)jfd;
	DWORD  scritti;
	DWORD  ret;

//printf("write %d %d %d\n", jfd, joffset, jlen);

	jbyte *dataP = (*env)->GetByteArrayElements(env, jarray, 0);

	while (jlen > 0) {
		ret = WriteFile(hComm, dataP+joffset, jlen, &scritti, NULL);
if (scritti == 0) {
printf(" zero length write %d %d\n", ret, scritti);
}
		if (ret == 0) {
printf(" short write %d %d\n", ret, scritti);
			throw_exception(env,IO_EXCEPTION,"write","write FAIL",GetLastError());
			return;
		}
		joffset += scritti;
		jlen -= scritti;
if (jlen > 0) {
printf(" retry write %d %d\n", ret, scritti);
}
		
	}
	//return scritti;
}

/**
 * close()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_close
  (JNIEnv *env, jobject obj, jint fd)
{
	HANDLE hComm = (HANDLE)fd;	
//printf("close %d\n", fd);

	if (CloseHandle(hComm))
	{
		return;
	}
	throw_exception( env, IO_EXCEPTION, "nativeClose", "CloseHandle FAIL", GetLastError() );
}

// ---( javax.comm.DriverGenUnix$Serial native functions )-----------

/**
 * open(String port)
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_open
  (JNIEnv *env, jobject jobj, jstring jstr)
{
	HANDLE hComm;
 	const char *filename = (*env)->GetStringUTFChars( env, jstr, 0 );

//printf("open %s\n", filename);
	
	hComm = CreateFile( filename,   
		GENERIC_READ | GENERIC_WRITE,  
		0,  
		NULL,  
		OPEN_EXISTING, 
		0,//FILE_FLAG_OVERLAPPED, 
		NULL); 
	
	if (hComm == INVALID_HANDLE_VALUE) 
	{
		throw_exception( env, IO_EXCEPTION, "nativeOpen", "CreateFile FAIL", GetLastError() );
		// Why Invalid value is -1 and not NULL as should be ???
		return (jint)INVALID_HANDLE_VALUE;
	}

	// Oh well, a bit of casting here....
	return (jint)hComm;
}

/**
 * TODO
 * disableReceiveFraming()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_disableReceiveFraming
  (JNIEnv *, jobject, jint);

/**
 * TODO
 * enableReceiveFraming()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_enableReceiveFraming
  (JNIEnv *, jobject, jint, jint);

/**
 * TODO
 * enableReceiveThreshold()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_enableReceiveThreshold
  (JNIEnv *env, jobject obj, jint fd, jint thresh);

/**
 * TODO
 * enableReceiveTimeout()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_enableReceiveTimeout
  (JNIEnv *env, jobject obj, jint fd, jint time)
{
	HANDLE hComm = (HANDLE)fd;
	COMMTIMEOUTS timeouts;
//printf("enableReceiveTimeout %d\n", time);
	timeouts.ReadIntervalTimeout         = time;
	timeouts.ReadTotalTimeoutMultiplier  = 0;
	timeouts.ReadTotalTimeoutConstant    = time;
	timeouts.WriteTotalTimeoutMultiplier = 0;
	timeouts.WriteTotalTimeoutConstant   = time;
	if (SetCommTimeouts(hComm, &timeouts)) return;
	throw_exception( env, IO_EXCEPTION, "SetCommTimeouts", "SetCommTimeouts FAILS", GetLastError() );
}

/**
 * TODO
 * getReceiveFramingByte()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getReceiveFramingByte
  (JNIEnv *, jobject, jint);

/**
 * TODO
 * getReceiveThreshold()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getReceiveThreshold
  (JNIEnv *env, jobject obj, jint fd);

/**
 * TODO
 * getReceiveTimeout()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getReceiveTimeout
  (JNIEnv *env, jobject obj, jint fd);

/**
 * TODO
 * getBaudRate()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getBaudRate
  (JNIEnv *env, jobject obj, jint fd)
{
	HANDLE hComm = (HANDLE)fd;
	DCB dcb; 
	if (!GetCommState(hComm, &dcb)) 
	{
		throw_exception( env, IO_EXCEPTION, "GetPortParam", "GetCommState FAIL", GetLastError() );
		return 0;
	}
	return dcb.BaudRate;
}

/**
 * TODO
 * getDataBits()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getDataBits
  (JNIEnv *env, jobject obj, jint fd)
{
	HANDLE hComm = (HANDLE)fd;
	DCB dcb; 
	if (!GetCommState(hComm, &dcb)) 
	{
		throw_exception( env, IO_EXCEPTION, "GetPortParam", "GetCommState FAIL", GetLastError() );
		return 0;
	}
	return dcb.ByteSize;
}

/**
 * TODO
 * getFlowControlMode()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getFlowControlMode
  (JNIEnv *env, jobject obj, jint fd);

/**
 * getParity()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getParity
  (JNIEnv *env, jobject obj, jint fd)
{
	HANDLE hComm = (HANDLE)fd;
	DCB dcb; 
	if (!GetCommState(hComm, &dcb)) 
	{
		throw_exception( env, IO_EXCEPTION, "GetPortParam", "GetCommState FAIL", GetLastError() );
		return 0;
	}
	return dcb.Parity;
}

/**
 * getStopBits()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getStopBits
  (JNIEnv *env, jobject obj, jint fd)
{
	HANDLE hComm = (HANDLE)fd;
	DCB dcb; 
	if (!GetCommState(hComm, &dcb)) 
	{
		throw_exception( env, IO_EXCEPTION, "GetPortParam", "GetCommState FAIL", GetLastError() );
		return 0;
	}
	return dcb.StopBits;
}

/**
 * TODO
 * sendBreak()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_sendBreak
  (JNIEnv *env, jobject obj, jint fd, jint len)
{
}

/**
 * TODO
 * setDTR()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setDTR
  (JNIEnv *, jobject, jint, jboolean);

/**
 * TODO
 * setFlowControlMode()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setFlowControlMode
  (JNIEnv *env, jobject obj, jint fd, jint mode)
{
	HANDLE hComm = (HANDLE)fd;
	DCB dcb;
	if (!GetCommState(hComm, &dcb))
	{
		throw_exception( env, IO_EXCEPTION, "SetPortParam", "GetCommState FAIL", GetLastError() );
		return;
	}
	dcb.fOutxCtsFlow = FALSE;
	dcb.fOutxDsrFlow = FALSE;
	dcb.fDsrSensitivity = FALSE;
	dcb.fOutX = (mode & jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_OUT) ? TRUE : FALSE;
	dcb.fInX = (mode & jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_IN) ? TRUE : FALSE;
	dcb.fRtsControl = (mode & jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_IN) ? RTS_CONTROL_HANDSHAKE : RTS_CONTROL_ENABLE;
	if (mode & jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_OUT)
	{
		dcb.fOutxCtsFlow = TRUE;
	}
	// if all is fine I am done
	if (SetCommState(hComm, &dcb))
	{
		return;
	}
	// Ack, exception !
	throw_exception(env, IO_EXCEPTION, "SetPortParam", "SetCommState FAIL", GetLastError());
}

/**
 * TODO
 * setRTS()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setRTS
  (JNIEnv *env, jobject obj, jint fd, jboolean bool)
{
	HANDLE hComm = (HANDLE)fd;
	if (EscapeCommFunction(hComm, bool ? SETRTS : CLRRTS) ) return;
	// exception if it fails.
	throw_exception(env,ILLEGAL_EXCEPTION,"nativeSendEscape","EscapeCommFunction FAIL", GetLastError());
}

/**
 * TODO
 * setSerialPortParams()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setSerialPortParams
  (JNIEnv *env, jobject obj, jint fd, jint baudrate, jint databits, jint stopbits, jint parity)
{
	HANDLE hComm = (HANDLE)fd;
	DCB dcb; 

//printf("setPortParams %d %d %d %d %d\n", fd, baudrate, databits, stopbits, parity);

	if (!GetCommState(hComm, &dcb) ) 
	{
		throw_exception(env,IO_EXCEPTION,"SetSerialPortParams","GetCommState FAIL", GetLastError());
		return;
	}
	// No need for conversio, just make sure you give decenpt values 
	dcb.BaudRate = baudrate;
	if (!convParityToWin(&dcb,parity))
	{
		throw_exception(env,ILLEGAL_EXCEPTION,"SetSerialPortParams","Illegal Parity",0);
		return;
	}
	if (!convStopbitToWin(&dcb,stopbits))
	{
		throw_exception(env,ILLEGAL_EXCEPTION,"SetSerialPortParams","Illegal Stopbit",0);
		return;
	}
	dcb.ByteSize      = databits;
	dcb.fAbortOnError = FALSE;               // do ont abort on errors
	dcb.fDtrControl   = DTR_CONTROL_ENABLE;  // Enables the DTR line when the device is opened and leaves it on.
	// If setting is fine I am done.
	if (SetCommState(hComm, &dcb))
	{
		return;
	}
	// Can happen, for way too many reasons
  	throw_exception(env,IO_EXCEPTION,"SetSerialPortParams","SetCommState FAIL", GetLastError());
}

// ---( javax.comm.DriverGenUnix$Parallel native functions )---------

