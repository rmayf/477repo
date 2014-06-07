// Reid Mayfield, 5/21/2014 6:47pm
//
// Echo, Audio Detection Device v2.4

//#define _AUTOMATIC_SETUP 
#define _DEBUG_PRINTING 
#define _INTERNET

#ifdef _DEBUG_PRINTING
#  define _dbprint(x) UARTWrite(1, x)
#else
#  define _dbprint(x) do {} while (0)
#endif


#include "taskFlyport.h"
#include "spi_custom.h"


#define SERV_PORT "6969"

#ifdef _INTERNET
#  define SERV_IP_ADDR "128.208.1.164"
#else
#  define SERV_IP_ADDR "192.168.8.104"
#endif

#define UPLOAD_HEADER_LEN 11
#define TXBUF_SIZE 4096
#define MAGIC_NUM 0xdeadbabe
#define SAMPLE_FREQ 0x2B11 // 11025 Hz
#define BIT_DEPTH 8
#define OPT_SETUP 2
#define OPT_UPLOAD 1
#define ENVELOPE_HISTORY_SIZE 1024


#define ENVELOPE_FIXED_THRES 120
#define MEM_SEQUENTIAL_READ 0x3
#define MEM_SEQUENTIAL_WRITE 0x2
#define SPI_CLK_PIN p18
#define SPI_OUT_PIN p17
#define SPI_IN_PIN p7
#define SPI_SS_PIN p8
/*
 * Global Variables
 */
BYTE txbuf[TXBUF_SIZE];
unsigned int env_hist[ENVELOPE_HISTORY_SIZE];
BOOL ParamSet = FALSE;
unsigned long long counter = 0;
const unsigned long long num_samps = 32768;
TCP_SOCKET server = INVALID_SOCKET;
BOOL TIMER = TRUE;
const UINT32 magic_num = MAGIC_NUM;
const UINT32 freq = SAMPLE_FREQ;

//int testI = 0;
//const UINT16 half_max = 0xFFC0 / 2;




void reset() {
  _dbprint("Resest Button Pushed\r\n");
  WFCustomDelete();
  Reset();
}

// ISR for sampling timer
void __attribute__((__interrupt__, auto_psv)) _T4Interrupt( void )
{
  TMR4 = 0;// interrupt counter reinitialized
  TIMER = TRUE;
  IFS1bits.T4IF = 0;//clear interrupt flag
}

