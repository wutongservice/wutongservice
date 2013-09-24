__author__ = 'rongxin.gao@borqs.com'

from util import *
import httpagentparser
import account_services

class Context(object):
    def __init__(self, **kvs):
        super(Context, self).__init__()
        self.device_id = kvs.get(u'device_id', u'')
        self.borqs_id = kvs.get(u'borqs_id', u'')
        self.google_ids = split2(kvs.get(u'google_ids', u''), u',')
        self.locale = kvs.get(u'locale', u'')
        self.ip = kvs.get(u'ip', u'')
        self.user_agent = kvs.get(u'user_agent', u'')
        self._parsed_user_agent = httpagentparser.detect(self.user_agent)

    def has_google_ids(self):
        return True if self.google_ids else False

    def has_device_id(self):
        return True if self.device_id else False

    def has_borqs_id(self):
        return True if self.borqs_id else False

    def google_id_count(self):
        return len(self.google_ids) if self.google_ids else 0

    def google_id1(self):
        return self.google_ids[0] if self.google_id_count() > 0 else u''

    def google_id2(self):
        return self.google_ids[1] if self.google_id_count() > 1 else u''

    def google_id3(self):
        return self.google_ids[2] if self.google_id_count() > 2 else u''

    def is_browser(self):
        return u'browser' in self._parsed_user_agent if self._parsed_user_agent else False

    def get_browser(self):
        try:
            return unicode(self._parsed_user_agent[u'browser'][u'name'])
        except:
            return u''

    def get_os(self):
        try:
            return unicode(self._parsed_user_agent[u'os'][u'name'])
        except:
            return u''


def context(req):
    args, headers = req.args, req.headers
    ticket = args.get(u'ticket')
    if not ticket:
        ticket = flask.session.get(u'ticket', u'')
    if not ticket:
        ticket = req.form.get(u'ticket', u'')
    borqs_id = account_services.who(ticket) if ticket else u''
    return Context(google_ids=args.get(u'google_ids', u''),
        locale=args.get(u'locale', u''),
        user_agent=headers.get(u'User-Agent', u''),
        borqs_id=borqs_id,
        device_id=args.get(u'device_id', u'')
    )

