var http = require('http');
var fs = require('fs');
var id = parseInt('deadbabe', 16);

var req = http.request({ hostname: 'klement.cs.washington.edu',
               port: 4774,
               path: '/upload?filename=dumb.wav&id='+id,
               method: 'POST' }, function(res) {
  console.log('STATUS: ' + res.statusCode);
  console.log('HEADERS: ' + JSON.stringify(res.headers));
  res.setEncoding('utf8');
  res.on('data', function (chunk) {
    console.log('BODY: ' + chunk);
  });
});

fs.createReadStream('test.wav')
.pipe(req);
