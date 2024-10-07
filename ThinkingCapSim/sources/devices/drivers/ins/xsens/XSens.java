/**
 * @version March 16, 2006
 * @author Humberto Martinez Barbera
 */

package devices.drivers.ins.xsens;

import javax.comm.*;
import java.io.*;

import devices.data.*;
import devices.drivers.ins.*;

import wucore.utils.math.*;

public class XSens extends Ins
{
	// ------------------------
	// Timeouts and sizes
	// ------------------------
	static public final int	SER_TIMEOUT				= 500; 		// Timeout for serial communication (ms)
	static public final int 	WAIT_TIMEOUT 			= 250; 		// Timeout for active waits (ms)
	static public final int	DEF_TIMEOUT				= 100;		// Read timeout (ms)
	static public final int	INIT_TIMEOUT				= 250;		// Initialisation timeout (ms)
	
	//	 Field message indexes
	static public final int IND_PREAMBLE				= 0;
	static public final int IND_BID					= 1;
	static public final int IND_MID					= 2;
	static public final int IND_LEN					= 3;
	static public final int IND_DATA0					= 4;
	static public final int IND_LENEXTH				= 4;
	static public final int IND_LENEXTL				= 5;
	static public final int IND_DATAEXT0				= 6;

	//	 Maximum number of sensors supported
	static public final int MAXDEVICES				= 15;

	static public final byte PREAMBLE					= (byte) 0xFA;
	static public final byte BID_MASTER				= (byte) 0xFF;
	static public final byte BID_MT					= 0x01;
	static public final int EXTLENCODE				= 0xFF;

	static public final int LEN_MSGHEADER				= 4;
	static public final int LEN_MSGEXTHEADER			= 6;
	static public final int LEN_MSGHEADERCS			= 5;
	static public final int LEN_MSGEXTHEADERCS			= 7;
	static public final int LEN_CHECKSUM				= 1;
	static public final int LEN_UNSIGSHORT				= 2;
	static public final int LEN_UNSIGINT				= 4;
	static public final int LEN_FLOAT					= 4;

	//	 Maximum message/data length
	static public final int MAXDATALEN				= 2048;
	static public final int MAXSHORTDATALEN			= 254;
	static public final int MAXMSGLEN					= (MAXDATALEN+7);
	static public final int MAXSHORTMSGLEN				= (MAXSHORTDATALEN+5);

	//	 DID Type (high nibble)
	static public final int DID_TYPEH_MASK				= 0x00F00000;
	static public final int DID_TYPEH_MT				= 0x00000000;
	static public final int DID_TYPEH_XM				= 0x00100000;
	static public final int DID_TYPEH_MTI_MTX			= 0x00300000;

	//	 All Message identifiers
	//	 WakeUp state messages
	static public final byte MID_WAKEUP				= 0x3E;
	static public final byte MID_WAKEUPACK				= 0x3F;

	//	 Config state messages
	static public final byte MID_REQDID				= 0x00;
	static public final byte MID_DEVICEID				= 0x01;
	static public final byte LEN_DEVICEID				= 4;
	static public final byte MID_INITBUS				= 0x02;
	static public final byte MID_INITBUSRESULTS		= 0x03;
	static public final byte LEN_INITBUSRESULTS		= 4;
	static public final byte MID_REQPERIOD				= 0x04;
	static public final byte MID_REQPERIODACK			= 0x05;
	static public final byte LEN_PERIOD				= 2;
	static public final byte MID_SETPERIOD				= 0x04;
	static public final byte MID_SETPERIODACK			= 0x05;

/*
//	 XbusMaster

	static public final int MID_SETBID					(const unsigned char)0x06

	static public final int MID_SETBIDACK				(const unsigned char)0x07

	static public final int MID_AUTOSTART				(const unsigned char)0x06

	static public final int MID_AUTOSTARTACK			(const unsigned char)0x07

	static public final int MID_BUSPWROFF				(const unsigned char)0x08

	static public final int MID_BUSPWROFFACK			(const unsigned char)0x09

//	 End XbusMaster

*/
	
	static public final byte MID_REQDATALENGTH			= 0x0A;
	static public final byte MID_DATALENGTH			= 0x0B;
	static public final int LEN_DATALENGTH				= 2;
	static public final byte MID_REQCONFIGURATION		= 0x0C;
	static public final byte MID_CONFIGURATION			= 0x0D;
	static public final int LEN_CONFIGURATION			= 118;
	static public final byte MID_RESTOREFACTORYDEF		= 0x0E;
	static public final byte MID_RESTOREFACTORYDEFACK	= 0x0F;

	static public final byte MID_GOTOMEASUREMENT		= 0x10;
	static public final byte MID_GOTOMEASUREMENTACK	= 0x11;
	static public final byte MID_REQFWREV				= 0x12;
	static public final byte MID_FIRMWAREREV			= 0x13;
	static public final byte LEN_FIRMWAREREV			= 3;

	/*
//	 XbusMaster

	static public final int MID_REQBTDISABLE			(const unsigned char)0x14

	static public final int MID_REQBTDISABLEACK			(const unsigned char)0x15

	static public final int MID_DISABLEBT				(const unsigned char)0x14

	static public final int MID_DISABLEBTACK			(const unsigned char)0x15

	static public final int MID_REQOPMODE				(const unsigned char)0x16

	static public final int MID_REQOPMODEACK			(const unsigned char)0x17

	static public final int MID_SETOPMODE				(const unsigned char)0x16

	static public final int MID_SETOPMODEACK			(const unsigned char)0x17

//	 End XbusMaster

	static public final int MID_REQBAUDRATE				(const unsigned char)0x18

	static public final int MID_REQBAUDRATEACK			(const unsigned char)0x19

	static public final int LEN_BAUDRATE				(const unsigned short)1

	static public final int MID_SETBAUDRATE				(const unsigned char)0x18

	static public final int MID_SETBAUDRATEACK			(const unsigned char)0x19

//	 XbusMaster

	static public final int MID_REQSYNCMODE				(const unsigned char)0x1A

	static public final int MID_REQSYNCMODEACK			(const unsigned char)0x1B

	static public final int MID_SETSYNCMODE				(const unsigned char)0x1A

	static public final int MID_SETSYNCMODEACK			(const unsigned char)0x1B

//	 End XbusMaster


*/
	static public final byte MID_REQOUTPUTMODE			= (byte) 0xD0;
	static public final byte MID_REQOUTPUTMODEACK		= (byte) 0xD1;
	static public final int LEN_OUTPUTMODE		 		= 2;
	static public final byte MID_SETOUTPUTMODE			= (byte) 0xD0;
	static public final byte MID_SETOUTPUTMODEACK		= (byte) 0xD1;

	static public final byte MID_REQOUTPUTSETTINGS		= (byte) 0xD2;
	static public final byte MID_REQOUTPUTSETTINGSACK	= (byte) 0xD3;
	static public final int LEN_OUTPUTSETTINGS		 	= 4;
	static public final byte MID_SETOUTPUTSETTINGS		= (byte) 0xD2;
	static public final byte MID_SETOUTPUTSETTINGSACK	= (byte) 0xD3;

