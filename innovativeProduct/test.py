import web
import urllib, httplib
import urllib2
import StringIO
import gzip

def findUrlGzip(url):
    request = urllib2.Request(url)
    request.add_header('Accept-encoding', 'gzip')
    opener = urllib2.build_opener()
    f = opener.open(request)
    isGzip = f.headers.get('Content-Encoding')
    #print isGzip
    if isGzip:
        compresseddata = f.read()
        compressedstream = StringIO.StringIO(compresseddata)
        gzipper = gzip.GzipFile(fileobj=compressedstream)
        data = gzipper.read()
    else:
        data = f.read()
    return data


da = findUrlGzip("http://api.borqs.com/user/show?users=10405")

print(da)

conn = httplib.HTTPConnection('api.borqs.com')
params = urllib.urlencode({'users': '10405'})
conn.request('POST', '/user/show', headers={"Content-Type": "application/x-www-form-urlencoded"}, body=params)
result = conn.getresponse()
resultContent = result.read()

urls = ('/(.*)/', 'redirect', "/.*", "hello")

class hello:
    def GET(self):
        return 'Hello, world!'


class redirect:
    def GET(self, path):
        web.seeother('/' + path)

if __name__ == "__main__":
    app = web.application(urls, globals())
    app.run()