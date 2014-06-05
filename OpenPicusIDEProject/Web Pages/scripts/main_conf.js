
	function getWpaPskKeyFromPassphrase(pass, salt) {

	  /* pad string to 64 bytes and convert to 16 32-bit words */
	  function stringtowords(s, padi) {
	    /* return a 80-word array for later use in the SHA1 code */
	    var z = new Array(80);
	    var j = -1, k = 0;
	    var n = s.length;
	    for (var i = 0; i < 64; i++) {
	      var c = 0;
	      if (i < n) {
	        c = s.charCodeAt(i);
	      } else if (padi) {
	        /* add 4-byte PBKDF2 block index and
		   standard padding for the final SHA1 input block */
		if (i == n) c = (padi >>> 24) & 0xff;
		else if (i == n + 1) c = (padi >>> 16) & 0xff;
		else if (i == n + 2) c = (padi >>> 8) & 0xff;
		else if (i == n + 3) c = padi & 0xff;
		else if (i == n + 4) c = 0x80;
	      }
	      if (k == 0) { j++; z[j] = 0; k = 32; }
	      k -= 8;
	      z[j] = z[j] | (c << k);
	    }
	    if (padi) z[15] = 8 * (64 + n + 4);
	    return z;
	  }

	  /* compute the intermediate SHA1 state after processing just
	     the 64-byte padded HMAC key */
	  function initsha(w, padbyte) {
	    var pw = (padbyte << 24) | (padbyte << 16) | (padbyte << 8) | padbyte;
	    for (var t = 0; t < 16; t++) w[t] ^= pw;
	    var s = [ 0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 ];
	    var a = s[0], b = s[1], c = s[2], d = s[3], e = s[4];
	    var t;
	    for (var k = 16; k < 80; k++) {
	      t = w[k-3] ^ w[k-8] ^ w[k-14] ^ w[k-16];
	      w[k] = (t<<1) | (t>>>31);
	    }
	    for (var k = 0; k < 20; k++) {
	      t = ((a<<5) | (a>>>27)) + e + w[k] + 0x5A827999 + ((b&c)|((~b)&d));
	      e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	    }
	    for (var k = 20; k < 40; k++) {
	      t = ((a<<5) | (a>>>27)) + e + w[k] + 0x6ED9EBA1 + (b^c^d);
	      e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	    }
	    for (var k = 40; k < 60; k++) {
	      t = ((a<<5) | (a>>>27)) + e + w[k] + 0x8F1BBCDC + ((b&c)|(b&d)|(c&d));
	      e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	    }
	    for (var k = 60; k < 80; k++) {
	      t = ((a<<5) | (a>>>27)) + e + w[k] + 0xCA62C1D6 + (b^c^d);
	      e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	    }
	    s[0] = (s[0] + a) & 0xffffffff;
	    s[1] = (s[1] + b) & 0xffffffff;
	    s[2] = (s[2] + c) & 0xffffffff;
	    s[3] = (s[3] + d) & 0xffffffff;
	    s[4] = (s[4] + e) & 0xffffffff;
	    return s;
	  }

	  /* compute the intermediate SHA1 state of the inner and outer parts
	     of the HMAC algorithm after processing the padded HMAC key */
	  var hmac_istate = initsha(stringtowords(pass, 0), 0x36);
	  var hmac_ostate = initsha(stringtowords(pass, 0), 0x5c);

	  /* output is created in blocks of 20 bytes at a time and collected
	     in a string as hexadecimal digits */
	  var hash = '';
	  var i = 0;
	  while (hash.length < 64) {
	    /* prepare 20-byte (5-word) output vector */
	    var u = [ 0, 0, 0, 0, 0 ];
	    /* prepare input vector for the first SHA1 update (salt + block number) */
	    i++;
	    var w = stringtowords(salt, i);
	    /* iterate 4096 times an inner and an outer SHA1 operation */
	    for (var j = 0; j < 2 * 4096; j++) {
	      /* alternate inner and outer SHA1 operations */
	      var s = (j & 1) ? hmac_ostate : hmac_istate;
	      /* inline the SHA1 update operation */
	      var a = s[0], b = s[1], c = s[2], d = s[3], e = s[4];
	      var t;
	      for (var k = 16; k < 80; k++) {
	        t = w[k-3] ^ w[k-8] ^ w[k-14] ^ w[k-16];
	        w[k] = (t<<1) | (t>>>31);
	      }
	      for (var k = 0; k < 20; k++) {
	        t = ((a<<5) | (a>>>27)) + e + w[k] + 0x5A827999 + ((b&c)|((~b)&d));
	        e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	      }
	      for (var k = 20; k < 40; k++) {
	        t = ((a<<5) | (a>>>27)) + e + w[k] + 0x6ED9EBA1 + (b^c^d);
	        e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	      }
	      for (var k = 40; k < 60; k++) {
	        t = ((a<<5) | (a>>>27)) + e + w[k] + 0x8F1BBCDC + ((b&c)|(b&d)|(c&d));
	        e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	      }
	      for (var k = 60; k < 80; k++) {
	        t = ((a<<5) | (a>>>27)) + e + w[k] + 0xCA62C1D6 + (b^c^d);
	        e = d; d = c; c = (b<<30) | (b>>>2); b = a; a = t & 0xffffffff;
	      }
	      /* stuff the SHA1 output back into the input vector */
	      w[0] = (s[0] + a) & 0xffffffff;
	      w[1] = (s[1] + b) & 0xffffffff;
	      w[2] = (s[2] + c) & 0xffffffff;
	      w[3] = (s[3] + d) & 0xffffffff;
	      w[4] = (s[4] + e) & 0xffffffff;
	      if (j & 1) {
	        /* XOR the result of each complete HMAC-SHA1 operation into u */
		u[0] ^= w[0]; u[1] ^= w[1]; u[2] ^= w[2]; u[3] ^= w[3]; u[4] ^= w[4];
	      } else if (j == 0) {
	        /* pad the new 20-byte input vector for subsequent SHA1 operations */
		w[5] = 0x80000000;
		for (var k = 6; k < 15; k++) w[k] = 0;
		w[15] = 8 * (64 + 20);
	      }
	    }
	    /* convert output vector u to hex and append to output string */
	    for (var j = 0; j < 5; j++)
	      for (var k = 0; k < 8; k++) {
	        var t = (u[j] >>> (28 - 4 * k)) & 0x0f;
		hash += (t < 10) ? t : String.fromCharCode(87 + t);
	      }
	  }

	  /* return the first 32 key bytes as a hexadecimal string */
	  return hash.substring(0, 64);
	}

	function fshowkey(s) {
	  if (s == "") { for (var i = 0; i < 64; i++) s += "\u00a0"; }



		switch(document.getElementById("SECTYPE").value)
		{
		
			case "WPA":
				var elem = document.getElementById("WPAKEY");
				var elempass = document.getElementById("WPAPASS");
	  			elem.value = s;
				elempass.disabled = true;
				elem.disabled = false;
				break;
			
			case "WPA2":
				var elem = document.getElementById("WPA2KEY");
				var elempass = document.getElementById("WPA2PASS");
	  			elem.value = s;
				elempass.disabled = true;
				elem.disabled = false;
				break;
		}

	}

	function fcalc() {
		switch(document.getElementById("SECTYPE").value)
		{
		
			case "WPA":
				var ssid = document.getElementById("SSID").value;
  				var pass = document.getElementById("WPAPASS").value;
  				var myssid = "SSID";
  				var mypass = "WPAPASS";
				break;
			
			case "WPA2":
				var ssid = document.getElementById("SSID").value;
  				var pass = document.getElementById("WPA2PASS").value;
  				var myssid = "SSID";
  				var mypass = "WPA2PASS";
				break;
		}
		
	  fshowkey("");
	  if (ssid.length < 1) {
	    alert("ERROR: You must enter the network SSID string.");
	    document.getElementById(myssid).focus();
	    return;
	  }
	  if (ssid.length > 32) {
	    alert("ERROR: The SSID string must not be longer than 32 characters.");
	    document.getElementById(myssid).focus();
	    return;
	  }
	  if (pass.length < 1) {
	    alert("ERROR: Passphrase must be at least 8 characters.");
	    document.getElementById(mypass).focus();
	    return;
	  }
	  if (pass.length > 63) {
	    alert("ERROR: Passphrase must not be longer than 63 characters.");
	    document.getElementById(mypass).focus();
	    return;
	  }
	  for (var i = 0; i < pass.length; i++)
	    if (pass.charCodeAt(i) < 1 || pass.charCodeAt(i) > 126) {
	      alert("ERROR: Passphrase contains strange characters.");
	      document.getElementById(mypass).focus();
	      return;
	    }
	  for (var i = 0; i < ssid.length; i++)
	    if (ssid.charCodeAt(i) < 1 || ssid.charCodeAt(i) > 126) {
	      alert("ERROR: SSID string contains strange characters.");
	      document.getElementById(myssid).focus();
	      return;
	    }
	  var hash = getWpaPskKeyFromPassphrase(pass, ssid);
	  fshowkey(hash);
	}

	function fclear() {

		switch(document.getElementById("SECTYPE").value)
		{
		
			case "WPA":
				document.getElementById("WPAPASS").disabled = false;
				document.getElementById("WPAPASS").value = "";
				document.getElementById("WPAKEY").value = "";
				document.getElementById("WPAKEY").disabled = true;
				break;
			
			case "WPA2":
				document.getElementById("WPA2PASS").disabled = false;
				document.getElementById("WPA2PASS").value = "";
				document.getElementById("WPA2KEY").value = "";
				document.getElementById("WPA2KEY").disabled = true;
				break;
		}

	}


