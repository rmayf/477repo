#include "spi_custom.h"
#include "taskFlyport.h"

#define 	_SPIEN  SPI2STATbits.SPIEN
#define 	_SPIRBF SPI2STATbits.SPIRBF
#define    SPIBUF    SPI2BUF
#define    SPICON1   SPI2CON1
#define 	SPICON2   SPI2CON2
 
#define xmit_spi(dat) 	xchg_spi(dat)
#define rcvr_spi()		xchg_spi(0x00)
#define rcvr_spi_m(p)	SPIBUF = 0x00; while (!_SPIRBF); *(p) = (BYTE)SPIBUF;



/* Private Function Prototypes */
static void CS_LOW();
static void CS_HIGH();
static void pinConfig(int _sdsc, int _sdsi, int _sdso, int _sdcs);
static BYTE xchg_spi(BYTE dat);
static void deselect(void);
static int select(void);
static void power_on(void);
static void power_off(void);

/* variables */
int sdsi, sdso, sdcs, sdsc;
extern int *CNPUs[];
extern int *CNPDs[];
extern int CNPos[];
static int addval;

static void CS_LOW()
{
	IOPut(sdcs,OFF);
}


static void CS_HIGH()
{
	IOPut(sdcs, ON);
}

/*-----------------------------------------------------------------------*/
/* Deselect the card and release SPI bus                                 */
/*-----------------------------------------------------------------------*/

static void deselect (void) {
	CS_HIGH();
	//IOPut (o4,off);
	rcvr_spi();
}

/*-----------------------------------------------------------------------*/
/* Select the card and wait ready                                        */
/*-----------------------------------------------------------------------*/

static int select (void) {	/* 1:Successful, 0:Timeout */
	CS_LOW();
  
  /* timeout not used here
	//IOPut (o4,on);
	if (wait_ready() != 0xFF) {
		deselect();
		//IOPut (o4,off);
		return 0;
	}
  */
	return 1;
}

static BYTE xchg_spi (BYTE dat) {
	SPIBUF = dat;
	while (!_SPIRBF);
	return (BYTE)SPIBUF;
}

static void power_on(void) {
  _SPIEN = 1;				// SPISTAT,15

  //config SPI1
  _SPIEN 		= 0;	// disable SPI port
  SPI2STATbits.SPISIDL 	= 0; 	// Continue module operation in Idle mode
  SPI2BUF 				= 0;   	// clear SPI buffer

  IFS0bits.SPI1IF 		= 0;	// clear interrupt flag
  IEC0bits.SPI1IE 		= 0;	// disable interrupt
  SPI2CON1bits.DISSCK		= 0;	// Internal SPIx clock is enabled
  SPI2CON1bits.DISSDO		= 0;	// SDOx pin is controlled by the module
  SPI2CON1bits.MODE16 	= 0;	// set in 16-bit mode, clear in 8-bit mode
  SPI2CON1bits.SMP		= 0;	// Input data sampled at middle of data output time
  SPI2CON1bits.CKE 		= 1;	// Clock edge select
  SPI2CON1bits.SSEN		= 0;	// not used
  SPI2CON1bits.CKP 		= 0;	// CKP polarity 1= active low 0=active high
  SPI2CON1bits.MSTEN 		= 1; 	// 1 =  Master mode; 0 =  Slave mode

	SPI2CON1bits.SPRE 		= 6; 	// Secondary Prescaler = 4:1  (0x100) // was 4
	SPI2CON1bits.PPRE 		= 3; 	// Primary Prescaler = 4:1 (0x10) // was 2
	
//	SPICON                 00111011
//	SPI2CON1 = 0x013B;
//	SPI2CON2 = 0x0000;

  SPI2CON2 				= 0;	// non-framed mode
  
  SPI2STATbits.SPIEN 		= 1; 	// enable SPI port, clear status
}

static void power_off (void) {
	select();			/* Wait for card ready */
	deselect();

	_SPIEN = 0;			/* Disable SPI2 */
}

static void pinConfig(int _sdsc, int _sdsi, int _sdso, int _sdcs) {
  sdsc = _sdsc;
	sdsi = _sdsi;
	sdso = _sdso;
	sdcs = _sdcs;
	
	//SPI2 pins configuration:
	IOInit(sdsi, SPI_IN);
	IOInit(sdsc, SPICLKOUT);
	IOInit(sdso, SPI_OUT);
	
	// init CS line as ouput, default high
	IOInit(sdcs, out); // SD-CS line
	IOPut(sdcs, on);
	
	//	Pullup resistors configuration
	
	addval = 1 << CNPos[sdsc-1];
	addval = ~addval;
	*CNPDs[sdsc-1] = *CNPDs[sdsc-1] & addval;
	addval = ~addval;
	*CNPUs[sdsc-1] = *CNPUs[sdsc-1] | addval;
	
	
	addval = 1 << CNPos[sdsi-1];
	addval = ~addval;
	*CNPDs[sdsi-1] = *CNPDs[sdsi-1] & addval;
	addval = ~addval;
	*CNPUs[sdsi-1] = *CNPUs[sdsi-1] | addval;

	addval = 1 << CNPos[sdso-1];
	addval = ~addval;
	*CNPDs[sdso-1] = *CNPDs[sdso-1] & addval;
	addval = ~addval;
	*CNPUs[sdso-1] = *CNPUs[sdso-1] | addval;
}