	static public final byte MID_REQOUTPUTSKIPFACTOR		= (byte) 0xD4;
	static public final byte MID_REQOUTPUTSKIPFACTORACK	= (byte) 0xD5;
	static public final int LEN_OUTPUTSKIPFACTOR			= 2;
	static public final byte MID_SETOUTPUTSKIPFACTOR		= (byte) 0xD4;
	static public final byte MID_SETOUTPUTSKIPFACTORACK	= (byte) 0xD5;


/*
	static public final int MID_REQSYNCINSETTINGS		(const unsigned char)0xD6

	static public final int MID_REQSYNCINSETTINGSACK	(const unsigned char)0xD7

	static public final int LEN_SYNCINMODE				(const unsigned short)2

	static public final int LEN_SYNCINSKIPFACTOR		(const unsigned short)2

	static public final int LEN_SYNCINOFFSET			(const unsigned short)4

	static public final int MID_SETSYNCINSETTINGS		(const unsigned char)0xD6

	static public final int MID_SETSYNCINSETTINGSACK	(const unsigned char)0xD7



	static public final int MID_REQSYNCOUTSETTINGS		(const unsigned char)0xD8

	static public final int MID_REQSYNCOUTSETTINGSACK	(const unsigned char)0xD9

	static public final int LEN_SYNCOUTMODE				(const unsigned short)2

	static public final int LEN_SYNCOUTSKIPFACTOR		(const unsigned short)2

	static public final int LEN_SYNCOUTOFFSET			(const unsigned short)4

	static public final int LEN_SYNCOUTPULSEWIDTH		(const unsigned short)4

	static public final int MID_SETSYNCOUTSETTINGS		(const unsigned char)0xD8

	static public final int MID_SETSYNCOUTSETTINGSACK	(const unsigned char)0xD9



	static public final int MID_REQERRORMODE			(const unsigned char)0xDA

	static public final int MID_REQERRORMODEACK			(const unsigned char)0xDB

	static public final int LEN_ERRORMODE				(const unsigned short)2

	static public final int MID_SETERRORMODE			(const unsigned char)0xDA

	static public final int MID_SETERRORMODEACK			(const unsigned char)0xDB



	static public final int MID_REQHEADING				(const unsigned char)0x82

	static public final int MID_REQHEADINGACK			(const unsigned char)0x83

	static public final int LEN_HEADING		 			(const unsigned short)4

	static public final int MID_SETHEADING				(const unsigned char)0x82

	static public final int MID_SETHEADINGACK			(const unsigned char)0x83



	static public final int MID_REQLOCATIONID			(const unsigned char)0x84

	static public final int MID_REQLOCATIONIDACK		(const unsigned char)0x85

	static public final int LEN_LOCATIONID				(const unsigned short)2

	static public final int MID_SETLOCATIONID			(const unsigned char)0x84

	static public final int MID_SETLOCATIONIDACK		(const unsigned char)0x85



	static public final int MID_REQEXTOUTPUTMODE		(const unsigned char)0x86

	static public final int MID_REQEXTOUTPUTMODEACK		(const unsigned char)0x87

	static public final int LEN_EXTOUTPUTMODE			(const unsigned short)2

	static public final int MID_SETEXTOUTPUTMODE		(const unsigned char)0x86

	static public final int MID_SETEXTOUTPUTMODEACK		(const unsigned char)0x87



	static public final int MID_REQINITTRACKMODE		(const unsigned char)0x88

	static public final int MID_REQINITTRACKMODEACK		(const unsigned char)0x89

	static public final int LEN_INITTRACKMODE			(const unsigned short)2

	static public final int MID_SETINITTRACKMODE		(const unsigned char)0x88

	static public final int MID_SETINITTRACKMODEACK		(const unsigned char)0x89



	static public final int MID_STOREFILTERSTATE		(const unsigned char)0x8A

	static public final int MID_STOREFILTERSTATEACK		(const unsigned char)0x8B

*/

	//	 Measurement state
	static public final byte MID_GOTOCONFIG			= 0x30;
	static public final byte MID_GOTOCONFIGACK			= 0x31;
	static public final byte MID_BUSDATA				= 0x32;
	static public final byte MID_MTDATA				= 0x32;

	//	 Manual
	static public final byte MID_PREPAREDATA			= 0x32;
	static public final byte MID_REQDATA				= 0x34;
	static public final byte MID_REQDATAACK			= 0x35;

	//	 Length of data blocks in bytes
	static public final int LEN_RAWDATA				= 20;
	static public final int LEN_CALIBDATA				= 36;
	static public final int LEN_ORIENT_QUATDATA		= 16;
	static public final int LEN_ORIENT_EULERDATA		= 12;
	static public final int LEN_ORIENT_MATRIXDATA		= 36;
	static public final int LEN_SAMPLECNT				= 2;

	//	 Length of data blocks in floats
	static public final int LEN_CALIBDATA_FLT			= 9;
	static public final int LEN_ORIENT_QUATDATA_FLT	= 4;
	static public final int LEN_ORIENT_EULERDATA_FLT	= 3;
	static public final int LEN_ORIENT_MATRIXDATA_FLT	= 9;

/*

//	 Indices of fields in DATA field of MTData message in bytes

//	 use in combination with LEN_CALIB etc

//	 Un-calibrated raw data

	static public final int IND_RAW_ACCX				0

	static public final int IND_RAW_ACCY				2

	static public final int IND_RAW_ACCZ				4

	static public final int IND_RAW_GYRX				6

	static public final int IND_RAW_GYRY				8

	static public final int IND_RAW_GYRZ				10

	static public final int IND_RAW_MAGX				12

	static public final int IND_RAW_MAGY				14

	static public final int IND_RAW_MAGZ				16

	static public final int IND_RAW_TEMP				18

//	 Calibrated data

	static public final int IND_CALIB_ACCX				0

	static public final int IND_CALIB_ACCY				4

	static public final int IND_CALIB_ACCZ				8

	static public final int IND_CALIB_GYRX				12

	static public final int IND_CALIB_GYRY				16

	static public final int IND_CALIB_GYRZ				20

	static public final int IND_CALIB_MAGX				24

	static public final int IND_CALIB_MAGY				28

	static public final int IND_CALIB_MAGZ				32

//	 Orientation data - quat

	static public final int IND_ORIENT_Q0				0

	static public final int IND_ORIENT_Q1				4

	static public final int IND_ORIENT_Q2				8

	static public final int IND_ORIENT_Q3				12

//	 Orientation data - euler

	static public final int IND_ORIENT_ROLL				0

	static public final int IND_ORIENT_PITCH			4

	static public final int IND_ORIENT_YAW				8

//	 Orientation data - matrix

	static public final int IND_ORIENT_A				0

	static public final int IND_ORIENT_B				4

	static public final int IND_ORIENT_C				8

	static public final int IND_ORIENT_D				12

	static public final int IND_ORIENT_E				16

	static public final int IND_ORIENT_F				20

	static public final int IND_ORIENT_G				24

	static public final int IND_ORIENT_H				28

	static public final int IND_ORIENT_I				32

//	 Orientation data - euler

	static public final int IND_SAMPLECNTH				0

	static public final int IND_SAMPLECNTL				1



//	 Indices of fields in DATA field of MTData message

//	 Un-calibrated raw data

	static public final int FLDNUM_RAW_ACCX				0

	static public final int FLDNUM_RAW_ACCY				1

	static public final int FLDNUM_RAW_ACCZ				2

	static public final int FLDNUM_RAW_GYRX				3

	static public final int FLDNUM_RAW_GYRY				4

	static public final int FLDNUM_RAW_GYRZ				5

	static public final int FLDNUM_RAW_MAGX				6

	static public final int FLDNUM_RAW_MAGY				7

	static public final int FLDNUM_RAW_MAGZ				8

	static public final int FLDNUM_RAW_TEMP				9

//	 Calibrated data

	static public final int FLDNUM_CALIB_ACCX			0

	static public final int FLDNUM_CALIB_ACCY			1

	static public final int FLDNUM_CALIB_ACCZ			2

	static public final int FLDNUM_CALIB_GYRX			3

	static public final int FLDNUM_CALIB_GYRY			4

	static public final int FLDNUM_CALIB_GYRZ			5

	static public final int FLDNUM_CALIB_MAGX			6

	static public final int FLDNUM_CALIB_MAGY			7

	static public final int FLDNUM_CALIB_MAGZ			8

//	 Orientation data - quat

	static public final int FLDNUM_ORIENT_Q0			0

	static public final int FLDNUM_ORIENT_Q1			1

	static public final int FLDNUM_ORIENT_Q2			2

	static public final int FLDNUM_ORIENT_Q3			3

//	 Orientation data - euler

	static public final int FLDNUM_ORIENT_ROLL			0

	static public final int FLDNUM_ORIENT_PITCH			1

	static public final int FLDNUM_ORIENT_YAW			2

//	 Orientation data - matrix

	static public final int FLDNUM_ORIENT_A				0

	static public final int FLDNUM_ORIENT_B				1

	static public final int FLDNUM_ORIENT_C				2

	static public final int FLDNUM_ORIENT_D				3

	static public final int FLDNUM_ORIENT_E				4

	static public final int FLDNUM_ORIENT_F				5

	static public final int FLDNUM_ORIENT_G				6

	static public final int FLDNUM_ORIENT_H				7

	static public final int FLDNUM_ORIENT_I				8

//	 Length

//	 Uncalibrated raw data

	static public final int LEN_RAW_ACCX				2

	static public final int LEN_RAW_ACCY				2

	static public final int LEN_RAW_ACCZ				2

	static public final int LEN_RAW_GYRX				2

	static public final int LEN_RAW_GYRY				2

	static public final int LEN_RAW_GYRZ				2

	static public final int LEN_RAW_MAGX				2

	static public final int LEN_RAW_MAGY				2

	static public final int LEN_RAW_MAGZ				2

	static public final int LEN_RAW_TEMP				2

*/

