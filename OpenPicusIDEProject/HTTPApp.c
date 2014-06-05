/* **************************************************************************																					
 *  Software License Agreement
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  This is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License (version 2) as published by 
 *  the Free Software Foundation AND MODIFIED BY OpenPicus team.
 *  
 *  ***NOTE*** The exception to the GPL is included to allow you to distribute
 *  a combined work that includes OpenPicus code without being obliged to 
 *  provide the source code for proprietary components outside of the OpenPicus
 *  code. 
 *  OpenPicus software is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 *  more details. 
 * 
 * 
 * Warranty
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT
 * WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION, ANY WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 * WE ARE LIABLE FOR ANY INCIDENTAL, SPECIAL, INDIRECT OR
 * CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF
 * PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY OR SERVICES, ANY CLAIMS
 * BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE
 * THEREOF), ANY CLAIMS FOR INDEMNITY OR CONTRIBUTION, OR OTHER
 * SIMILAR COSTS, WHETHER ASSERTED ON THE BASIS OF CONTRACT, TORT
 * (INCLUDING NEGLIGENCE), BREACH OF WARRANTY, OR OTHERWISE.
 *
 **************************************************************************/
 
 
/****************************************************************************
  SECTION 	Include
****************************************************************************/

#include "NETlib.h"
#include "TCPIP Stack/TCPIP.h"
#if defined(STACK_USE_HTTP2_SERVER)
#define __HTTPAPP_C

#define String_post_Len		300
char String_post[String_post_Len];

static BYTE security;
static char PassKey[129];
extern BOOL ParamSet;
// unused! static BOOL WPAKey = FALSE;
 
/****************************************************************************
  FUNCTION	HTTP_IO_RESULT HTTPExecuteGet(void)
	
  This function processes every GET request from the pages. For futher 
  information, please see the "standard webserver" example provided.
*****************************************************************************/
HTTP_IO_RESULT HTTPExecuteGet(void)
{
	return HTTP_IO_DONE;
}


/****************************************************************************
  Section:
	POST Form Handlers
  ***************************************************************************/
#if defined(HTTP_USE_POST)

/*****************************************************************************
  Function:
	HTTP_IO_RESULT HTTPExecutePost(void)

  	This function processes every POST request from the pages. 
  ***************************************************************************/
