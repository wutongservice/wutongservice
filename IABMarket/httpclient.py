
import httplib2
import urllib
from util import *

class HttpResponse(object):
    def __init__(self, resp, content):
        super(HttpResponse, self).__init__()
        self._resp, self._content = resp, content

    @property
    def status(self):
        return int(self._resp['status'])

    @property
    def content(self):
        return self._content

    @property
    def headers(self):
        return self._resp

    def is_ok(self):
        return self.status == 200

    def as_json(self):
        return parse_json(self.content)

class HttpClient(object):
    def __init__(self, host=u''):
        super(HttpClient, self).__init__()
        self.http = httplib2.Http()
        self.host = host.strip()

    def get(self, url, **params):
        uri = urllib.basejoin(self.host, url)
        if params:
            uri = uri + u'?' + unicode(urllib.urlencode(params))
        resp, content = self.http.request(uri)
        return HttpResponse(resp, content)


