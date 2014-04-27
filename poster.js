var http = require('http');

var req = http.request({ hostname: 'klement.cs.washington.edu',
               port: 4774,
               path: '/upload?filename=dumb.txt',
               method: 'POST' }, function(res) {
  console.log('STATUS: ' + res.statusCode);
  console.log('HEADERS: ' + JSON.stringify(res.headers));
  res.setEncoding('utf8');
  res.on('data', function (chunk) {
    console.log('BODY: ' + chunk);
  });
});

req.write('data\n');
req.end();
