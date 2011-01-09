/**
  WinJcom is a native interface to serial ports in java.
  Copyright 2007 by Damiano Bolla, Jcomm@engidea.com

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Library General Public
  License as published by the Free Software Foundation; either
  version 2 of the License, or (at your option) any later version.
  This can be used with commercial products and you are not obliged 
  to share your work with anybody.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Library General Public License for more details.

  You should have received a copy of the GNU Library General Public
  License along with this library; if not, write to the Free
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers

#include <windows.h>

// Include JNI prototyped
#include "jcommwin.h"


#define IO_EXCEPTION "java/io/IOException"
#define ILLEGAL_EXCEPTION "java/lang/IllegalArgumentException"


#define DEBUG_INIT       com_engidea_win32jcom_WinjcomIdentifier_DEBUG_INIT
#define DEBUG_TEST_PORT	 com_engidea_win32jcom_WinjcomIdentifier_DEBUG_TEST_PORT
#define DEBUG_ERRORS	 com_engidea_win32jcom_WinjcomIdentifier_DEBUG_ERRORS  
#define DEBUG_OPEN_PORT  com_engidea_win32jcom_WinjcomIdentifier_DEBUG_OPEN_PORT   
#define DEBUG_WRITE      com_engidea_win32jcom_WinjcomIdentifier_DEBUG_WRITE   
#define DEBUG_READ       com_engidea_win32jcom_WinjcomIdentifier_DEBUG_READ   
#define DEBUG_COMMEVENT  com_engidea_win32jcom_WinjcomIdentifier_DEBUG_COMMEVENT   
#define DEBUG_POPARAMS   com_engidea_win32jcom_WinjcomIdentifier_DEBUG_POPARAMS   
#define DEBUG_CLOSE_PORT com_engidea_win32jcom_WinjcomIdentifier_DEBUG_CLOSE_PORT   

// it does not need to be volatile since it is initialized at load time.
static jint debugMask;

/**
 * @return true if I must log this line
 */
static int mustLog ( int mask )
	{
	return debugMask & mask;
	}


/**
 * This is the first one being called after library is loaded
 */
JNIEXPORT void JNICALL Java_com_engidea_win32jcom_WinjcomIdentifier_nativeInitialize
  (JNIEnv *env, jobject jobj, jint jmask)
	{
	debugMask = jmask;
	
	if ( mustLog(DEBUG_INIT) ) printf ("nativeInitialize: CALLED debugMaks=0x%X\n",(unsigned int)debugMask);
	}


/**
 * Expand the given error code into a string format.
 * @return a pointer to the message, if it does not manage it returns null.
 * NOTE that the message MUST be cleared using LocalFree.
 */
static LPVOID expandLastError(DWORD lastError) 
	{ 
	LPVOID lpMsgBuf;

	if ( lastError <= ERROR_SUCCESS ) return NULL;

	FormatMessage(
      FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
      NULL,
      lastError,
      MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
      (LPTSTR) &lpMsgBuf,
      0, NULL );

	return lpMsgBuf;
	}

static void throw_exception( JNIEnv *env, char *exc, char *func, char *msg, DWORD lastError )
	{
	char buf[1000];
	LPVOID errorStr;
	int len;

	jclass javaclass = (*env)->FindClass( env, exc );

	if( javaclass == NULL ) 
		{
		(*env)->ExceptionDescribe( env );
		(*env)->ExceptionClear( env );
		printf ("throw_exception: cannot get javaclass\n");
		return;
		}

	errorStr = expandLastError(lastError);
	
	if ( errorStr != NULL )
		{
		len = _snprintf( buf,sizeof(buf)-2, "fun(%s) msg(%s) err(%d:%s)",func,msg,lastError,errorStr);
		LocalFree(errorStr);
		}
	else
		len = _snprintf( buf,sizeof(buf)-2, "fun(%s) msg(%s)",func,msg);

	buf[len]=0; // terminate string
	
	(*env)->ThrowNew( env, javaclass, buf );
	(*env)->DeleteLocalRef( env, javaclass );
	}



 
/**
 * Test if a given native port exists or not. 
 * NOTE that the port may not exist now since it is BUSY..
 */