/*
 * Public Functions
 *
 */

/* SPI Initialization */
BOOL cSPIInit(int pin_sck, int pin_si, int pin_so, int pin_cs) {
  
  // SDInit() -> pinConfig()
  // config pins for SPI2
  pinConfig(pin_sck, pin_si, pin_so, pin_cs);
  
  // SDInit() -> diskMount() -> disk_initialize() -> power_on()
  // set a bunch of control register bits for SPI2
  power_on();
  
  return TRUE;
}

BOOL cSPIgetMODE(BYTE* mode_out) {
  /* Select the card and wait for ready */
	deselect();
	if (!select()) return FALSE;
  
  // send Read MODE Register command
  xmit_spi(CMD_RDMR);
  BYTE mode = rcvr_spi();
  *mode_out = mode;
  
  deselect();
  
  return TRUE;
}

BOOL cSPIsetMODE(BYTE mode) {
  /* Select the card and wait for ready */
	deselect();
	if (!select()) return FALSE;
  
  // send Write MODE Register command
  xmit_spi(CMD_WRMR);
  // send mode byte
  xmit_spi(mode);
  
  deselect();
  
  return TRUE;
}

BOOL cSPIRead(WORD addr, BYTE* read_out) {
  /* Select the card and wait for ready */
	deselect();
	if (!select()) return FALSE;
  
  // send Read command
  xmit_spi(CMD_RD);
  // send address
  xmit_spi(WORD_HIGH(addr));
  xmit_spi(WORD_LOW(addr));
  // read data byte
  BYTE read = rcvr_spi();
  *read_out = read;
  
  deselect();
  
  return TRUE;
}

BOOL cSPIWrite(WORD addr, BYTE write_in) {
  /* Select the card and wait for ready */
	deselect();
	if (!select()) return FALSE;
  
  // send Write command
  xmit_spi(CMD_WR);
  // send address
  xmit_spi(WORD_HIGH(addr));
  xmit_spi(WORD_LOW(addr));
  // send data byte
  xmit_spi(write_in);
  
  deselect();
  
  return TRUE;
}


BOOL cSPIReadSeq(WORD addr, unsigned long to_read, BYTE* read_out) {
  if (to_read <= 0) {
    return FALSE;
  }
  
  /* Select the card and wait for ready */
	deselect();
	if (!select()) return FALSE;
  
  // send Write command
  xmit_spi(CMD_RD);
  // send address
  xmit_spi(WORD_HIGH(addr));
  xmit_spi(WORD_LOW(addr));
  // send data bytes
  unsigned long i = 0;
  for (i = 0; i < to_read; i++) {
    BYTE read = rcvr_spi();
    read_out[i] = read;
  }
  
  deselect();
  
  return TRUE;
}

BOOL cSPIWriteSeq(WORD addr, unsigned long to_write, BYTE* write_in) {
  /* Select the card and wait for ready */
  deselect();
  if (!select()) return FALSE;
  
  // send Write command
  xmit_spi(CMD_WR);
  // send address
  xmit_spi(WORD_HIGH(addr));
  xmit_spi(WORD_LOW(addr));
  // send data bytes
  unsigned long i = 0;
  for (i = 0; i < to_write; i++) {
    xmit_spi(write_in[i]);
  }
  
  deselect();
  
  return TRUE;
}

static unsigned long BYTES_WRITTEN;

BOOL cSPIStartSeqWrite(WORD addr, unsigned long long to_write) {
  if (to_write > MAX_BYTE_TO_WRITE || to_write <= 0) {
    return FALSE;
  }

  BYTES_WRITTEN = 0;
  
  /* Select the card and wait for ready */
  deselect();
  if (!select()) return FALSE;
  
  // send Write command
  xmit_spi(CMD_WR);
  // send address
  xmit_spi(WORD_HIGH(addr));
  xmit_spi(WORD_LOW(addr));
  
  return TRUE;
}

BOOL cSPIWriteNextBYTESeq(BYTE write_in) {
  if (BYTES_WRITTEN >= MAX_BYTE_TO_WRITE) {
    return FALSE;
  }
	
  BYTES_WRITTEN++;
  // write next byte
  xmit_spi(write_in);
  
  return TRUE;
}

BOOL cSPIWriteNextWORDSeq(WORD write_in) {
  if (BYTES_WRITTEN >= MAX_BYTE_TO_WRITE) {
    return FALSE;
  }
	
  BYTES_WRITTEN += 2;
  // write next byte
  xmit_spi(WORD_HIGH(write_in));
  xmit_spi(WORD_LOW(write_in));
  
  return TRUE;
}

BOOL cSPIEndSeqWrite(unsigned long long* bytes_written) {
  *bytes_written = BYTES_WRITTEN;
	
  deselect();
  
  return TRUE;
}

BOOL cSPITerminate() {
  // disable SPI2
  power_off();
  return TRUE;
}




/*
f_read -> disk_read -> { send_cmd() then rcvr_datablock() } then deselect()

send_cmd() -> deselect() followed by  select() then xmit_spi() = xchg_spi()

rcvr_datablock() -> rcvr_spi() and rcvr_spi_m() = xchg_spi()
  - always follows a send_cmd() to ensure select() was called first (i.e. chip enable)

*/
