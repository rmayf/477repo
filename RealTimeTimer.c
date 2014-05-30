// interrupt config
T4CON = 0;  //turn off timer(good practise!)        
T4CONbits.TCKPS = 0; //clock divider=1
PR4 = 0x5AB; //limit to raise interrupt=1451
TMR4 = 0; // init timer counter value
IFS1bits.T4IF = 0; //interrupt flag off



// Start Timer
IEC1bits.T4IE = 1; //interrupt activated
T4CONbits.TON = 1; // timer start


// ISR for sampling timer
void __attribute__((__interrupt__, auto_psv)) _T4Interrupt( void )
{
  TMR4 = 0;// interrupt counter reinitialized
  audioCollected = FALSE;
  IFS1bits.T4IF = 0;//clear interrupt flag
}