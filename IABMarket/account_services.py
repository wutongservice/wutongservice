__author__ = 'rongxin.gao@borqs.com'

import gv
from errors import *

def _check_wutong_response(resp):
    if not resp.is_ok():
        raise APIError(E_ACCOUNT_ERROR, u'Proxy response error')

    o = resp.as_json()
    if isinstance(o, dict) and u'error_code' in o:
        raise APIError(E_ACCOUNT_ERROR, u'Proxy response error : %s (code=%s)' % (o.get(u'error_msg', u''), o.get(u'error_code')))
    return o

def signin(name, passwd):
    resp = gv.wutong_client.get(u'/account/login', login_name=name, password=passwd)
    return _check_wutong_response(resp)

def signout(ticket):
    resp = gv.wutong_client.get(u'/account/logout', ticket=ticket)
    return _check_wutong_response(resp)

def who(ticket):
    resp = gv.wutong_client.get(u'/account/who', ticket=ticket)
    o = _check_wutong_response(resp)
    return unicode(o[u'result']) if o[u'result'] > 0 else u''

def get_profile(uid):
    resp = gv.wutong_client.get(u'/user/show', users=uid)
    users = _check_wutong_response(resp)
    if users:
        user = users[0]
        return {u'id':uid, u'name':user[u'display_name'], u'email':u''} #TODO: get user login_email
    else:
        return {u'id':uid, u'name':u'', u'email':u''}