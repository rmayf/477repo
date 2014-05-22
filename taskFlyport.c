// Reid Mayfield, 5/21/2014 6:47pm
//
// Echo, Audio Detection Device v2.3

#define _AUTOMATIC_SETUP 1
#define _DEBUG_PRINTING 1

#ifdef _DEBUG_PRINTING
#  define _dbprint(x) UARTWrite(1, x)
#else
#  define _dbprint(x) do {} while (0)
#endif


#include "SPIHelper.h"
#include "taskFlyport.h"
#define SERV_PORT "6969"
#define SERV_IP_ADDR "128.208.1.164"
#define UPLOAD_HEADER_LEN 11
#define TXBUF_SIZE 2048
#define MAGIC_NUM 0xdeadbabe
#define OPT_SETUP 2
#define OPT_UPLOAD 1
#define ENVELOPE_HISTORY_SIZE 1024
#define ADC_ZERO 512
#define ENVELOPE_FIXED_THRES 60
#define MEM_SEQUENTIAL_READ 0x3
#define MEM_SEQUENTIAL_WRITE 0x2
#define MEM_SIZE 65536 // Bytes
#define SPI_CLK_PIN p18
#define SPI_OUT_PIN p17
#define SPI_IN_PIN p7
#define SPI_SS_PIN p8
#define SPI_SPEED 250000 // TODO can increase up to 8000000
/*
 * Global Variables
 */
char txbuf[TXBUF_SIZE];

void reset() {
  _dbprint("Resest Button Pushed\r\n");
  WFCustomDelete();
  Reset();
}

void FlyportTask() {
  
  // Initialize the GPIO pins for the reset button
  IOInit(p2, inup);
  IOInit(p2, EXT_INT2);
  INTInit(2, reset, 1);
  IntEnable(2);

  TCP_SOCKET server = INVALID_SOCKET;
  UINT32 magic_num = MAGIC_NUM;

  // Initialze the wifi module.  If the device has already been configured,
  // It will connect to the saved network.  Otherwise, the device enters SoftAP mode
  // and required the user to save setup parameters using the app or http page.
  #ifdef _AUTOMATIC_SETUP
  _dbprint("Atomatic Setup\r\n");
  WFSetParam(NETWORK_TYPE, "infra");
  WFSetParam(DHCP_ENABLE, ENABLED);
  WFSetParam(SSID_NAME, "big titays");
  WFSetSecurity(WF_SECURITY_OPEN, "", 0, 0);
  WFConnect(WF_CUSTOM);
  while (WFStatus != CONNECTED);
  #else
  if (WFCustomExist()) {
    _dbprint("Loading Custom WIFI\r\n");
    WFCustomLoad();
    WFConnect(WF_CUSTOM);
    while (WFStatus != CONNECTED);
  }
  else {
    _dbprintf("Loading Default WIFI\r\n");
    WFConnect(WF_DEFAULT);
    while(!ParamSet) {
      vTaskDelay(25);
    }
    WFDisconnect();
    while (WFStatus != NOT_CONNECTED);
    WFCustomSave();
    WFConnect(WF_CUSTOM);
    while (WFStatus != CONNECTED);
    _dbprint("Saved Custom Profile\r\n");
    
    // Send the setupSuccess message to the server 
    server = TCPClientOpen(SERV_IP_ADDR, SERV_PORT);
    while (!TCPisConn(server));
    memcpy(txbuf, &magic_num, 4);
    txbuf[4] = OPT_SETUP;
    TCPWrite(server, txbuf, 5);
    TCPClose(server);
    server = INVALID_SOCKET;
    _dbprint("Sent Setup Complete message to the server\r\n");
  }
  #endif

  // SPI Setup
  IOInit(SPI_CLK_PIN, SPICLKOUT);
  IOInit(SPI_OUT_PIN, SPI_OUT);
  IOInit(SPI_IN_PIN, SPI_IN);
  IOInit(SPI_SS_PIN, out);
  IOPut(SPI_SS_PIN, on);
  IOInit(SPI_SS_PIN, out);
  SPIContext spi_ctx;
  SPIConfig(&spi_ctx, SPI_OPT_MASTER | SPI_OPT_MODE_0, SPI_SS_PIN, SPI_SPEED);
  SPIContextRestore(&spi_ctx);

  // Envelope detection variables
  int env_hist[ENVELOPE_HISTORY_SIZE];
  int env_idx, prev, curr, next, mem_idx, txbuf_idx, amount_read;
  BYTE samp;
  INT64 sum;
  for (env_idx = 0; env_idx < ENVELOPE_HISTORY_SIZE; env_idx++)
    env_hist[env_idx] = 0;
  env_idx = sum = prev = curr = next = 0;

  // Amplitude envelope detection loop. This samples the ADC and uses a running average plus a
  // fixed threshold to detect distinct sounds.
  while(1) {
    next = ADCVal(1) - ADC_ZERO;
    if ((curr > next) && (curr > prev)) {
      if (curr > ((sum / ENVELOPE_HISTORY_SIZE) + ENVELOPE_FIXED_THRES)) {

	// Threshold was surpassed.  Sample at a fixed rate and write to sram
	_dbprint("Envelope Surpassed\r\n");
	SPIOpen();
	SPIWriteByte(MEM_SEQUENTIAL_WRITE);
	SPIWriteByte(0x0);
	vTaskSuspendAll();
	for (mem_idx = 0; mem_idx < MEM_SIZE; mem_idx += 2) {
	  SPIWriteWord(ADCVal(1));
	}
	SPIClose();
	_dbprint("Done Sampling\r\n");

        // A 3 sec sample of 16 bit samples should now be stored in the sram.  These data are 
	// read back in from memory and sent to the server along with metadata about the 
	// sample.
	xTaskResumeAll();
	server = TCPClientOpen(SERV_IP_ADDR, SERV_PORT);
	while (!TCPisConn(server));
	_dbprint("Connected to the server\r\n");
	memcpy(txbuf, &magic_num, 4);
	txbuf[4] = OPT_UPLOAD;
	memcpy(&txbuf[5], /*TODO &sampling_rate*/&magic_num, 4);
	txbuf[9] = 8;
	txbuf[10] = 1;
	SPIOpen();
	SPIWriteByte(MEM_SEQUENTIAL_READ);
	SPIWriteByte(0x0);
	txbuf_idx = UPLOAD_HEADER_LEN;
	amount_read = 0;
	while (amount_read < MEM_SIZE) {
          for (;(txbuf_idx < TXBUF_SIZE) && (amount_read < MEM_SIZE); txbuf_idx++) {
            SPIReadByte(&samp);
	    txbuf[txbuf_idx] = samp;
	    amount_read++;
	  }
	  TCPWrite(server, txbuf, txbuf_idx);
	  _dbprint("Wrote a chunk\r\n");
	  txbuf_idx = 0;
	}
	TCPClose(server);
	server = INVALID_SOCKET;
	SPIClose();
	_dbprint("Done Sending!\r\n");
      }
      sum -= env_hist[env_idx];
      history[env_idx] = curr;
      sum += history[env_idx];
      if (++env_idx == ENVELOPE_HISTORY_SIZE)
	env_idx = 0;
    }
    prev = curr;
    curr = next;
  }
}

