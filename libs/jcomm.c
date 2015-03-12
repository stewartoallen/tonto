/*
 * Copyright (c) 2001, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

#include <errno.h>
#include <string.h>

#include <jcomm.h>
#include <jcomm_DriverGenUnix.h>
#include <jcomm_DriverGenUnix_Serial.h>
#include <jcomm_DriverGenUnix_Parallel.h>


// ---( utility functions )------------------------------------------
void throwMyException(JNIEnv *env, const char *ex, const char *msg)
{
	(*env)->ThrowNew(env, (*env)->FindClass(env, ex), msg);
}

//void log(char *str)
//{
//	printf(">> %s\n", str);
//}

// ---( javax.comm.DriverGenUnix native functions )------------------

// ---( BEGIN CYGWIN )---
#if defined(CYGWIN) && !defined(WIN32)
void cfmakeraw(struct termios *tio)
{
	tio->c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL|IXON);
	tio->c_oflag &= ~OPOST;
	tio->c_lflag &= ~(ECHO|ECHONL|ICANON|ISIG|IEXTEN);
	tio->c_cflag &= ~(CSIZE|PARENB);
	tio->c_cflag |= CS8;
}

void cfsetspeed(struct termios *tio, int speed)
{
	cfsetispeed(tio, speed);
	cfsetospeed(tio, speed);
}
#endif
// ---( END CYGWIN )-------------------------------------------------

/**
 * available()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_available
  (JNIEnv *env, jobject obj, jint fd)
{
	int result;
	if (ioctl(fd, FIONREAD, &result))
	{
		throwMyException(env, "java/io/IOException", (const char*)strerror(errno));
		return -1;
	}
	return (jint)result;
}

/**
 * read(byte[]. int off, int len)
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_read
  (JNIEnv *env, jobject obj, jint fd, jbyteArray data, jint off, jint len, jint time)
{
	fd_set fds;
	struct timeval sleep;
	struct timeval *psleep = &sleep;
	int numRead, rv, try = 0;
	jbyte *buf = (*env)->GetByteArrayElements(env, data, 0);

	FD_ZERO(&fds);
	FD_SET(fd, &fds);
	sleep.tv_sec = time/1000;
	sleep.tv_usec = 1000*(time%1000);
	if (time < 0)
	{
		psleep = NULL;
	}

	numRead = 0;
	while (numRead < len)
	{
		try = 0;
		do
		{
			rv = select(fd+1, &fds, NULL, NULL, psleep);
			if (rv < 0)
			{
				fprintf(stderr, "jcomm: select error %d\n",errno);
			}
		}
		while (rv < 0 && errno == EINTR && try++ < 2000);
		if (rv < 0)
		{
			numRead = -1;
			fprintf(stderr, "jcomm: select error %d\n",errno);
			throwMyException(env, "java/io/IOException", (const char*)strerror(errno));
			break;
		}
		if (rv == 0)
		{
			break;
		}
		rv = read(fd, buf + off + numRead, len - numRead);
		if (rv == 0)
		{
			break;
		}
		if (rv < 0)
		{
			fprintf(stderr, "jcomm: read error %d\n",errno);
			throwMyException(env, "java/io/IOException", (const char*)strerror(errno));
			numRead = -1;
			break;
		}
		numRead += rv;
	}

	(*env)->ReleaseByteArrayElements(env, data, buf, 0);
	return numRead;
}

/**
 * write(byte[]. int off, int len)
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_write
  (JNIEnv *env, jobject obj, jint fd, jbyteArray data, jint off, jint len)
{
	jbyte *buf;
	int wrote;
	int retry = 1000;
	buf = (*env)->GetByteArrayElements(env, data, 0);
	//printf("write %d %d\n", off, len);
	while (len > 0 && retry > 0) {
		wrote = write(fd, buf + off, len);
		//printf(" wrote %d\n", wrote);
		if (wrote < 0) {
			printf(" err %d\n", errno);
			if (errno == EAGAIN) {
				retry--;
				usleep(1);
				continue;
			}
			throwMyException(env, "java/io/IOException", "Unable to write data");
			break;
		}
		len -= wrote;
		off += wrote;
		//printf(" off %d len %d\n", off, len);
	}
	(*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
}

/**
 * close()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_close
  (JNIEnv *env, jobject obj, jint fd)
{
	close(fd);
}

// ---( javax.comm.DriverGenUnix$Serial native functions )-----------

/**
 * open(String port)
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_open
  (JNIEnv *env, jobject obj, jstring portName)
{
	int fd;
	struct termios tio;

	const char *port = (*env)->GetStringUTFChars(env, portName, 0);

	fd = open (port, O_RDWR | O_NOCTTY | O_NONBLOCK);
	if (fd < 0)
	{
		throwMyException(env, "java/io/IOException", (const char*)strerror(errno));
		return -1;
	}
	tcgetattr (fd, &tio);
	cfmakeraw (&tio);
	tio.c_cflag = B115200 | CS8 | CLOCAL | CREAD;
	tio.c_iflag = IGNPAR;
	tio.c_oflag = 0;
	tio.c_lflag = 0;
	tio.c_cc[VTIME] = 0;
	tio.c_cc[VMIN] = 1;
	cfsetspeed(&tio, B115200);
	tcflush (fd, TCIOFLUSH);
	if (tcsetattr (fd, TCSANOW, &tio) == -1)
	{
		throwMyException(env, "java/io/IOException", (const char*)strerror(errno));
	}
	(*env)->ReleaseStringUTFChars(env, portName, port);
	return fd;
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
 * disableReceiveThreshold()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_enableReceiveThreshold
  (JNIEnv *env, jobject obj, jint fd, jint thresh)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	tio.c_cc[VMIN] = thresh;
	tcsetattr (fd, TCSANOW, &tio);
}

/**
 * enableReceiveTimeout()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_enableReceiveTimeout
  (JNIEnv *env, jobject obj, jint fd, jint time)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	tio.c_cc[VTIME] = time / 100;
	tcsetattr (fd, TCSANOW, &tio);
}

/**
 * TODO
 * getReceiveFramingByte()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getReceiveFramingByte
  (JNIEnv *, jobject, jint);

/**
 * getReceiveThreshold()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getReceiveThreshold
  (JNIEnv *env, jobject obj, jint fd)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	return tio.c_cc[VMIN];
}

/**
 * getReceiveTimeout()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getReceiveTimeout
  (JNIEnv *env, jobject obj, jint fd)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	return tio.c_cc[VTIME] * 100;
}

/**
 * getBaudRate()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getBaudRate
  (JNIEnv *env, jobject obj, jint fd)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	switch (cfgetospeed(&tio))
	{
#ifdef B230400
		case B230400: return 230400;
#endif
		case B115200: return 115200;
		case B57600 : return 57600;
		case B38400 : return 38400;
		case B19200 : return 19200;
		case B9600  : return 9600;
		case B4800  : return 4800;
		case B2400  : return 2400;
		case B1800  : return 1800;
		case B1200  : return 1200;
		case B600   : return 600;
		case B300   : return 300;
		case B200   : return 200;
		case B150   : return 150;
		case B134   : return 134;
		case B110   : return 110;
		case B75    : return 75;
		case B50    : return 50;
		default     : return 0;
	}
}

/**
 * getDataBits()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getDataBits
  (JNIEnv *env, jobject obj, jint fd)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	switch (tio.c_cflag & CSIZE)
	{
		case CS8: return jcomm_DriverGenUnix_Serial_DATABITS_8;
		case CS7: return jcomm_DriverGenUnix_Serial_DATABITS_7;
		case CS6: return jcomm_DriverGenUnix_Serial_DATABITS_6;
		case CS5: return jcomm_DriverGenUnix_Serial_DATABITS_5;
		default : return -1;
	}
}

/**
 * getFlowControlMode()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getFlowControlMode
  (JNIEnv *env, jobject obj, jint fd)
{
	int mask = 0;
	struct termios tio;
	tcgetattr (fd, &tio);
	if (tio.c_iflag & IXON)
	{
		mask |= jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_OUT;
	}
	if (tio.c_iflag & IXOFF)
	{
		mask |= jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_IN;
	}
	if (tio.c_cflag & CRTSCTS)
	{
		mask |= (jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_IN |
				jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_OUT);
	}
	return mask;
}

JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getParity
  (JNIEnv *env, jobject obj, jint fd)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	if (tio.c_iflag & PARMRK)
	{
		if (tio.c_iflag & IGNPAR)
		{
			return jcomm_DriverGenUnix_Serial_PARITY_SPACE;
		}
		else
		{
			return jcomm_DriverGenUnix_Serial_PARITY_MARK;
		}
	}
	if (tio.c_cflag & PARENB)
	{
		if (tio.c_cflag & PARODD)
		{
			return jcomm_DriverGenUnix_Serial_PARITY_ODD;
		}
		else
		{
			return jcomm_DriverGenUnix_Serial_PARITY_EVEN;
		}
	}
	return jcomm_DriverGenUnix_Serial_PARITY_NONE;
}

/**
 * getStopBits()
 */