	//	 Calibrated data
	static public final int LEN_CALIB_ACCX				= 4;
	static public final int LEN_CALIB_ACCY				= 4;
	static public final int LEN_CALIB_ACCZ				= 4;
	static public final int LEN_CALIB_GYRX				= 4;
	static public final int LEN_CALIB_GYRY				= 4;
	static public final int LEN_CALIB_GYRZ				= 4;
	static public final int LEN_CALIB_MAGX				= 4;
	static public final int LEN_CALIB_MAGY				= 4;
	static public final int LEN_CALIB_MAGZ				= 4;

	/*
//	 Orientation data - quat

	static public final int LEN_ORIENT_Q0				4

	static public final int LEN_ORIENT_Q1				4

	static public final int LEN_ORIENT_Q2				4

	static public final int LEN_ORIENT_Q3				4

//	 Orientation data - euler

	static public final int LEN_ORIENT_ROLL				4

	static public final int LEN_ORIENT_PITCH			4

	static public final int LEN_ORIENT_YAW				4

//	 Orientation data - matrix

	static public final int LEN_ORIENT_A				4

	static public final int LEN_ORIENT_B				4

	static public final int LEN_ORIENT_C				4

	static public final int LEN_ORIENT_D				4

	static public final int LEN_ORIENT_E				4

	static public final int LEN_ORIENT_F				4

	static public final int LEN_ORIENT_G				4

	static public final int LEN_ORIENT_H				4

	static public final int LEN_ORIENT_I				4

*/

	//	 Defines for values
	static public final int VALUE_RAW_ACC				= 0;
	static public final int VALUE_RAW_GYR				= 1;
	static public final int VALUE_RAW_MAG				= 2;
	static public final int VALUE_RAW_TEMP				= 3;
	static public final int VALUE_CALIB_ACC			= 4;
	static public final int VALUE_CALIB_GYR			= 5;
	static public final int VALUE_CALIB_MAG			= 6;
	static public final int VALUE_ORIENT_QUAT			= 7;
	static public final int VALUE_ORIENT_EULER			= 8;
	static public final int VALUE_ORIENT_MATRIX		= 9;
	static public final int VALUE_SAMPLECNT			= 10;

	static public final int INVALIDSETTINGVALUE		= 0xFFFFFFFF;

	//	 Valid in all states
	static public final byte MID_RESET				= 0x40;
	static public final byte MID_RESETACK				= 0x41;
	static public final byte MID_ERROR				= 0x42;
	static public final byte LEN_ERROR				= 1;