function SetValue(action)
{
	dhcp_on = document.getElementById("DHCPCL_ON");
	dhcp_off = document.getElementById("DHCPCL_OFF");
	
	ipaddr = document.getElementById("IPADDR");
	subnet = document.getElementById("SUBNET");
	gateway = document.getElementById("GATEWAY");
	dns1 = document.getElementById("DNS1");
	dns2 = document.getElementById("DNS2");
	
	ssid = document.getElementById("SSID");
	sectype = document.getElementById("SECTYPE");
	
	wep40 = document.getElementById("WEP40");
	wep104 = document.getElementById("WEP104");
	wpa = document.getElementById("WPA");
	wpa2 = document.getElementById("WPA2");
	
	wpapass = document.getElementById("WPAPASS");
	wpakey = document.getElementById("WPAKEY");
	wpa2pass = document.getElementById("WPA2PASS");
	wpa2key = document.getElementById("WPA2KEY");
	
	calckey = document.getElementById("CALCKEY");
	clearkey = document.getElementById("CLEARKEY");

	/*wifi_menu = document.getElementById("wifi_menu");*/
	wifi_content = document.getElementById("wifi_content");
	
	switch(action)
	{
		case "infrastructure":
			dhcp_on.disabled = false;

			/*wifi_menu.style.height = "120px";*/
			wifi_content.style.height = "120px";
			
			wep40.style.display = "none";
			wep104.style.display = "none";
			wpa.style.display = "none";
			wpa2.style.display = "none";
			
			while (sectype.options.length)
				sectype.options[0] = null;
			
			sectype.options[0] = new Option("Open", "OPEN");
			sectype.options[1] = new Option("WEP 40 bit", "WEP40");
			sectype.options[2] = new Option("WEP 104 bit", "WEP104");
			sectype.options[3] = new Option("WPA", "WPA");
			sectype.options[4] = new Option("WPA2", "WPA2");
			break;
		
		case "adhoc":
			dhcp_on.disabled = true;
			dhcp_off.checked = true;
			
			ipaddr.disabled = false;
			subnet.disabled = false;
			gateway.disabled = false;
			dns1.disabled = false;
			dns2.disabled = false;
			
			wep40.style.display = "none";
			wep104.style.display = "none";
			wpa.style.display = "none";
			wpa2.style.display = "none";
			
			while (sectype.options.length)
				sectype.options[0] = null;
			
			sectype.options[0] = new Option("Open", "OPEN");
			sectype.options[1] = new Option("WEP 40 bit", "WEP40");
			sectype.options[2] = new Option("WEP 104 bit", "WEP104");
			break;
			
		case "softap":
			dhcp_on.disabled = true;
			dhcp_off.checked = true;
			
			ipaddr.disabled = false;
			subnet.disabled = false;
			gateway.disabled = false;
			dns1.disabled = false;
			dns2.disabled = false;
			
			wep40.style.display = "none";
			wep104.style.display = "none";
			wpa.style.display = "none";
			wpa2.style.display = "none";
			
			while (sectype.options.length)
				sectype.options[0] = null;
			
			sectype.options[0] = new Option("Open", "OPEN");
			sectype.options[1] = new Option("WEP 40 bit", "WEP40");
			sectype.options[2] = new Option("WEP 104 bit", "WEP104");
			break;
		
		case "dhcpon":
			ipaddr.disabled = true;
			subnet.disabled = true;
			gateway.disabled = true;
			dns1.disabled = true;
			dns2.disabled = true;
			break;
		
		case "dhcpoff":
			ipaddr.disabled = false;
			subnet.disabled = false;
			gateway.disabled = false;
			dns1.disabled = false;
			dns2.disabled = false;
			break;
		
		case "sectype":
			wep40.style.display = "none";
			wep104.style.display = "none";
			wpa.style.display = "none";
			wpa2.style.display = "none";
			
			switch(sectype.value)
			{
				case "OPEN":
					/*wifi_menu.style.height = "120px";*/
					wifi_content.style.height = "120px";
					calckey.style.display = "none";
					clearkey.style.display = "none";
					break;
				
				case "WEP40":
					/*wifi_menu.style.height = "260px";*/
					wifi_content.style.height = "260px";
					wep40.style.display = "block";
					calckey.style.display = "none";
					clearkey.style.display = "none";
					break;
				
				case "WEP104":
					/*wifi_menu.style.height = "140px";*/
					wifi_content.style.height = "140px";
					wep104.style.display = "block";
					calckey.style.display = "none";
					clearkey.style.display = "none";
					break;
				
				case "WPA":
					/*wifi_menu.style.height = "200px";*/
					wifi_content.style.height = "170px";
					wpa.style.display = "block";
					calckey.style.display = "block";
					clearkey.style.display = "block";
					break;
				
				case "WPA2":
					/*wifi_menu.style.height = "200px";*/
					wifi_content.style.height = "170px";
					wpa2.style.display = "block";
					calckey.style.display = "block";
					clearkey.style.display = "block";
					break;
			}
			break;
		
		case "wpapass":
			wpapass.disabled = false;
			wpakey.disabled = true;
			break;
		
		case "wpakey":
			wpapass.disabled = true;
			wpakey.disabled = false;
			break;
		
		case "wpa2pass":
			wpa2pass.disabled = false;
			wpa2key.disabled = true;
			break;
		
		case "wpa2key":
			wpa2pass.disabled = true;
			wpa2key.disabled = false;
			break;
	}
}