JNIEXPORT jint JNICALL Java_javax_comm_DriverGenUnix_00024Serial_getStopBits
  (JNIEnv *env, jobject obj, jint fd)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	if (tio.c_cflag & CSTOPB)
	{
		return jcomm_DriverGenUnix_Serial_STOPBITS_2;
	}
	else
	{
		return jcomm_DriverGenUnix_Serial_STOPBITS_1;
	}
}

/**
 * sendBreak()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_sendBreak
  (JNIEnv *env, jobject obj, jint fd, jint len)
{
	tcsendbreak(fd, len / 250);
}

/**
 * TODO
 * setDTR()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setDTR
  (JNIEnv *, jobject, jint, jboolean);

/**
 * setFlowControlMode()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setFlowControlMode
  (JNIEnv *env, jobject obj, jint fd, jint mode)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	tio.c_cflag &= ~CRTSCTS;
	tio.c_iflag &= ~(IXON | IXOFF | IXANY);
	if (mode & jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_OUT)
	{
		tio.c_iflag |= IXON;
	}
	if (mode & jcomm_DriverGenUnix_Serial_FLOWCONTROL_XONXOFF_IN)
	{
		tio.c_iflag |= IXOFF;
	}
	if (mode & (jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_IN |
		jcomm_DriverGenUnix_Serial_FLOWCONTROL_RTSCTS_IN))
	{
		tio.c_cflag |= CRTSCTS;
	}
	tcsetattr (fd, TCSANOW, &tio);
}

/**
 * TODO
 * setRTS()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setRTS
  (JNIEnv *, jobject, jint, jboolean);

/**
 * setSerialPortParams()
 */
