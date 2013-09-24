__author__ = 'rongxing.gao@borqs.com'

import gv
from util import *
import voluptuous as vv
import account_services
from controller_helper import *

@gv.app.route(u'/')
@gv.app.route(u'/index.html')
def index():
    ticket = flask.session.get(u'ticket', u'')
    if ticket:
        return flask.redirect(u'/publisher/index.html')
    else:
        return flask.redirect(u'/account/signin.html')

@gv.app.route(u'/api/v1/account/signin')
def signin():
    SCHEMA = vv.Schema({
        vv.Required(u'name'): vv.All(basestring, vv.Length(max=64)),
        vv.Required(u'password'): vv.All(basestring, vv.Range(min=0)),
    }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'name' in args:
            ps[u'name'] = args.get(u'name')
        if u'password' in args:
            ps[u'password'] = args.get(u'password')
        validate_params(SCHEMA, ps)
        return account_services.signin(ps[u'name'], ps[u'password'])

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/account/signout')
def signout():
    SCHEMA = vv.Schema({
        vv.Required(u'ticket'): vv.All(basestring),
    }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'ticket' in args:
            ps[u'ticket'] = args.get(u'ticket')
        validate_params(SCHEMA, ps)
        return account_services.signout(ps[u'ticket'])

    return api_do(flask.request, process)


#==========================

@gv.app.route(u'/account/signin.html', methods=[u'GET', u'POST'])
def signin_page():
    req = flask.request
    if req.method == u'POST':
        username, pwd = req.form.get(u'username', u''), req.form.get(u'password', u'')
        try:
            resp = account_services.signin(username, md5hex(pwd, lower=False))
            flask.session[u'ticket'] = resp[u'ticket']
            return flask.redirect(u'/publisher/products/list.html')
        except APIError, e:
            return error_page(u'Login error', u'%s (%s)' % (e.message, e.code))
    else:
        return flask.render_template(u'signin.jinja2',
            page_class=u'login_page',
            page_title=u'Signin'
        )

@gv.app.route(u'/account/signout.html', methods=[u'GET'])
def signout_page():
    ticket = flask.request.args.get(u'ticket', u'')
    if not ticket:
        ticket = flask.session.get(u'ticket', u'')
    if ticket:
        account_services.signout(ticket)
        del flask.session[u'ticket']

    return flask.redirect(u'/account/signin.html')