/*
 * Reid Mayfield, CSE 477 Spring 2014, rmayf@cs.washington.edu
 *
 */

// TCP Server
var net = require('net');
// Binary Data Parser
var binary = require('binary');
// Wav File Writer
var wav = require('wav');
// Time Formatter
var moment = require('moment');
// HTTP Server
var http = require('http');
var url = require('url');
// fs
var fs = require('fs');
// HTTPS Client
var https = require('https');
// Used for multiple event trigger
var async = require('async');

const tcp_timeout = 10000;
const sample_dir = "./samples/";
const parse_hostname = 'api.parse.com';
const parse_path = '/1/users/GyqucAeYa3';
const tcp_port = 6969;
const http_port = 4774;
const server_name = 'Klement';

/* TCP Message Formats (Network Ordered)

Sample Upload [Type 1]

+----------------+----------+------------------+---------------+------------------------+----------
| device id (32) | type (8) | sample rate (32) | bit depth (8) | number of channels (8) | data ... 
+----------------+----------+------------------+---------------+------------------------+----------


Status Message [Type 2]

+----------------+----------+------------------------+---------
| device id (32) | type (8) | battery percentage (8) | ??? ....
+----------------+----------+------------------------+---------


*/

var tcp_server = net.createServer(function(sock) {
  sock.pipe(binary()
    .word32bu('id')
	.word8bu('type')
	.tap(function(vars) {
	  if (vars.type == 1) {
            var extractor = this;
            async.parallel({
              catalog: function(callback) {		
	      // Get list of files from the server
		https.get({
	        hostname: parse_hostname,
		    path: parse_path,
                    headers: { 'X-Parse-Application-Id': '0JBJLFotV9xF1MvdmihvrH8Ozx8EG9CPNlHX2WFM',
                               'X-Parse-REST-API-Key': 'OPzuRnFuJDcbOmVwZbfUdWEtgnphyKwqoh5RT4NR' }
		  }, function(res) {
		    var body = '';
                    if (res.statusCode != 200) {
                       callback (new Error("HTTP Response: " + res.statusCode), null);
                    }
		    res.on('data', function(stuff) {
		      body += stuff;
	            })
		    res.on('end', function() {
		      callback (null, JSON.parse(body));
		    });
		})
		.on('error', function(e) {
	          callback (e, null);
	        });
              },
              sample: function(callback) {
		extractor.word32bu('sample_rate')
		    .word8bu('bit_depth')
		    .word8bu('channels')
		    .tap(function(vars) {
		      sock.unpipe();
		      var sample = sample_dir + 'd'+vars.id + "_" + moment().format('MMDDYY_hhmmss') + ".wav";
		      sock.pipe(wav.FileWriter(sample, {
	                format: 1,
		        channels: vars.channels,
		        sampleRate: vars.sample_rate,
		        bitDepth: vars.bit_depth
	              }));
                      sock.on('end', function() {
                        callback(null, sample);
                      });
		    });
             }
           },
           function(err, res) {
             if(err) {
               sock.emit('error', err);
             }
             // res {catalog: ["file1", "file2", ...], sample: "filename"}
             var comparer = require('child_process').exec('./compare ' + res.sample, 
                                                                        function(err, out, stderr) {
               if(err) {
                 console.log('error ' + err);
               }
               console.log('answer: ' + out);
               // Send match alert to parse
               // https.get(
             });
           });
	  } 
	  else if (vars.type == 2) {
	    // Status Message
            console.log("status message recv");
            extractor.word8bu('battery')
              .tap(function(vars) {
                // Send battery level to Parse
                // https.get(
              });
            
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
    req.on('dir_exists', function() {
      if(('filename' in header.query)) {
        var file = fs.createWriteStream(sample_dir + header.query.filename, {flags: 'w'});
        file.on('error', function(e) {
          req.emit('error', e);
        });
        req.pipe(file);
        req.on('end', function() {
          res.writeHeader(200);
          res.end('success!');
        });
      }
      else {
        res.writeHeader(418);
        res.end('no filename specified');
      }  
    });
    req.on('error', function(e) {
      res.writeHeader(418);
      res.end('error ' + e.message);
    });
    fs.exists(sample_dir, function(yes) {
      if (!yes) {
        fs.mkdir(sample_dir, function(e) {
          if(e) {
            req.emit('error', e);
          } 
          req.emit('dir_exists');
        });
      } else {
        req.emit('dir_exists');
      }
    });
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