// ------------------------------------------------

function CheckIP(ip)
{
	pattern = ip.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
	
	if(pattern == null)
		return false;
	var i = 1;
	for (i = 1; i < 5; i++) 
	{
		if(pattern[i] > 255)
			return false;
	}
	
	return true;
}

function CheckHEX(hex)
{
	hex_char = "0123456789ABCDEF";
	hex = hex.toUpperCase();
	var i;

	for(i = 0; i < hex.length; i++)
	{
		if(hex_char.indexOf(hex.charAt(i)) < 0)
			return false;
	}
	
	return true;
}


function CheckSend()
{
	frm = document.getElementById("configuration");
	
	if(document.getElementById("DHCPCL_OFF").checked)
	{
		if(!CheckIP(frm.IPADDR.value))
		{
			alert("Error: not valid IP address!");
			frm.IPADDR.style.backgroundColor = "#f99";
			return false;
		}
		else
			frm.IPADDR.style.backgroundColor = "#fff";
		
		if(!CheckIP(frm.SUBNET.value))
		{
			alert("Error: not valid subnet mask!");
			frm.SUBNET.style.backgroundColor = "#f99";
			return false;
		}
		else
			frm.SUBNET.style.backgroundColor = "#fff";
		
		if(!CheckIP(frm.GATEWAY.value))
		{
			alert("Error: not valid gateway!");
			frm.GATEWAY.style.backgroundColor = "#f99";
			return false;
		}
		else
			frm.GATEWAY.style.backgroundColor = "#fff";
		
	}
	
	if(frm.SSID.value.length < 1)
	{
		alert("Error: valid SSID required!");
		frm.SSID.style.backgroundColor = "#f99";
		return false;
	}
	else
		frm.SSID.style.backgroundColor = "#fff";

	if(frm.SECTYPE.value == "WEP40")
	{
		error = false;
		
		if((frm.WEP40KEY1.value.length < 10)||(frm.WEP40KEY2.value.length < 10)||
		   (frm.WEP40KEY3.value.length < 10)||(frm.WEP40KEY1.value.length < 10))
		{
			alert("Error: wep 40 bit passkey incomplete!");
			error = true;
		}
		
		if((!CheckHEX(frm.WEP40KEY1.value))||(!CheckHEX(frm.WEP40KEY2.value))||
		   (!CheckHEX(frm.WEP40KEY3.value))||(!CheckHEX(frm.WEP40KEY4.value)))
		{
			alert("Error: only hex values are allowed for the wep 40 bit passkey!");
			error = true;
		}
		
		if(error)
		{
			frm.WEP40KEY1.style.backgroundColor = "#f99";
			frm.WEP40KEY2.style.backgroundColor = "#f99";
			frm.WEP40KEY3.style.backgroundColor = "#f99";
			frm.WEP40KEY4.style.backgroundColor = "#f99";
			return false;
		}
		else
		{
			frm.WEP40KEY1.style.backgroundColor = "#fff";
			frm.WEP40KEY2.style.backgroundColor = "#fff";
			frm.WEP40KEY3.style.backgroundColor = "#fff";
			frm.WEP40KEY4.style.backgroundColor = "#fff";
			var j = 0;
			var strdummy = "";
			var strdummy2 = "";

			frm.WEP40KEY4.value = frm.WEP40KEY1.value + frm.WEP40KEY2.value + frm.WEP40KEY3.value + frm.WEP40KEY4.value;
		}
	}
	
	if(frm.SECTYPE.value == "WEP104")
	{
		error = false;
		
		if(frm.WEP104KEY.value.length < 26)
		{
			alert("Error: wep 104 bit passkey incomplete!");
			error = true;
		}
		
		if(!CheckHEX(frm.WEP104KEY.value))
		{
			alert("Error: only hex values are allowed for the wep 104 bit passkey!");
			error = true;
		}
		
		if(error)
		{
			frm.WEP104KEY.style.backgroundColor = "#f99";
			return false;
		}
		else
			frm.WEP140KEY.style.backgroundColor = "#fff";
	}
	
	if(frm.SECTYPE.value == "WPA")
	{
		error = false;
		
		/*if(document.getElementById("WPASECPASS").checked)
		{
			if(frm.WPAPASS.value.length < 8)
			{
				alert("Error: WPA passphrase incomplete!");
				frm.WPAPASS.style.backgroundColor = "#f99";
				error = true;
			}
		}
		else
		{*/
			if(frm.WPAKEY.value.length < 64)
			{
				alert("Error: wpa passkey incomplete!");
				frm.WPAKEY.style.backgroundColor = "#f99";
				error = true;
			}
			
			if(!CheckHEX(frm.WPAKEY.value))
			{
				alert("Error: only hex values are allowed for the wpa passkey!");
				frm.WPAKEY.style.backgroundColor = "#f99";
				error = true;
			}
		//}
		
		if(error)
		{
			return false;
		}
		else
		{
			frm.WPAPASS.style.backgroundColor = "#fff";
			frm.WPAKEY.style.backgroundColor = "#fff";
		}
	}
	
	if(frm.SECTYPE.value == "WPA2")
	{
		error = false;
		
		/*
		if(document.getElementById("WPA2SECPASS").checked)
		{
			if(frm.WPA2PASS.value.length < 8)
			{
				alert("Error: WPA2 passphrase incomplete!");
				frm.WPA2PASS.style.backgroundColor = "#f99";
				error = true;
			}
		}
		else
		{*/
			if(frm.WPA2KEY.value.length < 64)
			{
				alert("Error: wpa2 passkey incomplete!");
				frm.WPA2KEY.style.backgroundColor = "#f99";
				error = true;
			}
			
			if(!CheckHEX(frm.WPA2KEY.value))
			{
				alert("Error: only hex values are allowed for the wpa2 passkey!");
				frm.WPA2KEY.style.backgroundColor = "#f99";
				error = true;
			}
		//}
		
				
		if(error)
		{
			return false;
		}
		else
		{
			frm.WPA2PASS.style.backgroundColor = "#fff";
			frm.WPA2KEY.style.backgroundColor = "#fff";
		}
	}
	
	return true;
}