JNIEXPORT jboolean JNICALL Java_com_engidea_win32jcom_WinjcomIdentifier_nativeIsPortPresent
  	(JNIEnv *env, jobject jobj, jstring tty_name )
	{
	HANDLE hComm;

	const char *gszPort = (*env)->GetStringUTFChars(env, tty_name, 0);

	if ( mustLog(DEBUG_TEST_PORT)) printf("isPortPresent: test name(%s)\n",gszPort);

	hComm = CreateFile( gszPort,   
		GENERIC_READ | GENERIC_WRITE,  
		0,  
		NULL,  
		OPEN_EXISTING, 
		0, 
		NULL); 
	
	if (hComm == INVALID_HANDLE_VALUE) 
		{
		throw_exception(env,IO_EXCEPTION,"isPortPresent","CreateFile FAIL",GetLastError());
		return FALSE;
		}

	// Need to close before return
	CloseHandle (hComm);
	return TRUE;
	}





/**
 * Open a port to be used.
 * @return the handle of the port.
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_Open
  (JNIEnv *env, jobject jobj, jstring jstr)
	{
	HANDLE hComm;

 	const char *filename = (*env)->GetStringUTFChars( env, jstr, 0 );
	
	if ( mustLog(DEBUG_OPEN_PORT)) printf("nativeOpen: name(%s)\n",filename);

	hComm = CreateFile( filename,   
		GENERIC_READ | GENERIC_WRITE,  
		0,  
		NULL,  
		OPEN_EXISTING, 
		FILE_FLAG_OVERLAPPED, 
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
 * Close a port
 * This can throw exception if port is already closed or fd id bad...
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_Close
  (JNIEnv *env, jobject jobj, jint jFd)
	{
	HANDLE hComm = (HANDLE)jFd;
	
	if ( mustLog(DEBUG_CLOSE_PORT)) printf("nativeClose: CALL\n");

	if ( CloseHandle(hComm ) ) return;
	
	throw_exception( env, IO_EXCEPTION, "nativeClose", "CloseHandle FAIL", GetLastError() );
	}




/**
 * Set port params
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_SetPortParam
  (JNIEnv *env, jobject jobj, jint jFd, jint jwhat, jint jvalue)
	{
	HANDLE hComm = (HANDLE)jFd;
	DCB dcb; 
 
	if ( ! GetCommState(hComm, &dcb) ) 
		{
		throw_exception( env, IO_EXCEPTION, "SetPortParam", "GetCommState FAIL", GetLastError() );
		return;
		}

	if ( jwhat == com_engidea_win32jcom_WinjcomPort_DCB_FLOWCONTROL )
		{
		switch ( jvalue )
			{
			case com_engidea_comm_SerialPort_FLOWCONTROL_NONE:
				dcb.fOutxCtsFlow = FALSE;
				dcb.fOutxDsrFlow = FALSE;
				dcb.fDsrSensitivity = FALSE;
				dcb.fOutX = FALSE;
				dcb.fInX = FALSE;
				dcb.fRtsControl = RTS_CONTROL_ENABLE;
				break;

			case com_engidea_comm_SerialPort_FLOWCONTROL_RTSCTS_IN:
				dcb.fRtsControl = RTS_CONTROL_HANDSHAKE;
				break;

			case com_engidea_comm_SerialPort_FLOWCONTROL_RTSCTS_OUT:
				dcb.fOutxCtsFlow = TRUE;
				break;
			
			default:
				throw_exception( env, IO_EXCEPTION, "SetPortParam", "UNSUPPORTED value for FLOWCONTROL", 0);
				return;
			}
		}

	// if all is fine I am done
	if ( SetCommState(hComm, &dcb) ) return;
 	
	// Ack, exception !
	throw_exception( env, IO_EXCEPTION, "SetPortParam", "SetCommState FAIL", GetLastError() );
	}


/**
 * return a given param type as an integer value
 * throws exception if something goes wrong
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_GetPortParam
  (JNIEnv *env , jobject jobj, jint jFd, jint jwhat)
	{
	HANDLE hComm = (HANDLE)jFd;
	DCB dcb; 
 
	if ( ! GetCommState(hComm, &dcb) ) 
		{
		throw_exception( env, IO_EXCEPTION, "GetPortParam", "GetCommState FAIL", GetLastError() );
		return 0;
		}

	if ( jwhat == com_engidea_win32jcom_WinjcomPort_DCB_BAUDRATE )
		return dcb.BaudRate;

	if ( jwhat == com_engidea_win32jcom_WinjcomPort_DCB_DATABITS )
		return dcb.ByteSize;

	if ( jwhat == com_engidea_win32jcom_WinjcomPort_DCB_PARITY )
		return dcb.Parity;

	if ( jwhat == com_engidea_win32jcom_WinjcomPort_DCB_STOPBITS )
		return dcb.StopBits;

	throw_exception( env, IO_EXCEPTION, "GetPortParam", "Unmanaged param", 0 );
	return 0;
	}



/**
 * The reason for all of this, manage timeouts the proper way !
 * Just read windows documentsion for this !
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_SetCommTimeouts
  (JNIEnv *env, jobject job, jint jFd, 
	jint reInTi, jint reToTiMu, jint reToTiCo, jint wrToTiMu, jint wrToTiCo)
	{
	HANDLE hComm = (HANDLE)jFd;
	COMMTIMEOUTS timeouts;

	timeouts.ReadIntervalTimeout        = reInTi;
	timeouts.ReadTotalTimeoutMultiplier = reToTiMu;
	timeouts.ReadTotalTimeoutConstant   = reToTiCo;
	timeouts.WriteTotalTimeoutMultiplier = wrToTiMu;
	timeouts.WriteTotalTimeoutConstant  = wrToTiCo;

	// if all is nice I am dfone !
	if (SetCommTimeouts(hComm, &timeouts)) return;

	// Ack function failed...
	throw_exception( env, IO_EXCEPTION, "SetCommTimeouts", "SetCommTimeouts FAILS", GetLastError() );
	}


/**
 * An accessor for the timeout
 * NOTE: only one is returned !
 */ 
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_GetReceiveTotalTimeoutConstant
  (JNIEnv *env, jobject jobj, jint jFd)
	{
	HANDLE hComm = (HANDLE)jFd;
	COMMTIMEOUTS timeouts;

	// if I manage to get the value I am done !
	if ( GetCommTimeouts(hComm, &timeouts)) return timeouts.ReadTotalTimeoutConstant;

	// Windows failure :-)
	throw_exception( env, IO_EXCEPTION, "GetReceiveTotalTimeoutConstant", "GetCommTimeouts FAILS", GetLastError() );
	return 0;
	}