	/*
//	 XbusMaster

	static public final int MID_XMPWROFF				(const unsigned char)0x44

//	 End XbusMaster



	static public final int MID_REQFILTERSETTINGS		(const unsigned char)0xA0

	static public final int MID_REQFILTERSETTINGSACK	(const unsigned char)0xA1

	static public final int LEN_FILTERSETTINGS			(const unsigned short)4

	static public final int MID_SETFILTERSETTINGS		(const unsigned char)0xA0

	static public final int MID_SETFILTERSETTINGSACK	(const unsigned char)0xA1

	static public final int MID_REQAMD					(const unsigned char)0xA2

	static public final int MID_REQAMDACK				(const unsigned char)0xA3

	static public final int LEN_AMD						(const unsigned short)2

	static public final int MID_SETAMD					(const unsigned char)0xA2

	static public final int MID_SETAMDACK				(const unsigned char)0xA3

	static public final int MID_RESETORIENTATION		(const unsigned char)0xA4

	static public final int MID_RESETORIENTATIONACK		(const unsigned char)0xA5

	static public final int LEN_RESETORIENTATION		(const unsigned short)2



//	 All Messages

//	 WakeUp state messages

	static public final int MSG_WAKEUPLEN				5

	static public final int MSG_WAKEUPACK				(const unsigned char *)"\xFA\xFF\x3F\x00"

	static public final int MSG_WAKEUPACKLEN			4

//	 Config state messages

	static public final int MSG_REQDID					(const unsigned char *)"\xFA\xFF\x00\x00"

	static public final int MSG_REQDIDLEN				4

	static public final int MSG_DEVICEIDLEN				9

	static public final int MSG_INITBUS					(const unsigned char *)"\xFA\xFF\x02\x00"

	static public final int MSG_INITBUSLEN				4

	static public final int MSG_INITBUSRESMAXLEN		(5 + 4 * MAXSENSORS)

	static public final int MSG_REQPERIOD				(const unsigned char *)"\xFA\xFF\x04\x00"

	static public final int MSG_REQPERIODLEN			4

	static public final int MSG_REQPERIODACKLEN			7

	static public final int MSG_SETPERIOD				(const unsigned char *)"\xFA\xFF\x04\x02"

	static public final int MSG_SETPERIODLEN			6

	static public final int MSG_SETPERIODACKLEN			5

	static public final int MSG_SETBID					(const unsigned char *)"\xFA\xFF\x06\x05"

	static public final int MSG_SETBIDLEN				9

	static public final int MSG_SETBIDACKLEN			5

	static public final int MSG_AUTOSTART				(const unsigned char *)"\xFA\xFF\x06\x00"

	static public final int MSG_AUTOSTARTLEN			4

	static public final int MSG_AUTOSTARTACKLEN			5

	static public final int MSG_BUSPWROFF				(const unsigned char *)"\xFA\xFF\x08\x00"

	static public final int MSG_BUSPWROFFLEN			4

	static public final int MSG_BUSPWROFFACKLEN			5

	static public final int MSG_RESTOREFACTORYDEF		(const unsigned char *)"\xFA\xFF\x0E\x00"

	static public final int MSG_RESTOREFACTORYDEFLEN	4

	static public final int MSG_RESTOREFACTORYDEFACKLEN	5

	static public final int MSG_REQDATALENGTH			(const unsigned char *)"\xFA\xFF\x0A\x00"

	static public final int MSG_REQDATALENGTHLEN		4

	static public final int MSG_DATALENGTHLEN			7

	static public final int MSG_REQCONFIGURATION		(const unsigned char *)"\xFA\xFF\x0C\x00"

	static public final int MSG_REQCONFIGURATIONLEN		4

	static public final int MSG_GOTOMEASUREMENT			(const unsigned char *)"\xFA\xFF\x10\x00"

	static public final int MSG_GOTOMEASUREMENTLEN		4

	static public final int MSG_GOTOMEASMAN				(const unsigned char *)"\xFA\x01\x10\x00"

	static public final int MSG_GOTOMEASMANLEN			4

	static public final int MSG_GOTOMEASACKLEN			5

	static public final int MSG_REQFWREV				(const unsigned char *)"\xFA\xFF\x12\x00"

	static public final int MSG_REQFWREVLEN				4

	static public final int MSG_FIRMWAREREVLEN			8

	static public final int MSG_REQBTDISABLED			(const unsigned char *)"\xFA\xFF\x14\x00"

	static public final int MSG_REQBTDISABLEDLEN		4

	static public final int MSG_REQBTDISABLEDACKLEN		6

	static public final int MSG_DISABLEBT				(const unsigned char *)"\xFA\xFF\x14\x01"

	static public final int MSG_DISABLEBTLEN			5

	static public final int MSG_DISABLEBTACKLEN			5

	static public final int MSG_REQOPMODE				(const unsigned char *)"\xFA\xFF\x16\x00"

	static public final int MSG_REQOPMODELEN			4

	static public final int MSG_REQOPMODEACKLEN			6

	static public final int MSG_SETOPMODE				(const unsigned char *)"\xFA\xFF\x16\x01"

	static public final int MSG_SETOPMODELEN			5

	static public final int MSG_SETOPMODEACKLEN			5

	static public final int MSG_REQBAUDRATE				(const unsigned char *)"\xFA\xFF\x18\x00"

	static public final int MSG_REQBAUDRATELEN			4

	static public final int MSG_REQBAUDRATEACKLEN		6	

	static public final int MSG_SETBAUDRATE				(const unsigned char *)"\xFA\xFF\x18\x01"

	static public final int MSG_SETBAUDRATELEN			5

	static public final int MSG_SETBAUDRATEACKLEN		5

	static public final int MSG_REQSYNCMODE				(const unsigned char *)"\xFA\xFF\x1A\x00"

	static public final int MSG_REQSYNCMODELEN			4

	static public final int MSG_REQSYNCMODEACKLEN		6

	static public final int MSG_SETSYNCMODE				(const unsigned char *)"\xFA\xFF\x1A\x01"

	static public final int MSG_SETSYNCMODELEN			5

	static public final int MSG_SETSYNCMODEACKLEN		6

	static public final int MSG_REQMTS					(const unsigned char *)"\xFA\xFF\x90\x01"

	static public final int MSG_REQMTSLEN				5

	static public final int MSG_MTSDATA					61

	static public final int MSG_STORECUSMTS				(const unsigned char *)"\xFA\xFF\x92\x58"

	static public final int MSG_STORECUSMTSLEN			92

	static public final int MSG_STORECUSMTSACKLEN		5

	static public final int MSG_REVTOORGMTS				(const unsigned char *)"\xFA\xFF\x94\x00"

	static public final int MSG_REVTOORGMTSLEN			4

	static public final int MSG_REVTOORGMTSACKLEN		5

	static public final int MSG_STOREMTS				(const unsigned char *)"\xFA\xFF\x96\x41"

	static public final int MSG_STOREMTSLEN				69

	static public final int MSG_STOREMTSACKLEN			5

	static public final int MSG_REQSYNCOUTMODE			(const unsigned char *)"\xFA\xFF\xD8\x01\x00"

	static public final int MSG_REQSYNCOUTMODELEN		5

	static public final int MSG_REQSYNCOUTSKIPFACTOR	(const unsigned char *)"\xFA\xFF\xD8\x01\x01"

	static public final int MSG_REQSYNCOUTSKIPFACTORLEN	5

	static public final int MSG_REQSYNCOUTOFFSET		(const unsigned char *)"\xFA\xFF\xD8\x01\x02"

	static public final int MSG_REQSYNCOUTOFFSETLEN		5

	static public final int MSG_REQSYNCOUTPULSEWIDTH	(const unsigned char *)"\xFA\xFF\xD8\x01\x03"

	static public final int MSG_REQSYNCOUTPULSEWIDTHLEN	5

	static public final int MSG_REQERRORMODE			(const unsigned char *)"\xFA\xFF\xDA\x00"

	static public final int MSG_REQERRORMODELEN			4

	static public final int MSG_REQERRORMODEACKLEN		7

//	 Measurement state - auto messages

	static public final int MSG_GOTOCONFIG				(const unsigned char *)"\xFA\xFF\x30\x00"

	static public final int MSG_GOTOCONFIGLEN			4

	static public final int MSG_GOTOCONFIGACKLEN		5

//	 Measurement state - manual messages (Use DID = 0x01)

	static public final int MSG_GOTOCONFIGM				(const unsigned char *)"\xFA\x01\x30\x00"

	static public final int MSG_GOTOCONFIGMLEN			4

	static public final int MSG_GOTOCONFIGMACKLEN		5

	static public final int MSG_PREPAREDATA				(const unsigned char *)"\xFA\x01\x32\x00"

	static public final int MSG_PREPAREDATALEN			4

	static public final int MSG_REQDATA					(const unsigned char *)"\xFA\x01\x34\x00"

	static public final int MSG_REQDATALEN				4

//	 Valid in all states

	static public final int MSG_RESET					(const unsigned char *)"\xFA\xFF\x40\x00"

	static public final int MSG_RESETLEN				4

	static public final int MSG_RESETACKLEN				5

	static public final int MSG_XMPWROFF				(const unsigned char *)"\xFA\xFF\x44\x00"

	static public final int MSG_XMPWROFFLEN				4

	static public final int MSG_XMPWROFFACKLEN			5



//	 Baudrate defines for SetBaudrate message

	static public final int BAUDRATE_9K6				0x09

	static public final int BAUDRATE_14K4				0x08

	static public final int BAUDRATE_19K2				0x07

	static public final int BAUDRATE_28K8				0x06

	static public final int BAUDRATE_38K4				0x05

	static public final int BAUDRATE_57K6				0x04

	static public final int BAUDRATE_76K8				0x03

	static public final int BAUDRATE_115K2				0x02

	static public final int BAUDRATE_230K4				0x01

	static public final int BAUDRATE_460K8				0x00

	static public final int BAUDRATE_921K6				0x80



//	 Xbus protocol error codes (Error)

	static public final int ERROR_PERIODINVALID			0x03

	static public final int ERROR_MESSAGEINVALID		0x04

	static public final int ERROR_TIMEROVERFLOW			0x1E

	static public final int ERROR_BAUDRATEINVALID		0x20

	static public final int ERROR_PARAMETERINVALID		0x21



//	 Error modes (SetErrorMode)

	static public final int ERRORMODE_IGNORE					0x0000

	static public final int ERRORMODE_INCSAMPLECNT				0x0001

	static public final int ERRORMODE_INCSAMPLECNT_SENDERROR	0x0002

	static public final int ERRORMODE_SENDERROR_GOTOCONFIG		0x0003



//	 Configuration message defines

	static public final int CONF_MASTERDID				0

	static public final int CONF_PERIOD					4

	static public final int CONF_OUTPUTSKIPFACTOR		6

	static public final int CONF_SYNCIN_MODE			8

	static public final int CONF_SYNCIN_SKIPFACTOR		10

	static public final int CONF_SYNCIN_OFFSET			12

	static public final int CONF_DATE					16

	static public final int CONF_TIME					24

	static public final int CONF_NUMDEVICES				96

//	 Configuration sensor block properties

	static public final int CONF_DID					98

	static public final int CONF_DATALENGTH				102

	static public final int CONF_OUTPUTMODE				104

	static public final int CONF_OUTPUTSETTINGS			106

	#define	CONF_BLOCKLEN				20

//	 To calculate the offset in data field for output mode of sensor #2 use

//			CONF_OUTPUTMODE + 1*CONF_BLOCKLEN

	static public final int CONF_MASTERDIDLEN			4

	static public final int CONF_PERIODLEN				2

	static public final int CONF_OUTPUTSKIPFACTORLEN	2

	static public final int CONF_SYNCIN_MODELEN			2

	static public final int CONF_SYNCIN_SKIPFACTORLEN	2

	static public final int CONF_SYNCIN_OFFSETLEN		4

	static public final int CONF_DATELEN				8

	static public final int CONF_TIMELEN				8

	static public final int CONF_RESERVED_CLIENTLEN		32

	static public final int CONF_RESERVED_HOSTLEN		32

	static public final int CONF_NUMDEVICESLEN			2

//	 Configuration sensor block properties

	static public final int CONF_DIDLEN					4

	static public final int CONF_DATALENGTHLEN			2

	static public final int CONF_OUTPUTMODELEN			2

	static public final int CONF_OUTPUTSETTINGSLEN		4



//	 Clock frequency for offset & pulse width

	static public final int SYNC_CLOCKFREQ				29.4912e6



//	 SyncIn params

	static public final int PARAM_SYNCIN_MODE			(const unsigned char)0x00

	static public final int PARAM_SYNCIN_SKIPFACTOR		(const unsigned char)0x01

	static public final int PARAM_SYNCIN_OFFSET			(const unsigned char)0x02



//	 SyncIn mode

	static public final int SYNCIN_DISABLED				0x0000

	static public final int SYNCIN_EDGE_RISING			0x0001

	static public final int SYNCIN_EDGE_FALLING			0x0002

	static public final int SYNCIN_EDGE_BOTH			0x0003

	static public final int SYNCIN_TYPE_SENDLASTDATA	0x0004

	static public final int SYNCIN_TYPE_DOSAMPLING		0x0000

	static public final int SYNCIN_EDGE_MASK			0x0003

	static public final int SYNCIN_TYPE_MASK			0x000C



//	 SyncOut params

	static public final int PARAM_SYNCOUT_MODE			(const unsigned char)0x00

	static public final int PARAM_SYNCOUT_SKIPFACTOR	(const unsigned char)0x01

	static public final int PARAM_SYNCOUT_OFFSET		(const unsigned char)0x02

	static public final int PARAM_SYNCOUT_PULSEWIDTH	(const unsigned char)0x03



//	 SyncOut mode

	static public final int SYNCOUT_DISABLED		0x0000

	static public final int SYNCOUT_TYPE_TOGGLE		0x0001

	static public final int SYNCOUT_TYPE_PULSE		0x0002

	static public final int SYNCOUT_POL_NEG			0x0000

	static public final int SYNCOUT_POL_POS			0x0010

	static public final int SYNCOUT_TYPE_MASK		0x000F

	static public final int SYNCOUT_POL_MASK		0x0010



//	 Sample frequencies (SetPeriod)

	static public final int PERIOD_10HZ				0x2D00

	static public final int PERIOD_12HZ				0x2580

	static public final int PERIOD_15HZ				0x1E00

	static public final int PERIOD_16HZ				0x1C20

	static public final int PERIOD_18HZ				0x1900

	static public final int PERIOD_20HZ				0x1680

	static public final int PERIOD_24HZ				0x12C0

	static public final int PERIOD_25HZ				0x1200

	static public final int PERIOD_30HZ				0x0F00

	static public final int PERIOD_32HZ				0x0E10

	static public final int PERIOD_36HZ				0x0C80

	static public final int PERIOD_40HZ				0x0B40

	static public final int PERIOD_45HZ				0x0A00

	static public final int PERIOD_48HZ				0x0960

	static public final int PERIOD_50HZ				0x0900

	static public final int PERIOD_60HZ				0x0780

	static public final int PERIOD_64HZ				0x0708

	static public final int PERIOD_72HZ				0x0640

	static public final int PERIOD_75HZ				0x0600

	static public final int PERIOD_80HZ				0x05A0

	static public final int PERIOD_90HZ				0x0500

	static public final int PERIOD_96HZ				0x04B0

	static public final int PERIOD_100HZ			0x0480

	static public final int PERIOD_120HZ			0x03C0

	static public final int PERIOD_128HZ			0x0384

	static public final int PERIOD_144HZ			0x0320

	static public final int PERIOD_150HZ			0x0300

	static public final int PERIOD_160HZ			0x02D0

	static public final int PERIOD_180HZ			0x0280

	static public final int PERIOD_192HZ			0x0258

	static public final int PERIOD_200HZ			0x0240

	static public final int PERIOD_225HZ			0x0200

	static public final int PERIOD_240HZ			0x01E0

	static public final int PERIOD_256HZ			0x01C2

	static public final int PERIOD_288HZ			0x0190

	static public final int PERIOD_300HZ			0x0180

	static public final int PERIOD_320HZ			0x0168

	static public final int PERIOD_360HZ			0x0140

	static public final int PERIOD_384HZ			0x012C

	static public final int PERIOD_400HZ			0x0120

	static public final int PERIOD_450HZ			0x0100

	static public final int PERIOD_480HZ			0x00F0

	static public final int PERIOD_512HZ			0x00E1

*/