JNIEXPORT void JNICALL Java_javax_comm_DriverGenUnix_00024Serial_setSerialPortParams
  (JNIEnv *env, jobject obj, jint fd, jint baud, jint data, jint stop, jint par)
{
	struct termios tio;
	tcgetattr (fd, &tio);
	tio.c_cflag = CLOCAL | CREAD;
	// set baud
	switch (baud)
	{
#ifdef B230400
		case 230400 : cfsetspeed(&tio, B230400); break;
#endif
		case 115200 : cfsetspeed(&tio, B115200); break;
		case 57600  : cfsetspeed(&tio, B57600); break;
		case 38400  : cfsetspeed(&tio, B38400); break;
		case 19200  : cfsetspeed(&tio, B19200); break;
		case 9600   : cfsetspeed(&tio, B9600); break;
		case 4800   : cfsetspeed(&tio, B4800); break;
		case 2400   : cfsetspeed(&tio, B2400); break;
		case 1800   : cfsetspeed(&tio, B1800); break;
		case 1200   : cfsetspeed(&tio, B1200); break;
		case 600    : cfsetspeed(&tio, B600); break;
		case 300    : cfsetspeed(&tio, B300); break;
		case 200    : cfsetspeed(&tio, B200); break;
		case 150    : cfsetspeed(&tio, B150); break;
		case 134    : cfsetspeed(&tio, B134); break;
		case 110    : cfsetspeed(&tio, B110); break;
		case 75     : cfsetspeed(&tio, B75); break;
		case 50     : cfsetspeed(&tio, B50); break;
		case 0      : cfsetspeed(&tio, B0); break;
		default     :
			throwMyException(env, "javax/comm/UnsupportedCommOperation",
				"Invalid Baud Rate");
			return;
	}
	// set databits
	switch (data)
	{
		case jcomm_DriverGenUnix_Serial_DATABITS_8 :
			tio.c_cflag |= CS8; break;
		case jcomm_DriverGenUnix_Serial_DATABITS_7 :
			tio.c_cflag |= CS7; break;
		case jcomm_DriverGenUnix_Serial_DATABITS_6 :
			tio.c_cflag |= CS6; break;
		case jcomm_DriverGenUnix_Serial_DATABITS_5 :
			tio.c_cflag |= CS5; break;
		default:
			throwMyException(env, "javax/comm/UnsupportedCommOperation",
				"Invalid Data Bits");
			return;

	}
	// set parity
	if (par & jcomm_DriverGenUnix_Serial_PARITY_SPACE)
	{
		tio.c_iflag |= (PARMRK | IGNPAR);
	}
	else
	if (par & jcomm_DriverGenUnix_Serial_PARITY_MARK)
	{
		tio.c_iflag |= PARMRK;
	}
	else
	if (par & jcomm_DriverGenUnix_Serial_PARITY_ODD)
	{
		tio.c_iflag |= (PARENB | PARODD);
	}
	else
	if (par & jcomm_DriverGenUnix_Serial_PARITY_EVEN)
	{
		tio.c_iflag |= PARENB;
	}
	else
	{
		tio.c_iflag |= IGNPAR;
	}
	// set stopbits
	if (stop & jcomm_DriverGenUnix_Serial_STOPBITS_2)
	{
		tio.c_cflag |= CSTOPB;
	}
	// commit
	if (tcsetattr (fd, TCSANOW, &tio) == -1)
	{
		throwMyException(env, "java/io/IOException", (const char*)strerror(errno));
	}
}

// ---( javax.comm.DriverGenUnix$Parallel native functions )---------