/**
 * Enables or disable specific notification for the given port
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_NotifyEnable
  (JNIEnv *env , jobject jobj, jint jFd, jint what, jboolean status)
	{
	HANDLE hComm = (HANDLE)jFd;
	DWORD commMask;

	if ( ! GetCommMask (hComm, &commMask) )
		{
		throw_exception(env,IO_EXCEPTION,"notifyEnable","GetCommMask FAIL",GetLastError());
		return;
		}

	if ( status )
		commMask = commMask | what;
	else
		commMask = commMask & (~what);

	if ( mustLog(DEBUG_COMMEVENT) ) printf("nativeNotifyEnable: commMask=0x%X\n",(unsigned int)commMask);

	// if setting is ok I am done.
	if ( SetCommMask (hComm, commMask) ) return;

	// FAilure, with exception !
	throw_exception(env,IO_EXCEPTION,"notifyEnable","SetCommMask FAIL",GetLastError());
	}

/**
 * You may think that you can just use the blocking version of the wait, after all there is another Java
 * thread waiting for events. The issue is not the java thread, BUT the fact that windows BLOCK all operations
 * on the "serial port" if there is one blocking operation pending. So you HAVE to be OVERLAPPED.
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_GetCommEvent
  (JNIEnv *env, jobject jobj, jint jFd)
	{
	HANDLE hComm = (HANDLE)jFd;
	DWORD  dwCommEvent=0;
	DWORD  res,dummy;
	OVERLAPPED overlapped = {0}; 

	if ( mustLog(DEBUG_COMMEVENT) ) printf("getCommEvent: CALL\n");
 
	// Create the overlapped event. Must be closed before exiting to avoid a handle leak. 
	overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL); 
 
	if (overlapped.hEvent == NULL) 
		{
		throw_exception(env,IO_EXCEPTION,"getCommEvent","CreateEvent FAIL", GetLastError());
		return 0;
		}
 
	if ( WaitCommEvent(hComm, &dwCommEvent, &overlapped) ) 
		{
		// Function returned immediately with a proper value, it may happen
		CloseHandle(overlapped.hEvent);
		return dwCommEvent;
		}
		
	res = GetLastError();

	if (res != ERROR_IO_PENDING )
		{
		// What is different than IO_PENDING is an error !!!
		CloseHandle(overlapped.hEvent);
		throw_exception(env,IO_EXCEPTION,"getCommEvent","GetLastError != ERROR_IO_PENDING",res);
		return 0;
		}

	// here I wait for the read to complete, INFINITE Timeout is correct
	res = WaitForSingleObject(overlapped.hEvent, INFINITE); 

	if (res != WAIT_OBJECT_0 )
		{
		throw_exception(env,IO_EXCEPTION,"getCommEvent","WaitForSingleObject FAIL",GetLastError());
		CloseHandle(overlapped.hEvent);
		return 0;
		}

	/**
	 * I cannot believe it but it is true.
	 * When overlapped returns the value is set into the original &dvCommEvent, not into what is
	 * being passed to GetOverlappedResult. 
	 * As an example, lets say that the pointer given to WaitCOmmEvent comes from allocation into stack
	 * (as it is in this function) but GetOverlappedResult is in another stack frame than the original call
	 * The result IS memory area corruption !!!! CRAPPY Windows !!!
	 */
	if (! GetOverlappedResult(hComm, &overlapped, &dummy, FALSE)) 
		{
		// MUST close handle before returning
		CloseHandle(overlapped.hEvent);
		throw_exception(env,IO_EXCEPTION,"getCommEvent","GetOverlappedResult FAIL", GetLastError());
		return 0;
		}

	if ( mustLog(DEBUG_COMMEVENT) ) printf("getCommEvent: commEvent=0x%X\n",(unsigned int)dwCommEvent);

	CloseHandle(overlapped.hEvent);
	return dwCommEvent; 
	}



