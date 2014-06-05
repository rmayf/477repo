#ifndef SPI_CUSTOM
#define SPI_CUSTOM

#include "HWlib.h"

/* Microchip RAM commands */
#define CMD_RDMR (BYTE)0x05
#define CMD_WRMR (BYTE)0x01

#define MODE_BYTE (BYTE)0x00
#define MODE_PAGE (BYTE)0x80
#define MODE_SEQ (BYTE)0x40
#define MODE_RES (BYTE)0xC0

#define CMD_RD (BYTE)0x03
#define CMD_WR (BYTE)0x02

#define WORD_LOW(a) (BYTE)(a)
#define WORD_HIGH(a) (BYTE)(a>>8)

#define MAX_BYTE_TO_WRITE 65536

BOOL cSPIInit(int pin_sck, int pin_si, int pin_so, int pin_cs);

BOOL cSPIgetMODE(BYTE* mode_out);
BOOL cSPIsetMODE(BYTE mode);

BOOL cSPIRead(WORD addr, BYTE* read_out);
BOOL cSPIWrite(WORD addr, BYTE write_in);

BOOL cSPIReadSeq(WORD addr, unsigned long to_read, BYTE* read_out);
BOOL cSPIWriteSeq(WORD addr, unsigned long to_write, BYTE* write_in);

BOOL cSPIStartSeqWrite(WORD addr, unsigned long long to_write);
BOOL cSPIWriteNextBYTESeq(BYTE write_in);
BOOL cSPIWriteNextWORDSeq(WORD write_in);
BOOL cSPIEndSeqWrite(unsigned long long* bytes_written);

BOOL cSPITerminate();

#endif
