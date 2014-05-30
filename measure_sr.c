int i = 0;

// ISR that will print the sampling rate and reset i
void stop_sampling() {
  char stupid[40];
  sprintf(stupid, "Sample Rate: %u\r\n ", i);
  UARTWrite(1, "*******************************\r\n");
  UARTWrite(1, stupid);
  i = 0;
}


  // Setup clock
  struct tm dumb;
  RTCCSet(&dumb);
  RTCCAlarmConf(&dumb, REPEAT_INFINITE, EVERY_SEC, stop_sampling);
  
  // Start Alarm
  RTCCAlarmSet(TRUE);
  