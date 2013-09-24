# coding: utf-8
__author__ = 'rongxin.gao@borqs.com'

import time
import flask
import random
import uuid
import binascii
import json
import voluptuous as vv
import jsonschema
import Image
from errors import *
import config
import os.path
import hashlib
import base64
import sys

MAX_INT32 = int(2 ** 31 - 1)
MIN_INT32 = int(-2 ** 31)

MAX_INT64 = long(2 ** 63 - 1)
MIN_INT64 = long(-2 ** 63)

# json
def parse_json(s, defval={}):
    try:
        o = json.loads(s, encoding="utf8")
        return o
    except:
        return defval


def parse_json_field(d, k, defval={}):
    if k in d:
        d[k] = parse_json(d[k], defval)

def parse_json_fields(d, ks):
    for k in ks:
        parse_json_field(d, k)

def to_json(o):
    return json.dumps(o, encoding="utf8")

# api
def api_ok(o):
    #return flask.jsonify(code=0, data=o)
    j = json.dumps({u'code':0, u'data':o})
    cb = flask.request.args.get(u'callback')
    if cb:
        j = u'%s(%s);' % (cb, j)
        return flask.Response(response=j, status=200, content_type=u'text/javascript')
    else:
        return flask.Response(response=j, status=200, content_type=u'application/json')

def api_error(code, msg):
    return flask.jsonify(code=code, error_message=msg)


def api_do(req, func):
    try:
        o = func(req)
        resp = api_ok(o)
        return resp
    except APIError, e:
        return api_error(e.code, e.message)
    except BaseException, e:
        if config.getboolean(u'misc', u'debug', False):
            raise e
        else:
            return api_error(E_UNKNOWN, u'Unknown error')


def _make_upload_html(file_field=u'file'):
    return u"""<!doctype html>
    <title>Upload new File</title>
    <h1>Upload new File</h1>
    <form action="" method="post" enctype="multipart/form-data">
    <p><input type="file" name="%s">
    <input type="submit" value="Upload!">
    </form>
    """ % file_field

def upload_api_do(req, func):
    if req.method == u'POST':
        return api_do(req, func)
    else:
        return _make_upload_html()


# validate params
def validate_params(schema, params):
    try:
        schema(params)
    except vv.Invalid, e:
        raise APIError(E_ILLEGAL_PARAM, e.msg)

# validate json
def validate_json(schema, json_value):
    try:
        jsonschema.validate(json_value, schema)
    except jsonschema.ValidationError, e:
        raise APIError(E_ILLEGAL_PARAM, e.message)




# time
def now_ms():
    return int(round(time.time() * 1000))



# random

random.seed(now_ms())

def random_int():
    return (now_ms() << 21) | random.randint(0, 2097152)


def uuid_hex():
    return binascii.hexlify(uuid.uuid1().bytes)

# string
def split2(s, sep):
    return s.split(sep) if s else []


# Multiple lang

def select_lang(json_or_obj, locale):
    d = parse_json(json_or_obj) if isinstance(json_or_obj, basestring) else json_or_obj
    return d[locale] if locale in d else d[u'default']

def select_lang_field(d, locale, k):
    if k in d:
        d[k] = select_lang(d[k], locale)

def select_lang_fields(d, locale, ks):
    for k in ks:
        select_lang_field(d, locale, k)

# Price
def select_lang_for_price(price, locale):
    if price == 0:
        return u'0'

    if locale == u'zh_CN':
        return unicode((price * 1.0 / 100) * 6) + u'￥'
    else:
        return unicode((price * 1.0 / 100)) + u'$'

def select_lang_for_price_field(d, locale, k):
    if k in d:
        d[k] = select_lang_for_price(d[k], locale)

# image

def get_image_size(filepath):
    im = Image.open(filepath)
    return im.size

# file
def get_file_size(filepath):
    return os.path.getsize(filepath)




# change file ext
def change_filename_ext(fp, ext):
    return os.path.join(os.path.dirname(fp), os.path.splitext(fp)[0] + '.' + ext)

def get_filename_ext(fp):
    return os.path.splitext(fp)[1]

# str to bool
def str2bool(s):
    return True if s.lower() in [u'true', u't', u'1', u'yes', u'y', u'1'] else False

# md5 base64

def md5base64(s):
    md5r = hashlib.md5(s).digest()
    return base64.encodestring(md5r)

def md5hex(s, lower=True):
    s = hashlib.md5(s).hexdigest()
    return s.lower() if lower else s.upper()

# locale/lang name

_LOCALE_NAME = {
    u'':u'Default',
    u'default':u'Default',
    u'zh_CN':u'中文(中国)',
    u'en_US':u'English(US)',
}
def get_locale_name(locale):
    return _LOCALE_NAME.get(locale, u'')

# request
def merge_request_params(req):
    ps = {}
    for k, v in req.args.items():
        ps[k] = v
    for k, v in req.form.items():
        ps[k] = v
    for k, v in req.files.items():
        ps[k] = v
    return ps

# string
def str_is_blank(s):
    return s is None or s == u'' or s.strip() == ''

# MAX_INT32
MAX_INT32 = int(2**31-1)

# version string
def version_comparator(x, y):
    def split(ver):
        a = split2(ver, u'.')
        l = len(a)
        if l == 1:
            return int(a[0]), 0, 0
        elif l == 2:
            return int(a[0]), int(a[1]), 0
        else:
            return int(a[0]), int(a[1]), int(a[2])
    xa, xb, xc = split(x)
    ya, yb, yc = split(y)
    if xa < ya:
        return -1
    elif xa > ya:
        return 1
    else:
        if xb < yb:
            return -1
        elif xb > yb:
            return 1
        else:
            if xc < yc:
                return -1
            elif xc > yc:
                return 1
            else:
                return 0

def version_sort_key(ver):
    def split(ver):
        a = split2(ver, u'.')
        l = len(a)
        if l == 1:
            return int(a[0]), 0, 0
        elif l == 2:
            return int(a[0]), int(a[1]), 0
        else:
            return int(a[0]), int(a[1]), int(a[2])
    if str_is_blank(ver):
        return 0
    x, y, z = split(ver)
    return 1000000 * x + y * 1000 + z


def is_greater_than_265():
    vi = sys.version_info
    if vi[0] > 2:
        return True
    if vi[1] > 6:
        return True
    if vi[2] >= 5:
        return True
    return False

# python 2.6
def kws_to_26(kws):
    if not is_greater_than_265():
        kws1 = {}
        for k, v in kws.items():
            if isinstance(v, dict):
                v = kws_to_26(v)
            if isinstance(k, unicode):
                kws1[str(k)] = v
            else:
                kws1[k] = v
        return kws1
    else:
        return kws