	//	 OutputModes
	static public final int OUTPUTMODE_MT9					= 0x8000;
	static public final int OUTPUTMODE_XM					= 0x0000;
	static public final int OUTPUTMODE_RAW					= 0x4000;
	static public final int OUTPUTMODE_CALIB				= 0x0002;
	static public final int OUTPUTMODE_ORIENT				= 0x0004;

	//	 OutputSettings
	static public final int OUTPUTSETTINGS_XM				= 0x00000001;
	static public final int OUTPUTSETTINGS_NOTIMESTAMP		= 0x00000000;
	static public final int OUTPUTSETTINGS_SAMPLECNT		= 0x00000001;
	static public final int OUTPUTSETTINGS_QUATERNION		= 0x00000000;
	static public final int OUTPUTSETTINGS_EULER			= 0x00000004;
	static public final int OUTPUTSETTINGS_MATRIX			= 0x00000008;
	static public final int OUTPUTSETTINGS_TIMESTAMP_MASK	= 0x00000003;
	static public final int OUTPUTSETTINGS_ORIENTMODE_MASK	= 0x0000000C;

/*
//	 Extended Output Modes

	static public final int EXTOUTPUTMODE_DISABLED			0x0000

	static public final int EXTOUTPUTMODE_EULER				0x0001



//	 Initial tracking mode (SetInitTrackMode)

	static public final int INITTRACKMODE_DISABLED		0x0000

	static public final int INITTRACKMODE_ENABLED		0x0001



//	 Filter settings params

	static public final int PARAM_FILTER_GAIN			(const unsigned char)0x00

	static public final int PARAM_FILTER_RHO			(const unsigned char)0x01

	static public final int DONOTSTORE					0x00

	static public final int STORE						0x01



//	 AMDSetting (SetAMD)

	static public final int AMDSETTING_DISABLED			0x0000

	static public final int AMDSETTING_ENABLED			0x0001



//	 Reset orientation message type

	static public final int RESETORIENTATION_STORE		0

	static public final int RESETORIENTATION_HEADING	1

	static public final int RESETORIENTATION_GLOBAL		2

	static public final int RESETORIENTATION_OBJECT		3

	static public final int RESETORIENTATION_ALIGN		4



//	 Send raw string mode

	static public final int SENDRAWSTRING_INIT			0

	static public final int SENDRAWSTRING_DEFAULT		1

	static public final int SENDRAWSTRING_SEND			2




//	 openPort baudrates

	#ifdef WIN32

	static public final int PBR_9600					CBR_9600

	static public final int PBR_14K4					CBR_14400

	static public final int PBR_19K2					CBR_19200

	static public final int PBR_28K8					28800

	static public final int PBR_38K4					CBR_38400

	static public final int PBR_57K6					CBR_57600

	static public final int PBR_76K8					76800

	static public final int PBR_115K2					CBR_115200

	static public final int PBR_230K4					230400

	static public final int PBR_460K8					460800

	static public final int PBR_921K6					921600

	#else

	static public final int PBR_9600					B9600

	static public final int PBR_14K4					B14400

	static public final int PBR_19K2					B19200

	static public final int PBR_28K8					B28800

	static public final int PBR_38K4					B38400

	static public final int PBR_57K6					B57600

	static public final int PBR_76K8					B76800

	static public final int PBR_115K2					B115200

	static public final int PBR_230K4					B230400

	static public final int PBR_460K8					B460800

	static public final int PBR_921K6					B921600

	#endif



//	 setFilePos defines

	#ifdef WIN32

	static public final int FILEPOS_BEGIN				FILE_BEGIN

	static public final int FILEPOS_CURRENT				FILE_CURRENT

	static public final int FILEPOS_END					FILE_END

	#else

	static public final int FILEPOS_BEGIN				SEEK_SET

	static public final int FILEPOS_CURRENT				SEEK_CUR

	static public final int FILEPOS_END					SEEK_END

	#endif



//	 Return values

	static public final int XBRV_OK						0	// Operation successful

	static public final int XBRV_NOTSUCCESSFUL			1	// General no success return value

	static public final int XBRV_TIMEOUT				2	// Operation aborted because of a timeout

	static public final int XBRV_CHECKSUMFAULT			3	// Checksum fault occured

	static public final int XBRV_NODATA					4	// No data is received

	static public final int XBRV_RECVERRORMSG			5	// A error message is received

	static public final int XBRV_OUTOFMEMORY			6	// No internal memory available

	static public final int XBRV_UNKNOWDATA				7	// An invalid message is read

	static public final int XBRV_INVALIDTIMEOUT			8	// An invalid value is used to set the timeout

	static public final int XBRV_UNEXPECTEDMSG			9	// Unexpected message received (e.g. no acknowledge message received)

	static public final int XBRV_INPUTCANNOTBEOPENED	10	// The specified file / serial port can not be opened

	static public final int XBRV_ANINPUTALREADYOPEN		11	// File and serial port can not be opened at same time

	static public final int XBRV_ENDOFFILE				12	// End of file is reached

	static public final int XBRV_NOINPUTINITIALIZED		13	// No file or serial port opened for reading/writing

	static public final int XBRV_NOVALIDMODESPECIFIED	14	// No valid outputmode or outputsettings are specified (use 

											// mtGetDeviceMode or mtSetMode)

	static public final int XBRV_INVALIDVALUESPEC		15	// Value specifier does not match value type or not available in data

	static public final int XBRV_INVALIDFORFILEINPUT	16	// Function is not valid for file based interfaces

*/
	
