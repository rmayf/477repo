var net = require('net');
var binary = require('binary');
var wav = require('wav');
//var Speaker = require('speaker');

var server = net.createServer(function(sock) {
  //console.log(sock.remoteAddress + ":" + sock.remotePort + " connected!");
  var extractor = binary()
    .word32bu('id')
	.word32bu('sample_rate')
    .word8bu('bit_depth')
    .word8bu('channels')
    .tap(function(vars) {
	  sock.unpipe(extractor);
		
      // Debug Stuff
	  console.log("Device ID: 0x" + vars.id.toString(16));
	  console.log("Sample Rate: " + vars.sample_rate);
	  console.log("Bit Depth: " + vars.bit_depth);
	  console.log("Number of Channels: " + vars.channels + "\n");
            
	  //Send the data to the speaker
	  // sock.pipe(new Speaker({
		  // channels: vars.channels,
		  // bitDepth: vars.bit_depth,
		  // sampleRate: vars.sample_rate
        // })
	  // );
	  
	  // Write to a file
	  sock.pipe(wav.FileWriter("./test.wav", {
	    format: 1,
		channels: vars.channels,
		sampleRate: vars.sample_rate,
		bitDepth: vars.bit_depth
	  }));
	  
	});
	
  sock.pipe(extractor);
	
  sock.on('error', function(e) {
    console.log("error: " + e.code);
	sock.end();
  });
});

server.listen(6969, function() {
  var info = server.address();
  console.log("server is live on port " + info.port);
});