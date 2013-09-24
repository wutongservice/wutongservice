import gv
import os.path
import util
import config

# process upload file

def save_upload_file(f, filename=None):
    upload_folder = gv.app.config[u'UPLOAD_FOLDER']
    fp = os.path.join(upload_folder, filename if filename else f.filename)
    f.save(fp)
    return fp

def process_upload_file(f, func):
    upload_folder = gv.app.config[u'UPLOAD_FOLDER']
    path = os.path.join(upload_folder, util.uuid_hex())
    try:
        f.save(path)
        return func(f.filename, path)
    finally:
        if os.path.exists(path):
            os.remove(path)

def wrap_image_object(o):
    if isinstance(o, dict):
        if u'url' in o:
            url = o[u'url']
            if not url.startswith(u'http://'):
                o[u'url'] = config.get(u'web', u'image_host') + u'/' + url
        return o
    else:
        return o

def wrap_image_field(d, k):
    if k in d:
        d[k] = wrap_image_object(d[k])

def wrap_image_fields(d, ks):
    for k in ks:
        wrap_image_field(d, k)

def wrap_product_url(url):
    if url and not url.startswith(u'http://'):
        return config.get(u'web', u'product_host') + u'/' + url
    else:
        return url

def wrap_product_url_field(d, k):
    if k in d:
        d[k] = wrap_product_url(d[k])