	static public final String		TX		= "TX";
	static public final String		RX		= "RX";
	
	protected SerialPort				serial;
	protected InputStream				input;
	protected OutputStream			output;
	protected byte[]					ibuffer	= new byte[MAXMSGLEN];	// Buffer for serial read
	protected byte[]					obuffer	= new byte[MAXMSGLEN];	// Buffer for serial write
	protected byte[]					rbuffer	= new byte[MAXMSGLEN];	// Buffer for raw received data
	private int						nTempBufferLen = 0;

	// OutputMode, OutputSettings & DataLength for connected devices + 1 for master
	protected int[]					devMode			= new int[MAXDEVICES+1];
	protected int[]					devSettings		= new int[MAXDEVICES+1];
	protected int[]					devDataLength	= new int[MAXDEVICES+1];

	protected double[]				values	= new double[10];	
	protected InsData				data		= new InsData ();
	private boolean					valid;
	private boolean					debug	= false;
	
	public XSens ()
	{
		super ();
	}
	
	// Class methods
	static public void intToBytes (int value, byte[] buffer, int offset, int size)
	{
		// The MTi is endian reversed wrt Java
		for (int i = 0; i < size; i++)
		{
			buffer[offset+(size-1)-i] = (byte) (value & 0x000000FF);
			value = value >> 8;
		}
	}

	static public int bytesToInt (byte[] buffer, int offset, int size)
	{	
		int			value = 0;
		int			aux1, aux2;
			
		// The MTi is endian reversed wrt Java
		for (int i = 0; i < size; i++)
		{
			aux1 = 0;
			aux2 = 0xFF;
			for (int j = 0; j < (size-1)-i; j++)
			{
				aux1 = aux1 + 8;
				aux2 = aux2 << 8;
			}
			value += (int) ((buffer[offset+i] << aux1) & aux2);
		}
		if (buffer[offset] < 0)
		{
			aux2 = 0xFF;
			for (int j = 0; j < size-1; j++)
				aux2 = (aux2 << 8) + 0xFF;

			value--;
			value = value ^ aux2;
			value = -value;
		}
		
		return value;
	}

	static public float bytesToFloat (byte[] buffer, int offset)
	{	
		return Float.intBitsToFloat (bytesToInt (buffer, offset, 4));
	}

	/* ---------------------------- */
	/*        INITIALISATION        */
	/* ---------------------------- */
	protected void initialise () throws XSensException
	{
		// ------------------
		// Open serial port
		// ------------------
		CommPortIdentifier	id;

		try
		{
			id 		= CommPortIdentifier.getPortIdentifier (port);
			serial	= (SerialPort) id.open ("MTi", 1000);
			serial.setSerialPortParams (115200, 8, 2, SerialPort.PARITY_NONE);
			serial.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serial.enableReceiveTimeout (SER_TIMEOUT);
			
			input	= serial.getInputStream ();
			output	= serial.getOutputStream ();
		} catch (Exception e) { throw new XSensException (e.getMessage()); }

		// ------------------
		// Configure sensor
		// ------------------
		
		// Put MTi/MTx in Config State
		writeMessage (MID_GOTOCONFIG);
		// reqSettingInt (MID_REQDID, BID_MASTER);
		setDeviceMode (OUTPUTMODE_ORIENT + OUTPUTMODE_CALIB, OUTPUTSETTINGS_EULER, BID_MASTER);	
		setSetting (MID_SETOUTPUTSKIPFACTOR, 0xFFFF, LEN_OUTPUTSKIPFACTOR, BID_MASTER); 
		
		// Put MTi/MTx in Measurement State
		writeMessage (MID_GOTOMEASUREMENT);
	}

	public void close () throws XSensException
	{
		try
		{
			input.close ();
			output.close ();
			serial.close ();
		} catch (Exception e) { throw new XSensException (e.getMessage()); }
	}
	
	protected void writeMessage (byte mid) throws XSensException
	{	
		writeMessage (mid, 0, (byte)0, BID_MASTER);
	}
	
	protected void writeMessage (byte mid, int value, byte len, byte bid) throws XSensException
	{	
		obuffer[IND_PREAMBLE] = PREAMBLE;		
		obuffer[IND_BID] = bid;		
		obuffer[IND_MID] = mid;		
		obuffer[IND_LEN] = len;
		intToBytes (value, obuffer, IND_DATA0, len);
		calcChecksum (obuffer, LEN_MSGHEADER + len);
		
		// Send message
		try
		{
			if (debug)	dumpFrame (TX, obuffer, LEN_MSGHEADERCS + len);	
			output.write (obuffer, 0, LEN_MSGHEADERCS + len);	
		} catch (Exception e) { throw new XSensException ("writeMessage() " + e.getMessage ()); }

		// Keep reading until an Ack or Error message is received (or timeout)
		long		clkStart;
		int		msgLen;
		
		clkStart = System.currentTimeMillis ();
		do
		{
			msgLen = readMessageRaw (rbuffer);
			if (msgLen > 0)
			{
				if (rbuffer[IND_MID] == (mid+1))
					return;
				else if (rbuffer[IND_MID] == MID_ERROR)
					throw new XSensException ("writeMessage() error <" + rbuffer[IND_DATA0] + ">");
			}
		} while (System.currentTimeMillis () - clkStart < DEF_TIMEOUT);
		
		throw new XSensException ("writeMessage() timeout");
	}

	protected void readMessage (byte mid, byte[] data, byte bid) throws XSensException
	{	
		readMessage (mid, data, 0, (byte)0, bid);
	}
	
	protected void readMessage (byte mid, byte[] data, int value, byte len, byte bid) throws XSensException
	{
		obuffer[IND_PREAMBLE] = PREAMBLE;		
		obuffer[IND_BID] = bid;		
		obuffer[IND_MID] = mid;		
		obuffer[IND_LEN] = len;
		intToBytes (value, obuffer, IND_DATA0, len);
		calcChecksum (obuffer, LEN_MSGHEADER + len);
		
		// Send message
		try
		{
			if (debug)	dumpFrame (TX, obuffer, LEN_MSGHEADERCS + len);	
			output.write (obuffer, 0, LEN_MSGHEADERCS + len);	
		} catch (Exception e) { throw new XSensException ("readMessage() " + e.getMessage ()); }

	
		// Keep reading until an Ack or Error message is received (or timeout)
		long		clkStart;
		int		msgLen;
		int		dataLen;
		
		clkStart = System.currentTimeMillis ();
		do
		{
			msgLen = readMessageRaw (rbuffer);
			if (msgLen > 0)
			{
				if (rbuffer[IND_MID] == MID_MTDATA)
				{
					if (rbuffer[IND_LEN] != EXTLENCODE)
					{
						dataLen = rbuffer[IND_LEN];
						System.arraycopy (rbuffer, IND_DATA0, dbuffer, 0, dataLen);
						return;
					}
					else
					{
						dataLen = rbuffer[IND_LENEXTH]*256 + rbuffer[IND_LENEXTL];
						System.arraycopy (rbuffer, IND_DATAEXT0, data, 0, dataLen);					
						return;
					}
				}
				else if (rbuffer[IND_MID] == MID_ERROR)
					throw new XSensException ("readMessage() error <" + rbuffer[IND_DATA0] + ">");
			}
		} while (System.currentTimeMillis () - clkStart < DEF_TIMEOUT);
		
		throw new XSensException ("readMessage() timeout");
	}
	