HTTP_IO_RESULT HTTPExecutePost(void)
{
// Resolve which function to use and pass along
	BYTE filename[20];
	int len;
	// Load the file name
	// Make sure BYTE filename[] above is large enough for your longest name
	MPFSGetFilename(curHTTP.file, filename, sizeof(filename));
	while(curHTTP.byteCount)
	{
		// Check for a complete variable
		len = TCPFind(sktHTTP, '&', 0, FALSE);
		if(len == 0xffff)
		{
			// Check if is the last post, otherwise continue in the loop
			if( TCPIsGetReady(sktHTTP) == curHTTP.byteCount)
				len = curHTTP.byteCount - 1;
			else 
			{	
				return HTTP_IO_NEED_DATA; // No last post, we need more data
			}
		}

	 
		if(len > HTTP_MAX_DATA_LEN - 2)
		{
			// Make sure we don't overflow
			curHTTP.byteCount -= TCPGetArray(sktHTTP, (BYTE*)String_post, len+1);
			continue;
		}

		len = TCPGetArray(sktHTTP,curHTTP.data, len+1);

		curHTTP.byteCount -= len;
		curHTTP.data[len] = '\0';
		HTTPURLDecode(curHTTP.data);
		
		//	NETWORK TYPE SELECTION: ADHOC/INFRASTRUCTURE/SOFTAP(WIFI G only)
		if(memcmppgm2ram(curHTTP.data,(ROM void*)"NETTYPE", 7) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[8], len-8);			
			WFSetParam(NETWORK_TYPE, String_post);
		}		  
		
		//	DHCP CLIENT ENABLING/DISABLING
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"DHCPCL", 6) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[7], len-7);
			if (String_post [0] == 'd')
				WFSetParam(DHCP_ENABLE , DISABLED);
			else 
				WFSetParam(DHCP_ENABLE , ENABLED);
		}	
			
		//	IP ADDRESS OF THE DEVICE
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"IPADDR", 6) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[7], len-7);
			WFSetParam(MY_IP_ADDR, String_post);
		}			
				
		//	SUBNET MASK
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"SUBNET", 6) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[7], len-7);
			WFSetParam(SUBNET_MASK, String_post);
		}		
				
		//	DEFAULT GATEWAY 
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"GATEWAY", 7) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[8], len-8);
			WFSetParam(MY_GATEWAY, String_post); 
		}	
					
		//	DNS SERVER #1
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"DNS1", 4) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[5], len-5);
			WFSetParam(PRIMARY_DNS, String_post);
		}
						
		//	DNS SERVER #2
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"DNS2", 4) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[5], len-5);
			WFSetParam(SECONDARY_DNS, String_post);
		}
									
		//	SSID 
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"SSID", 4) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[5], len-5);
			WFSetParam(SSID_NAME, String_post); 
		}
								
		//	SECURITY TYPE
		else if(memcmppgm2ram(curHTTP.data,(ROM void*)"SECTYPE", 7) == 0)
		{
			memcpy(String_post,(void*)&curHTTP.data[8], len-8);
			
			if (String_post[2] == 'E')
			{
				security = 0;
				WFSetSecurity(WF_SECURITY_OPEN, "", 0, 0);
				ParamSet = TRUE;
			}
			else if (String_post[2] == 'A')
			{
				if (String_post[3] == '2')
					security = 5;
				else 
					security = 3;
			}
			else if (String_post[2] == 'P') 
			{
				if (String_post[3] == '4')
					security = 1;
				else 
					security = 2;
			}			
		}
		
		//	----------	SECURITY KEY AND PASSWORD	----------	
		
		//	WEP40 KEY
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WEP40KEY4", 9) == 0)
		{
			if (security == 1)
			{
				if (len > 10)
				{
					int j = 0, j1 = 0;
					WORD_VAL dummy;
					for ( j=0; j<40; j=j+2)
					{
						memcpy(String_post,(void*)&curHTTP.data[10+j], 2);
					
						dummy.v[1] =  String_post[0];
						dummy.v[0] =  String_post[1];
						PassKey[j1] = hexatob(dummy);
						j1++;
					}
					PassKey[j1]= '\0';
					security = 1;
				}
			}
		}
		
		//	WEP40 KEY INDEX
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WEP40KEYID", 10) == 0)
		{	
			memcpy(String_post,(void*)&curHTTP.data[11], len-11);
			
			int k_index;
			k_index = atoi(String_post);
			k_index--;
			if (security == 1)
			{
				WFSetSecurity(WF_SECURITY_WEP_40, PassKey, 20, k_index);
				ParamSet = TRUE;
			}
		}
		
		//	WEP104 KEY INDEX
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WEP104KEY", 9) == 0)
		{
			if (security == 2)
			{
				int j = 0, j1 = 0;
				WORD_VAL dummy;
				for ( j=0; j<32; j=j+2)
				{
					memcpy(String_post,(void*)&curHTTP.data[10+j], 2);
					
					dummy.v[1] =  String_post[0];
					dummy.v[0] =  String_post[1];
					PassKey[j1] = hexatob(dummy);
					j1++;
				}
				PassKey[j1]= '\0';
				WFSetSecurity(WF_SECURITY_WEP_104, PassKey, 16, 0);	
				ParamSet = TRUE;
			}
			
		}
		
		//	WPA WITH PASSPHRASE
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WPAPASS", 7) == 0)
		{	
			if (security == 3)
			{
				if (len > 10)
				{
					memcpy(String_post,(void*)&curHTTP.data[8], len-8);									
					WFSetSecurity(WF_SECURITY_WPA_WITH_PASS_PHRASE, String_post, len-9, 0);
					ParamSet = TRUE;
				}
			}
		}

		//	WPA WITH PASSKEY
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WPAKEY", 6) == 0)
		{	
			if (security == 3)
			{
				if (len > 10)
				{
					int j = 0, j1 = 0;
					WORD_VAL dummy;
					for ( j=0; j<64; j=j+2)
					{
						memcpy(String_post,(void*)&curHTTP.data[7+j], 2);
						
						dummy.v[1] =  String_post[0];
						dummy.v[0] =  String_post[1];
						PassKey[j1] = hexatob(dummy);
						j1++;
					}
					PassKey[j1]= '\0';
					WFSetSecurity(WF_SECURITY_WPA_WITH_KEY, PassKey, 32, 0);
					ParamSet = TRUE;
				}
			}
		}
		
		//	WPA2 WITH PASSPHRASE
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WPA2PASS", 8) == 0)
		{	
			if (len > 10)
			{
				memcpy(String_post,(void*)&curHTTP.data[9], len-9);
							
				WFSetSecurity(WF_SECURITY_WPA2_WITH_PASS_PHRASE, String_post, len-9, 0);
				ParamSet = TRUE;
			}
		}
		
		//	WPA2 WITH PASSKEY
		else if (memcmppgm2ram(curHTTP.data,(ROM void*)"WPA2KEY", 7) == 0)
		{	
			if (len > 10)
			{
				int j = 0, j1 = 0;
				WORD_VAL dummy;
				for ( j=0; j<64; j=j+2)
				{
					memcpy(String_post,(void*)&curHTTP.data[8+j], 2);

					dummy.v[1] =  String_post[0];
					dummy.v[0] =  String_post[1];
					PassKey[j1] = hexatob(dummy);
					j1++;
				}
				PassKey[j1]= '\0';
				WFSetSecurity(WF_SECURITY_WPA2_WITH_KEY, PassKey, 32, 0);
				ParamSet = TRUE;
			}
		}
	}
	
	return HTTP_IO_DONE;
}

