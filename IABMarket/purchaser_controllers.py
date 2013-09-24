__author__ = 'rongxin.gao@borqs.com'

import flask
from util import *
import purchaser_services
import gv
from context import *
import voluptuous as vv


@gv.app.route(u'/api/v1/purchaser/products/list')
def api_list_products():
    SCHEMA = vv.Schema({
        vv.Required(u'app'): vv.All(basestring, vv.Length(max=200), vv.Match(purchaser_services.APP_PATTERN)),
        vv.Required(u'version_code'): vv.All(int, vv.Range(min=0)),
        vv.Optional(u'category'): vv.All(basestring),
        vv.Optional(u'order'): vv.All(basestring),
        vv.Optional(u'page'): vv.All(int, vv.Range(min=0)),
        vv.Optional(u'count'): vv.All(int, vv.Range(min=1)),
        vv.Optional(u'google_ids'): vv.All(basestring),
        vv.Optional(u'device_id'): vv.All(basestring),
        vv.Optional(u'mod'): vv.All(basestring),
        }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'app' in args:
            ps[u'app'] = args.get(u'app')
        if u'version_code' in args:
            ps[u'version_code'] = int(args.get(u'version_code'))
        if u'category' in args:
            ps[u'category'] = args.get(u'category')
        if u'order' in args:
            ps[u'order'] = args.get(u'order')
        if u'page' in args:
            ps[u'page'] = int(args.get(u'page'))
        if u'count' in args:
            ps[u'count'] = int(args.get(u'count'))
        if u'mod' in args:
            ps[u'mod'] = args.get(u'mod')

        validate_params(SCHEMA, ps)
        ctx = context(req)
        return purchaser_services.list_products(ctx,
            ps.get(u'app'),
            ps.get(u'version_code'),
            ps.get(u'category', u'*'),
            ps.get(u'order', u'updated'),
            ps.get(u'mod', u'*'),
            (ps.get(u'page', 0), ps.get(u'count', 10)))

    return api_do(flask.request, process)

@gv.app.route(u'/api/v1/purchaser/products/get')
def api_show_product():
    SCHEMA = vv.Schema({
        vv.Required(u'id'): vv.All(basestring, vv.Length(max=200), vv.Match(purchaser_services.APP_PATTERN)),
        vv.Required(u'version'): vv.All(basestring, vv.Length(max=20), vv.Match(purchaser_services.VERSION_PATTERN))
        }, extra=True)

    def process(req):
        args = req.args

        ps = dict()
        if u'id' in args:
            ps[u'id'] = args.get(u'id')
        if u'version' in args:
            ps[u'version'] = args.get(u'version')

        validate_params(SCHEMA, ps)
        ctx = context(req)
        return purchaser_services.show_products(ctx, ps.get(u'id'), ps.get(u'version'))

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/purchaser/purchase')
def api_purchase():
    SCHEMA = vv.Schema({
        vv.Required(u'id'): vv.All(basestring, vv.Length(max=200), vv.Match(purchaser_services.APP_PATTERN)),
        vv.Required(u'version'): vv.All(basestring, vv.Length(max=20), vv.Match(purchaser_services.VERSION_PATTERN)),
        vv.Optional(u'google_iab_order_id'): vv.All(basestring, vv.Length(max=64)),
    }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'id' in args:
            ps[u'id'] = args.get(u'id')
        if u'version' in args:
            ps[u'version'] = args.get(u'version')
        if u'google_iab_order_id' in args:
            ps[u'google_iab_order_id'] = args.get(u'google_iab_order_id')

        validate_params(SCHEMA, ps)
        ctx = context(req)
        return purchaser_services.purchase(ctx,
            ps.get(u'id'),
            ps.get(u'version'),
            ps.get(u'google_iab_order_id', u''))

    return api_do(flask.request, process)