	protected void setSetting (byte mid, int value, int len, byte bid) throws XSensException
	{	
		obuffer[IND_PREAMBLE] = PREAMBLE;		
		obuffer[IND_BID] = bid;		
		obuffer[IND_MID] = mid;		
		obuffer[IND_LEN] = (byte) len;
		intToBytes (value, obuffer, IND_DATA0, len);
		calcChecksum (obuffer, LEN_MSGHEADER + len);
		
		// Send message
		try
		{
			if (debug)	dumpFrame (TX, obuffer, LEN_MSGHEADERCS + len);	
			output.write (obuffer, 0, LEN_MSGHEADERCS + len);	
		} catch (Exception e) { throw new XSensException ("setSetting() " + e.getMessage ()); }

		// Keep reading until an Ack or Error message is received (or timeout)
		long		clkStart;
		int		msgLen;
		
		clkStart = System.currentTimeMillis ();
		do
		{
			msgLen = readMessageRaw (rbuffer);
			if (msgLen > 0)
			{
				if (rbuffer[IND_MID] == (mid+1))
					return;
				else if (rbuffer[IND_MID] == MID_ERROR)
					throw new XSensException ("writeMessage() error <" + rbuffer[IND_DATA0] + ">");
			}
		} while (System.currentTimeMillis () - clkStart < DEF_TIMEOUT*10);
		
		throw new XSensException ("setSetting() timeout");
	}

	protected int reqSettingInt (byte mid, byte bid) throws XSensException
	{
		obuffer[IND_PREAMBLE] = PREAMBLE;
		obuffer[IND_BID] = bid;
		obuffer[IND_MID] = mid;
		obuffer[IND_LEN] = 0;		
		calcChecksum (obuffer, LEN_MSGHEADER);
		
		// Send message
		try
		{
			if (debug)	dumpFrame (TX, obuffer, LEN_MSGHEADERCS);	
			output.write (obuffer, 0, LEN_MSGHEADERCS);	
		} catch (Exception e) { throw new XSensException ("reqSettingInt() " + e.getMessage ()); }

		// Keep reading until an Ack or Error message is received (or timeout)
		long		clkStart;
		int		msgLen;
		
		clkStart = System.currentTimeMillis ();
		do
		{
			msgLen = readMessageRaw (rbuffer);
			if (msgLen > 0)
			{
				if (rbuffer[IND_MID] == (mid+1))
				{
					return bytesToInt (rbuffer, IND_DATA0, rbuffer[IND_LEN]);
				}
				else if (rbuffer[IND_MID] == MID_ERROR)
					throw new XSensException ("reqSettingsInt() error <" + rbuffer[IND_DATA0] + ">");
			}
		} while (System.currentTimeMillis () - clkStart < DEF_TIMEOUT);
		
		throw new XSensException ("reqSettingsInt() timeout");
	}

	protected void setDeviceMode (int mode, int settings, byte bid) throws XSensException
	{
		setSetting (MID_SETOUTPUTMODE, mode, LEN_OUTPUTMODE, bid);
		setSetting (MID_SETOUTPUTSETTINGS, settings, LEN_OUTPUTSETTINGS, bid);

		if (bid == BID_MASTER)
		{
			devMode[0] = devMode[BID_MT] = mode;
			devSettings[0] = devSettings[BID_MT] = settings;
		}
		else
		{
			devMode[bid] = mode;
			devSettings[bid] = settings;
		}
		
		// Get DataLength from device
		if (mode != OUTPUTMODE_XM)
		{
			int value;

			value = reqSettingInt (MID_REQDATALENGTH, bid);
			if (bid == BID_MASTER)
				devDataLength[0] = devDataLength[BID_MT] = value;
			else
				devDataLength[bid] = value;
		}
		else
			devDataLength[0] = LEN_SAMPLECNT;
	}

	protected void setMode (int mode, int settings, byte bid)
	{
		byte			nbid = bid;

		if (nbid == BID_MASTER)
			nbid = 0;

		devMode[nbid] = mode;
		devSettings[nbid] = settings;

		if (mode == INVALIDSETTINGVALUE || settings == INVALIDSETTINGVALUE)
			devDataLength[nbid] = 0;
		else
		{
			int dataLength = 0;

			if ((mode & OUTPUTMODE_MT9) > 0)
				dataLength = ((settings & OUTPUTSETTINGS_TIMESTAMP_MASK) == OUTPUTSETTINGS_SAMPLECNT)?LEN_SAMPLECNT:0 + LEN_RAWDATA;
			else if (mode == OUTPUTMODE_XM)
				dataLength = LEN_SAMPLECNT;
			else
			{
				if ((mode & OUTPUTMODE_RAW) > 0)
					dataLength = LEN_RAWDATA;
				else
				{
					if ((mode & OUTPUTMODE_CALIB) > 0) 
						dataLength = LEN_CALIBDATA;
					if ((mode & OUTPUTMODE_ORIENT) > 0)
						switch (settings & OUTPUTSETTINGS_ORIENTMODE_MASK)
						{
						case OUTPUTSETTINGS_QUATERNION:
							dataLength += LEN_ORIENT_QUATDATA;
							break;

						case OUTPUTSETTINGS_EULER:
							dataLength += LEN_ORIENT_EULERDATA;
							break;

						case OUTPUTSETTINGS_MATRIX:
							dataLength += LEN_ORIENT_MATRIXDATA;
							break;

						default:
						}
				}

				switch (settings & OUTPUTSETTINGS_TIMESTAMP_MASK)
				{
				case OUTPUTSETTINGS_SAMPLECNT:
					dataLength += LEN_SAMPLECNT;
					break;

				default:
				}
			}
			devDataLength[nbid] = dataLength;
		}

		// If not XbusMaster store also in BID_MT
		if (bid == BID_MASTER && mode != OUTPUTMODE_XM)
		{
			devMode[BID_MT] = devMode[0];
			devSettings[BID_MT] = devSettings[0];
			devDataLength[BID_MT] = devDataLength[0];
		}
	}

	protected void getValue (int valueSpec, float[] value, byte[] data, byte bid)
	{
		short offset = 0;
		byte nbid = bid;
			
		if (nbid == BID_MASTER)
			nbid = 0;
		
		//	Calculate offset for XM input
		if (devMode[0] == OUTPUTMODE_XM)
		{		
			int i = 0;
			
			while (i < nbid) 			
				offset += devDataLength[i++];
		}
			
		// Check if data is float & available in data
		if (valueSpec >= VALUE_CALIB_ACC && valueSpec <= VALUE_CALIB_MAG)
		{		
			if ((devMode[nbid] & OUTPUTMODE_CALIB) > 0) 
			{
				offset += (valueSpec-VALUE_CALIB_ACC)*LEN_CALIB_ACCX*3;

				for (int i = 0; i < 3; i++)
					value[i] = bytesToFloat (data, offset+i*LEN_FLOAT);
			}
		}
		else if (valueSpec >= VALUE_ORIENT_QUAT && valueSpec <= VALUE_ORIENT_MATRIX)
		{
			offset += ((devMode[nbid] & OUTPUTMODE_CALIB) != 0)?LEN_CALIBDATA:0;
			
			if ((devMode[nbid] & OUTPUTMODE_ORIENT) > 0)
			{
				int orientmode = devSettings[nbid] & OUTPUTSETTINGS_ORIENTMODE_MASK;				
				int nElements = 0;
				
				switch (valueSpec)
				{
				case VALUE_ORIENT_QUAT:
					if (orientmode == OUTPUTSETTINGS_QUATERNION)
						nElements = LEN_ORIENT_QUATDATA / LEN_FLOAT;
					break;
					
				case VALUE_ORIENT_EULER:					
					if (orientmode == OUTPUTSETTINGS_EULER)						
						nElements = LEN_ORIENT_EULERDATA / LEN_FLOAT;						
					break;
					
				case VALUE_ORIENT_MATRIX:					
					if (orientmode == OUTPUTSETTINGS_MATRIX)					
						nElements = LEN_ORIENT_MATRIXDATA / LEN_FLOAT;
					break;
					
				default:
				}
				
				for (int i = 0; i < nElements; i++)
					value[i] = bytesToFloat (data, offset+i*LEN_FLOAT);
			}			
		}
	}