#endif 

/***************************************************************************
  SECTION	Dynamic Variable Callback Functions
  
  In this section are managed the "dynamic variables" of the webserver.
  Dynamic variables are contained in the status.xml file requested by the 
  webpage. 
  For each dynamic variable a callback function named HTTPPrint_varname 
  must be created.
****************************************************************************/

/****************************************************************************
  SECTION 	Authorization Handlers
****************************************************************************/
 

/*****************************************************************************
  FUNCTION	BYTE HTTPNeedsAuth(BYTE* cFile)

  This function is used by the stack to decide if a page is access protected.
  If the function returns 0x00, the page is protected, if returns 0x80, no 
  authentication is required
*****************************************************************************/
#if defined(HTTP_USE_AUTHENTICATION)
BYTE HTTPNeedsAuth(BYTE* cFile)
{
	//	If you want to restrict the access to some page, include it in the folder "protect"
	//	here you can change the folder, or add others
	if(memcmp(cFile, (void*)"protect", 7) == 0)
		return 0x00;		// Authentication will be needed later

	return 0x80;			// No authentication required
}
#endif

/*****************************************************************************
  FUNCTION	BYTE HTTPCheckAuth(BYTE* cUser, BYTE* cPass)
	
  This function checks if username and password inserted are acceptable

  ***************************************************************************/
#if defined(HTTP_USE_AUTHENTICATION)
BYTE HTTPCheckAuth(BYTE* cUser, BYTE* cPass)
{
	if(strcmp((char *)cUser,(char *)"admin") == 0
		&& strcmp((char *)cPass, (char *)"flyport") == 0)
		return 0x80;		// We accept this combination

	return 0x00;			// Provided user/pass is invalid
}
#endif

#endif