/**
 * return true if all is fine, FALSE othervise
 */
static int convParityToWin ( DCB *dcb, int parity )
	{
	if ( mustLog(DEBUG_POPARAMS) ) printf("convParityToWin: parityin=%d\n",parity);

	switch ( parity )
		{
		case com_engidea_comm_SerialPort_PARITY_NONE:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convParityToWin: NOPARITY\n");
			dcb->fParity = FALSE;
			dcb->Parity = NOPARITY;
			break;
		
		case com_engidea_comm_SerialPort_PARITY_MARK:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convParityToWin: MARKPARITY\n");
			dcb->fParity = TRUE;
			dcb->Parity = MARKPARITY;
			break;

		case com_engidea_comm_SerialPort_PARITY_EVEN:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convParityToWin: EVENPARITY\n");
			dcb->fParity = TRUE;
			dcb->Parity = EVENPARITY;
			break;

		case com_engidea_comm_SerialPort_PARITY_ODD:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convParityToWin: ODDPARITY\n");
			dcb->fParity = TRUE;
			dcb->Parity = ODDPARITY;
			break;

		case com_engidea_comm_SerialPort_PARITY_SPACE:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convParityToWin: SPACEPARITY\n");
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
static int convStopbitToWin ( DCB *dcb, int stopbit )
	{
	if ( mustLog(DEBUG_POPARAMS) ) printf("convStopbitToWin: stopbitin=%d\n",stopbit);

	switch ( stopbit )
		{
		case com_engidea_comm_SerialPort_STOPBITS_1:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convStopbitToWin: ONESTOPBIT\n");
			dcb->StopBits = ONESTOPBIT;
			break;
		
		case com_engidea_comm_SerialPort_STOPBITS_2:
			if ( mustLog(DEBUG_POPARAMS) ) printf("convStopbitToWin: TWOSTOPBITS\n");
			dcb->StopBits = TWOSTOPBITS;
			break;

		default:
			return FALSE;
		}

	return TRUE;
	}


/**
 * One of the first calls. Sets the port parameters.
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_SetSerialPortParams
  (JNIEnv *env, jobject jobj, jint jhandle, 
	 jint baudrate, jint databits, jint stopbits, jint parity)
	{
	HANDLE hComm = (HANDLE)jhandle;
	DCB dcb; 
 
	if ( mustLog(DEBUG_POPARAMS) ) printf("SetSerialPortParams: baud(%d)\n",(int)baudrate);

	if ( ! GetCommState(hComm, &dcb) ) 
		{
		throw_exception(env,IO_EXCEPTION,"SetSerialPortParams","GetCommState FAIL", GetLastError());
		return;
		}

	// No need for conversio, just make sure you give decenpt values 
	dcb.BaudRate = baudrate;

	if ( ! convParityToWin(&dcb,parity)  )
		{
		throw_exception(env,ILLEGAL_EXCEPTION,"SetSerialPortParams","Illegal Parity",0);
		return;
		}

	if ( ! convStopbitToWin(&dcb,stopbits)  )
		{
		throw_exception(env,ILLEGAL_EXCEPTION,"SetSerialPortParams","Illegal Stopbit",0);
		return;
		}

	dcb.ByteSize      = databits;
	dcb.fAbortOnError = FALSE;               // do ont abort on errors
	dcb.fDtrControl   = DTR_CONTROL_ENABLE;  // Enables the DTR line when the device is opened and leaves it on.

	// If setting is fine I am done.
	if ( SetCommState(hComm, &dcb) ) return; 

	// Can happen, for way too many reasons
  	throw_exception(env,IO_EXCEPTION,"SetSerialPortParams","SetCommState FAIL", GetLastError());
	}


/**
 * Used to set/clear port lines
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_SendEscape
  (JNIEnv *env, jobject jobj, jint jfd, jint what)
	{
	HANDLE hComm = (HANDLE)jfd;

	/*
	SETRTS; // 3
	CLRRTS;	// 4
	SETDTR;	// 5
	CLRDTR;	// 6
	SETBREAK;	// 8
	CLRBREAK;	// 9
	*/
	
	// really easy if it goes well
	if ( EscapeCommFunction(hComm, what) ) return;
	
	// exception if it fails.
	throw_exception(env,ILLEGAL_EXCEPTION,"nativeSendEscape","EscapeCommFunction FAIL", GetLastError());
	}


/**
 * Write an array of bytes using the OVERLAPPED mode.
 * @return the number of bytes written
 * On timeout the number of bytes written will be LESS that what has been given !
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_WriteArray
  (JNIEnv * env, jobject jobj, jint jfd, jbyteArray jarray, jint joffset, jint jlen)
	{
	HANDLE hComm = (HANDLE)jfd;
	DWORD  scritti;
	DWORD  res;
	OVERLAPPED overlapped = {0}; 

	jbyte *dataP = (*env)->GetByteArrayElements( env, jarray, 0 );

	if ( mustLog(DEBUG_WRITE) ) printf("writeArray: len=%d\n",(int)jlen);

	// Create the overlapped event. Must be closed before exiting to avoid a handle leak. 
	overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL); 
 
	if (overlapped.hEvent == NULL) 
		{
		throw_exception(env,IO_EXCEPTION,"writeArray","CreateEvent FAIL", GetLastError());
		return 0;
		}
 
	if ( WriteFile(hComm, dataP+joffset, jlen, &scritti, &overlapped)) 
		{
		// if it does all in a single go...
		CloseHandle(overlapped.hEvent);
		return scritti;
		}
		
	if ( (res = GetLastError()) != ERROR_IO_PENDING )
		{
		// The only acceptable error is IO_PENDING, meaning that we are overlapping
		CloseHandle(overlapped.hEvent);
		throw_exception(env,IO_EXCEPTION,"writeArray","GetLastError GetLastError != ERROR_IO_PENDING",res);
		return 0;
		}

	// here I wait for the read to complete, with a timeout.
	// Handle timeout using COMM timeout, leave this to INFINITE
	res = WaitForSingleObject(overlapped.hEvent, INFINITE); 

	if (res != WAIT_OBJECT_0 )
		{
		throw_exception(env,IO_EXCEPTION,"writeArray","WaitForSingleObject FAIL",GetLastError());
		CloseHandle(overlapped.hEvent);
		return 0;
		}

	if (!GetOverlappedResult(hComm, &overlapped, &scritti, FALSE)) 
		{
		CloseHandle(overlapped.hEvent);
		throw_exception(env,IO_EXCEPTION,"writeArray","GetOverlappedResult FAIL", GetLastError());
		return 0;
		}

	// This is the normal termination, when all goes well
	// note that it may happen that overlapper returns on serial timeout and not all bytes are written
	CloseHandle(overlapped.hEvent);
	return scritti; 
	}


/**
 * Read data from port in overlapped mode.
 * Note that on timeout you may actually have read some data !
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_ReadArray
  (JNIEnv *env, jobject jobj, jint jhandle, jbyteArray jarray, jint joffset, jint jlen)
	{
	HANDLE hComm = (HANDLE)jhandle;
	DWORD  letti,res;
	jbyte *dataP;
	OVERLAPPED overlapped = {0}; 

	dataP = (*env)->GetByteArrayElements( env, jarray, 0 );

	if ( mustLog(DEBUG_READ) ) printf("readArray: len=%d\n",(int)jlen);
 
	// Create the overlapped event. MUST be closed before exiting to avoid a handle leak. 
	overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL); 
 
	if (overlapped.hEvent == NULL) 
		{
		throw_exception(env,IO_EXCEPTION,"readArray","CreateEvent FAIL",GetLastError());
		return -1;
		}
 
	if ( ReadFile(hComm, dataP+joffset, jlen, &letti, &overlapped) ) 
		{
		// There is a chance that the read returns immediately.
		CloseHandle(overlapped.hEvent);
		// you have to tell to java to pick up the data...	
		(*env)->ReleaseByteArrayElements( env, jarray, dataP, 0 );
		return letti;
		}
		
	if ( (res = GetLastError()) != ERROR_IO_PENDING )
		{
		// The only error allowed here is IO_PENDING manieng I am overlapping
		CloseHandle(overlapped.hEvent);
		throw_exception(env,IO_EXCEPTION,"readArray","GetLastError != ERROR_IO_PENDING",res);
		return -1;
		}

	// here I wait for the read to complete, INFINITE is correct !
	res = WaitForSingleObject(overlapped.hEvent, INFINITE); 

	if (res != WAIT_OBJECT_0 )
		{
		throw_exception(env,IO_EXCEPTION,"readArray","WaitForSingleObject res != WAIT_OBJECT_O",GetLastError());
		CloseHandle(overlapped.hEvent);
		return -1;
		}

  if (! GetOverlappedResult(hComm, &overlapped, &letti, FALSE)) 
		{
		throw_exception(env,IO_EXCEPTION,"readArray","GetOverlappedResult FAIL", GetLastError());
		CloseHandle(overlapped.hEvent);
		return -1;
		}

  	// Generally speaking I cannot have an EOF (I think) on a serial port.
    // So, basically, when letti != jlen it is a timeout....
  
  	// tell java to pick up the new data, it MAY be less than what I wanted if I have Timeout !
	(*env)->ReleaseByteArrayElements( env, jarray, dataP, 0 );
	CloseHandle(overlapped.hEvent);
	return letti; 
	}


/**
 * This handles
 * MS_CTS_ON;
 * MS_DSR_ON;
 * MS_RING_ON;
 * MS_RLSD_ON;
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_GetModemStatus
	( JNIEnv *env,	jobject jobj, jint jfd )
	{
	HANDLE hComm = (HANDLE)jfd;
	DWORD status;
	
	if ( GetCommModemStatus(hComm,&status)) return status;
	
	throw_exception(env,IO_EXCEPTION,"nativeGetModemStatus","GetCommModemStatus FAIL", GetLastError());
	return 0;
	}


/**
 * This is used to get various info about comm
 * DOn' t ask me about the name..... I just gotten from Windows ...
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_ClearCommError
  (JNIEnv *env , jobject jobj, jint jFd, jint what )
	{
	HANDLE hComm = (HANDLE)jFd;
	DWORD error;
	COMSTAT comStat;
	
	if ( ! ClearCommError(hComm, &error, &comStat ) )
		{
		throw_exception(env,IO_EXCEPTION,"nativeClearCommError","ClearCommError FAIL", GetLastError());
		return 0;
		}
	
	if ( what == com_engidea_win32jcom_WinjcomPort_COMSTAT_cbInQue )
		return comStat.cbInQue;
	
	throw_exception(env,IO_EXCEPTION,"nativeClearCommError","Unsupported operation",0);
	return 0;
	}

/**
 * Flush possible pending chars
 * BOTH Rx AND tx !!! 
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_PurgeComm
  (JNIEnv *env , jobject jobj, jint jFd)
	{
	HANDLE hComm = (HANDLE)jFd;
	
	if ( PurgeComm(hComm, PURGE_TXCLEAR | PURGE_RXCLEAR)) return;
	
	throw_exception(env,IO_EXCEPTION,"nativePurgeComm","PurgeComm FAIL", GetLastError());
	}


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *java_vm, void *reserved)
	{
//	printf("JNI_OnLoad\n");
	return JNI_VERSION_1_2;  
	}


JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
	{
//	printf("JNI_OnUnload\n");
	}