	private int readMessageRaw (byte[] msgBuffer)
	{
		long		clkStart;
		int		state = 0;
		int		nBytesToRead = 1;
		int		nBytesRead = 0;
		int		nOffset = 0;
		int		nMsgDataLen = 0;
		int		nMsgLen;
		boolean	synced = false;

		// Copy previous read bytes if available
		if (nTempBufferLen > 0) {
			System.arraycopy (ibuffer, 0, msgBuffer, 0, nTempBufferLen);
			nBytesRead = nTempBufferLen;
			nTempBufferLen = 0;
		}

		clkStart = System.currentTimeMillis ();		// Get current processor time	
		do
		{
			do
			{
				// First check if we already have some bytes read
				if (nBytesRead > 0 && nBytesToRead > 0) 
				{
					if (nBytesToRead > nBytesRead) 
					{
						nOffset += nBytesRead;
						nBytesToRead -= nBytesRead;
						nBytesRead = 0;
					}
					else
					{
						nOffset += nBytesToRead;
						nBytesRead -= nBytesToRead;
						nBytesToRead = 0;
					}
				}

				// Check if serial port buffer must be read
				if (nBytesToRead > 0)
				{
					try { nBytesRead = input.read (msgBuffer, nOffset, nBytesToRead); } catch (Exception e) { e.printStackTrace (); nBytesRead = 0; }
					
					nOffset += nBytesRead;
					nBytesToRead -= nBytesRead;
					nBytesRead = 0;
				}

				if(nBytesToRead == 0)
				{
					switch(state)
					{
					case 0:					// Check preamble
						if(msgBuffer[IND_PREAMBLE] == PREAMBLE){
							state++;
							nBytesToRead = 3;
						}
						else
						{
							nOffset = 0;
							nBytesToRead = 1;
						}
						break;

					case 1:					// Check ADDR, MID, LEN
						if (msgBuffer[IND_LEN] != 0xFF)
						{
							state = 3;
							nBytesToRead = (nMsgDataLen = msgBuffer[IND_LEN]) + 1; // Read LEN + 1 (chksum)
						}
						else 
						{
							state = 2;
							nBytesToRead = 2;	// Read extended length
						}	
						break;

					case 2:
						state = 3;
						nBytesToRead = (nMsgDataLen = msgBuffer[IND_LENEXTH] * 256 + msgBuffer[IND_LENEXTL]) + 1;	// Read LENEXT + CS
						if (nMsgDataLen > MAXMSGLEN-LEN_MSGEXTHEADERCS)
						{
							// Not synced - check for preamble in the bytes read
							for (int i = 1; i < LEN_MSGEXTHEADER; i++) 
							{
								if (msgBuffer[i] == PREAMBLE) 
								{
									// Found a maybe preamble - 'fill buffer'
									nBytesRead = LEN_MSGEXTHEADER - i;
									System.arraycopy (msgBuffer, i, msgBuffer, 0, nBytesRead);
									break;
								}
							}
							synced = false;
							nOffset = 0;
							state = 0;
							nBytesToRead = 1;			// Start looking for preamble
						}
						break;

					case 3:					// Check msg
						nMsgLen = nMsgDataLen + 5 + (msgBuffer[IND_LEN] == 0xFF?2:0);
						if(checkChecksum (msgBuffer, nMsgLen))
						{				// Checksum ok?
							if (nBytesRead > 0)
							{			// Store bytes if read too much
								System.arraycopy (ibuffer, nMsgLen, msgBuffer, 0, nBytesRead);
								nTempBufferLen = nBytesRead;
							}
							synced = true;
							
							if (debug)	dumpFrame (RX, msgBuffer, nMsgLen);
							return nMsgLen;
						}
						else
						{
							if(!synced)
							{
								// Not synced - maybe recheck for preamble in this incorrect message
								for (int i = 1; i < nMsgLen; i++) 
								{
									if (msgBuffer[i] == PREAMBLE) 
									{
										// Found a maybe preamble - 'fill buffer'
										nBytesRead = nMsgLen - i;
										System.arraycopy (msgBuffer, i, msgBuffer, 0, nBytesRead);
										break;
									}
								}
							}
							synced = false;
							nOffset = 0;
							state = 0;
							nBytesToRead = 1;			// Start looking for preamble
						}
						break;
					}
				}
			} while (((System.currentTimeMillis() - clkStart) < DEF_TIMEOUT) || nBytesRead != 0);

			// Check if pending message has a valid message
			if(state > 0)
			{
				int i;

				// Search for preamble
				for (i = 1; i < nOffset; i++) 
				{
					if (msgBuffer[i] == PREAMBLE) 
					{
						// Found a maybe preamble - 'fill buffer'
						nBytesRead = nOffset-i-1; // no preamble
						System.arraycopy (msgBuffer, i+1, msgBuffer, 1, nBytesRead);
						break;
					}
				}

				if (i < nOffset) 
				{
					// Found preamble in message - recheck
					nOffset = 1;
					state = 1;
					nBytesToRead = 3;			// Start looking for preamble

					continue;
				}
			}
			break;

		} while (true);

		return 0;
	}

	private void dumpFrame (String mode, byte[] buffer, int size)
	{
		System.out.print (mode + " [");
		for (int i = 0; i < size; i++)
			System.out.print (Integer.toHexString (buffer[i] & 0xFF)+ " ");
		System.out.println ("]");
	}
	
	private void calcChecksum (byte[] msgBuffer, int msgBufferLength)
	{
		int checkSum = 0;
		int i;

		for(i = 1; i < msgBufferLength; i++)		
			checkSum += msgBuffer[i] & 0xFF;
		checkSum = checkSum & 0xFF;
		
		msgBuffer[msgBufferLength] = (byte) -checkSum;		
	}

	private boolean checkChecksum (byte[] msgBuffer, int msgBufferLength)
	{
		byte checkSum = 0;
		int i;

		for (i = 1; i < msgBufferLength; i++)
			checkSum += msgBuffer[i];

		return (checkSum == 0);
	}

	private byte[]			dbuffer	= new byte[MAXMSGLEN];
	protected float[]		fdata	= new float[18];

	public InsData getData ()
	{
		try
		{
			readMessage (MID_REQDATA, dbuffer, BID_MASTER);

			getValue (VALUE_ORIENT_EULER, fdata, dbuffer, BID_MASTER);
			data.setRoll (fdata[0] * Angles.DTOR);
			data.setPitch (fdata[1] * Angles.DTOR);
			data.setYaw (fdata[2] * Angles.DTOR);

			getValue(VALUE_CALIB_ACC, fdata, dbuffer, BID_MASTER);
			data.setAccX (fdata[0]);
			data.setAccY (fdata[1]);
			data.setAccZ (fdata[2]);

			getValue(VALUE_CALIB_GYR, fdata, dbuffer, BID_MASTER);
			data.setRollRate (fdata[0]);
			data.setPitchRate (fdata[1]);
			data.setYawRate (fdata[2]);
	
			getValue(VALUE_CALIB_MAG, fdata, dbuffer, BID_MASTER);
			data.setMagX (fdata[0]);
			data.setMagY (fdata[1]);
			data.setMagZ (fdata[2]);
		}
		catch (Exception e) { e.printStackTrace (); }
		
		return data;
	}
}