
import urllib, urllib2, StringIO, gzip, json

from config import *


def wt_call(method_name, **params):
    if not method_name.startswith('/'):
        method_name = '/' + method_name

    url = WT_SERVER_ADDRESS + method_name + '?' + urllib.urlencode(params)
    req = urllib2.Request(url)
    req.add_header('Accept-encoding', 'gzip')
    opener, f = None, None
    try:
        opener = urllib2.build_opener()
        f = opener.open(req)
        is_gzip = f.headers.get('Content-Encoding') == 'gzip'
        if is_gzip:
            with gzip.GzipFile(fileobj=StringIO.StringIO(f.read())) as unzipper:
                data = unzipper.read()
        else:
            data = f.read()

        try:
            return json.loads(data)
        except:
            return data
    finally:
        if f:
            f.close()
        if opener:
            opener.close()







