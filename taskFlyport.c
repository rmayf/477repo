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


#include "taskFlyport.h"
#include "spi_custom.h"


#define SERV_PORT "6969"
// #define SERV_IP_ADDR "128.208.1.164"
#define SERV_IP_ADDR "192.168.8.133"
#define UPLOAD_HEADER_LEN 11
#define TXBUF_SIZE 2048
#define MAGIC_NUM 0xdeadbabe
#define OPT_SETUP 2
#define OPT_UPLOAD 1
#define ENVELOPE_HISTORY_SIZE 1024

#define ADC_ZERO 768
// this needs to fixed: zero may change based on exact input voltage to mic
// the initial zero needs to be correctly established somehow

#define ENVELOPE_FIXED_THRES 60
#define MEM_SEQUENTIAL_READ 0x3
#define MEM_SEQUENTIAL_WRITE 0x2
#define MEM_SIZE 65536 // Bytes
#define SPI_CLK_PIN p18
#define SPI_OUT_PIN p17
#define SPI_IN_PIN p7
#define SPI_SS_PIN p8
/*
 * Global Variables
 */
BYTE txbuf[TXBUF_SIZE];
BOOL ParamSet = FALSE;
unsigned long long counter = 0;

void reset() {
  _dbprint("Resest Button Pushed\r\n");
  WFCustomDelete();
  Reset();
}

// ISR for sampling timer
void __attribute__((__interrupt__, auto_psv)) _T4Interrupt( void )
{
  TMR4 = 0;// interrupt counter reinitialized
  WORD val = (WORD)ADCVal(1);
  cSPIWriteNextWORDSeq(val);
  counter += 2;
  IFS1bits.T4IF = 0;//clear interrupt flag
}

void FlyportTask() {
  
  // Initialize the GPIO pins for the reset button
  IOInit(p2, inup);
  IOInit(p2, EXT_INT2);
  INTInit(2, reset, 1);
  INTEnable(2);

  TCP_SOCKET server = INVALID_SOCKET;
  UINT32 magic_num = MAGIC_NUM;
  
  // interrupt config for real time timer
  T4CON = 0;  //turn off timer(good practise!)        
  T4CONbits.TCKPS = 0; //clock divider=1
  PR4 = 0x5AB; //limit to raise interrupt=1451
  TMR4 = 0; // init timer counter value
  IFS1bits.T4IF = 0; //interrupt flag off

  // Initialze the wifi module.  If the device has already been configured,
  // It will connect to the saved network.  Otherwise, the device enters SoftAP mode
  // and requires the user to save setup parameters using the app or http page.
  #ifdef _AUTOMATIC_SETUP
  _dbprint("Automatic Setup\r\n");
  WFSetParam(NETWORK_TYPE, "infra");
  WFSetParam(DHCP_ENABLE, ENABLED);
  WFSetParam(SSID_NAME, "big titays");
  WFSetSecurity(WF_SECURITY_OPEN, "", 0, 0);
  WFConnect(WF_CUSTOM);
  while (WFStatus != CONNECTED);
  //vTaskDelay(50);
  
  #else
  
  if (WFCustomExist()) {
    _dbprint("Loading Custom WIFI\r\n");
    WFCustomLoad();
    WFConnect(WF_CUSTOM);
    while (WFStatus != CONNECTED);
  }
  else {
    _dbprint("Loading Default WIFI\r\n");
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
    TCPClientClose(server);
    server = INVALID_SOCKET;
    _dbprint("Sent Setup Complete message to the server\r\n");
  }
  #endif
  vTaskDelay(100);
  // Initialize SPI
  cSPIInit(p8, p12, p10, p7);

  // Initialize the environment to use for inside the main while loop
  int env_hist[ENVELOPE_HISTORY_SIZE];
  int env_idx, prev, curr, next, mem_idx, txbuf_idx, amount_read;
  //  BYTE samp;
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
			
			// weird type shit was going on with the sampling loop
			// how many bits is int, unsigned, unsigned long, long??
			// using unsigned long long appears to fix the problem and the ADC samples correct number of times
			unsigned long long bytes_written;
			unsigned long long k = 65536;
			// start sequential write
			cSPIStartSeqWrite(0, k);
			char msg3[40];
			sprintf(msg3, "%llu\r\n", k);
			_dbprint(msg3);
			_dbprint("Before loop\r\n");
			
			// Start Timer
            IEC1bits.T4IE = 1; //interrupt activated
			T4CONbits.TON = 1; // timer start
			
			//vTaskDelay();
			for (counter = 0; counter < k;);\
			//xTaskResumeAll();
			
			_dbprint("After loop\r\n");
			char msg2[40];
			sprintf(msg2, "loop ran %llu times\r\n", counter/2);
			_dbprint(msg2);
			cSPIEndSeqWrite(&bytes_written);
		
			/* DEBUG */
			cSPIsetMODE(MODE_BYTE);
			for (counter = 0; counter < k/16384; counter++) {
				BYTE b;
				cSPIRead((WORD)counter,&b);
				char m[40];
				sprintf(m, "read back 0x%X\r\n", b);
				_dbprint(m);
			}
			
			
			_dbprint("Done Sampling\r\n");
			char m[40];
			sprintf(m, "sampled %llu bytes\r\n", bytes_written);
			_dbprint(m);

			// A 3 sec clip of 16 bit samples should now be stored in the sram.  These data are 
			// read back in from memory and sent to the server along with metadata about the 
			// sample.
			
			while((server = TCPClientOpen(SERV_IP_ADDR, SERV_PORT)) == INVALID_SOCKET)
			  vTaskDelay(10);
			while (!TCPisConn(server))
			  vTaskDelay(10);
			_dbprint("Connected to the server\r\n");
			memcpy(txbuf, &magic_num, 4);
			txbuf[4] = OPT_UPLOAD;
			memcpy(&txbuf[5], /*TODO &sampling_rate*/&magic_num, 4);
			txbuf[9] = 8;
			txbuf[10] = 1;
			
			
			txbuf_idx = UPLOAD_HEADER_LEN;
			amount_read = 0;
			unsigned long long remaining = bytes_written;
			
			while (remaining > 0) {
				int to_read = TXBUF_SIZE;
				if (remaining < TXBUF_SIZE) {
					to_read = remaining;
				}
				cSPIReadSeq((WORD)amount_read, (unsigned long)to_read, &txbuf[txbuf_idx]);
				txbuf_idx = 0;
				
				TCPWrite(server, txbuf, to_read);
				
				remaining -= to_read;
				amount_read += (TXBUF_SIZE - txbuf_idx);
			  
				_dbprint("Wrote a chunk\r\n");
			}
			
			TCPClientClose(server);
			server = INVALID_SOCKET;
			_dbprint("Done Sending!\r\n");
		  }
		  
		  sum -= env_hist[env_idx];
		  env_hist[env_idx] = curr;
		  sum += env_hist[env_idx];
		  if (++env_idx == ENVELOPE_HISTORY_SIZE)
		env_idx = 0;
    }
    prev = curr;
    curr = next;
  }
  
  //cSPITerminate();
  
}