void FlyportTask() {
	
  // Initialize the GPIO pins for the reset button
  IOInit(p2, inup);
  IOInit(p2, EXT_INT2);
  INTInit(2, reset, 1);
  INTEnable(2);

  
  // interrupt config for real time timer
  T4CON = 0x00;  //turn off timer(good practise!)
  TMR4 = 0x00;
  T4CONbits.TCKPS = 0; //clock divider=1
  IPC6bits.T4IP = 0x07;  // Priority
  PR4 = 0x5A2; //limit to raise interrupt !!ORIGINAL!! 5AB =1451
  IFS1bits.T4IF = 0; //interrupt flag off
  IEC1bits.T4IE = 1; //interrupt activated

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
    // Allow time for DHCP to connect
  while(!DHCPAssigned);
  //vTaskDelay(50);
  
  #else
  
  if (WFCustomExist()) {
    _dbprint("Loading Custom WIFI\r\n");
    WFCustomLoad();
    WFConnect(WF_CUSTOM);
    while (WFStatus != CONNECTED);
	  // Allow time for DHCP to connect
  while(!DHCPAssigned);
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
	while(!DHCPAssigned);
	while((server = TCPClientOpen(SERV_IP_ADDR, SERV_PORT)) == INVALID_SOCKET) {
	  vTaskDelay(25);
	  _dbprint("TCP Client Open\r\n");
	}
	while (!TCPisConn(server)) {
	  vTaskDelay(25);
	  _dbprint("TCP Client Connecting\r\n");
	}
    memcpy(txbuf, &magic_num, 4);
    txbuf[4] = OPT_SETUP;
    TCPWrite(server, txbuf, 5);
    TCPClientClose(server);
    server = INVALID_SOCKET;
    _dbprint("Sent Setup Complete message to the server\r\n");
  }
  #endif

  
  // Initialize SPI
  cSPIInit(p8, p12, p10, p7);

  // Initialize the environment to use for inside the main while loop
  unsigned int env_idx, prev, curr, next, amount_read;
  UINT64 sum;
  env_idx = 0;
  sum = 0;
  prev = curr = next = ADCVal(1);
  
  // Fill the buffer with starting values
  while( env_idx < ENVELOPE_HISTORY_SIZE)
  {
	next = ADCVal(1);
	if ((curr > next) && (curr > prev)) {
	  env_hist[env_idx++] = curr;
	  sum += curr;
	}
	prev = curr;
	curr = next;
  }
  
  // Amplitude envelope detection loop. This samples the ADC and uses a running average plus a
  // fixed threshold to detect distinct sounds.
  env_idx = 0;
  _dbprint("Echo is Listening...\r\n");
  while(1) {
    next = ADCVal(1);
    if ((curr > next) && (curr > prev)) {
      if (curr > ((sum / ENVELOPE_HISTORY_SIZE) + ENVELOPE_FIXED_THRES)) {

			// Threshold was surpassed.  Sample at a fixed rate and write to sram
			_dbprint("Envelope Surpassed\r\n");
			IOPut(o4, on);
			
			
			// weird type shit was going on with the sampling loop
			// how many bits is int, unsigned, unsigned long, long??
			// using unsigned long long appears to fix the problem and the ADC samples correct number of times
			unsigned long long bytes_written = 0;
			// start sequential write
			cSPIStartSeqWrite(0, num_samps);
			
			// Start Timer
			vTaskSuspendAll();
			T4CONbits.TON = 1; // timer start
			counter = 0;
			while (counter < num_samps) {
				if (TIMER) {
				  counter++;
				  BYTE sample = (BYTE)(ADCVal(1) >> 2);
				  cSPIWriteNextBYTESeq(sample);
				  TIMER = FALSE;
				}
			}
			T4CONbits.TON = 0; // timer stop
			cSPIEndSeqWrite(&bytes_written);
			xTaskResumeAll();
			while (WFStatus != CONNECTED);	
			while(!DHCPAssigned);

			_dbprint("Done Sampling\r\n");

			// A 3 sec clip of 16 bit samples should now be stored in the sram.  These data are 
			// read back in from memory and sent to the server along with metadata about the 
			// sample.
			
			while((server = TCPClientOpen(SERV_IP_ADDR, SERV_PORT)) == INVALID_SOCKET) {
			  vTaskDelay(5);
			  _dbprint("TCP Client Open\r\n");
			}
			while (!TCPisConn(server)) {
			  vTaskDelay(5);
			  _dbprint("TCP connecting\r\n");
			}
			_dbprint("Connected to the server\r\n");
			// Send the header
			memcpy(txbuf, &magic_num, 4);
			txbuf[4] = OPT_UPLOAD;
			memcpy(&txbuf[5], &freq , 4); // SAMPLE_FREQ = 0x2B11 = 11025 Hz
			txbuf[9] = BIT_DEPTH; // BIT_DEPTH = 8
			txbuf[10] = 1;
			TCPWrite(server, (char *)txbuf, UPLOAD_HEADER_LEN);
			amount_read = 0;
			unsigned long long remaining = bytes_written;
			cSPIsetMODE(MODE_SEQ);
			// Read and send the data
			while (remaining > 0) {
				unsigned int to_read = TXBUF_SIZE;
				if (remaining < TXBUF_SIZE) {
					to_read = remaining;
				}
				cSPIReadSeq((WORD)amount_read, (unsigned long)to_read, txbuf);
				
				int tcp_written = to_read;
				while (tcp_written > 0) {
				  tcp_written -= TCPWrite(server, (char *)&txbuf[to_read - tcp_written], tcp_written);
				  vTaskDelay(25);
				}
				
				remaining -= to_read;
				amount_read += to_read;
			  
				_dbprint("Wrote a chunk\r\n");
			}
			// Reset and do it all over again
			TCPClientClose(server);
			server = INVALID_SOCKET;
			_dbprint("Done Sending!\r\n");
			IOPut(o4, off);
		  }
		  
		  sum -= env_hist[env_idx];
		  env_hist[env_idx] = curr;
		  sum += env_hist[env_idx];
		  if (++env_idx >= ENVELOPE_HISTORY_SIZE)
		    env_idx = 0;
    }
    prev = curr;
    curr = next;
  }
  
  //cSPITerminate();
  // I YAM IMMORTAL!!!!  
}
