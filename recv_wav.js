// TCP Server
var net = require('net');
// Binary Data Parser
var binary = require('binary');
// Wav File Writer
var wav = require('wav');
// Time Formatter
var moment = require('moment');
// HTTP Server and Client
var http = require('http');
var url = require('url');
// fs
var fs = require('fs');

const tcp_timeout = 10000;
const sample_dir = "./samples/";
const parse_hostname = '';
const parse_port = '';
const tcp_port = 6969;
const http_port = 4774;
const server_name = 'Klement';


var tcp_server = net.createServer(function(sock) {

  sock.pipe(binary()
    .word32bu('id')
	.word8bu('type')
	.tap(function(vars) {
	  if (vars.type == 1) {
	    var filenames = {};
		
	    // Get list of files from the server
		http.get({
	        hostname: parse_hostname,
		    port: parse_port,
		    path: '/getSamples?id=' + vars.id,
		  }, function(res) {
		    var body = '';
            if (res.statusCode != 200) {
              sock.emit('error', new Error("HTTP Response: " + res.statusCode));
            }
			res.on('data', function(stuff) {
			  body += stuff;
			})
			.on('end', function() {
			  //var filenames = JSON.parse(body);
			  sock.emit('file', JSON.parse(body));
			});
		})
		.on('error', function(e) {
	      sock.emit('error', e);
	    });
		
		this.word32bu('sample_rate')
		    .word8bu('bit_depth')
		    .word8bu('channels')
			.tap(function(vars) {
			  sock.unpipe();
			  var sample = sample_dir + vars.id + "_" + moment().format('MMDDYY') + ".wav";
			  sock.pipe(wav.FileWriter(sample, {
	            format: 1,
		        channels: vars.channels,
		        sampleRate: vars.sample_rate,
		        bitDepth: vars.bit_depth
	          }));
			});
	  } 
	  else if (vars.type == 2) {
	    // Status Message
		console.log("status message recv");
	  } 
	  else {
	    //bad message type
		sock.emit('error', new Error("InvalidMessageFormat"));
	  }
	})
  );
  sock.setTimeout(tcp_timeout, function() {
    sock.end('Connection Closed', 'utf8');
    console.log("timeout" + sock.remoteAddress);	
  });
  sock.on('error', function(e) {
    console.log("error: " + e.message);
	sock.destroy();
  });
  sock.on('file', function(sample) {
    console.log(sample);
	
	//console.log("filenames: " + filenames);
  });
});

tcp_server.listen(tcp_port, function() {
  console.log("tcp server is live on port " + tcp_port);
});

http.createServer(function(req, res) {
  // Parse req header
  var header = url.parse(req.url, true);
  if (header.pathname === "/") {
    res.writeHeader(200, {'Content-Type': 'text/plain'});
    res.end(server_name + " is live");
  }
  else if (header.pathname === "/delete") {
    // if file exists try to delete it and return 200
    if('filename' in header.query) {
      fs.unlink(sample_dir + header.query.filename, function(e) {
        if(e) {
          res.writeHeader(418);
          res.end('error ' + e.message);
        }
        res.writeHeader(200);
        res.end('success');
      });
    } 
    else {
      // otherwise return 404
      res.writeHeader(404);
      res.end('no filename specified');
    }
  }
  else if (header.pathname === "/upload") {
    if(('filename' in header.query)) {
      fs.createWriteStream(sample_dir + header.query.filename, function(e) {
        if(e) {
          res.writeHeader(418);
          res.end('error ' + e.message);
        }
        req.pipe(this)
        .on('error', function(e) {
          res.writeHeader(418);
          res.end('error ' + e.message);
        })
        .on('finish', function() {
          res.writeHeader(200);
          res.end('success!');
        });
      });
    }
    else {
      res.writeHeader(418);
      res.end('no filename specified');
    }  
  }
  else if (header.pathname === "/return") {
    // if file exists pipe it to res
    if('filename' in header.query) {
      fs.createReadStream(sample_dir + header.query.filename)
      .on('open', function() {
        res.writeHeader(200, {'Content-Type': 'audio/vnd.wav'});
        this.pipe(res);
      })
      .on('error', function(e) {
        console.log("error " + e.message);
        res.writeHeader(404);
        res.end(header.query.filename + " not found");
      });
    } else {
      res.writeHeader(404);
      res.end('no filename specified');
    }
  }
  else {
    res.writeHeader(200);
    res.end('404 Oh Nose!');
  }
})
.listen(http_port, function() {
  console.log("http server is up on port " + http_port);
});

