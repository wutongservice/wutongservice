__author__ = 'rongxin.gao@borqs.com'

import flask
import gv
import publisher_services
import purchaser_services
from context import *
import voluptuous as vv
from upload import *
from controller_helper import *
from manifest import Resource
from shutil import copyfile
import os
from werkzeug.datastructures import FileStorage


@gv.app.route(u'/api/v1/publisher/apps/all')
def get_all_apps():
    def process(req):
        ctx = context(req)
        return publisher_services.get_all_apps(ctx)

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/publisher/products/all')
def get_all_products_for_publisher():
    SCHEMA = vv.Schema({
                           vv.Required(u'app'): vv.All(basestring),
                       }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'app' in args:
            ps[u'app'] = args.get(u'app')
        validate_params(SCHEMA, ps)
        ctx = context(req)
        return publisher_services.get_app_products(ctx, ps.get(u'app'))

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/publisher/products/get')
def get_product_version():
    SCHEMA = vv.Schema({
                           vv.Required(u'id'): vv.All(basestring),
                           vv.Required(u'version'): vv.All(basestring, vv.Match(purchaser_services.VERSION_PATTERN))
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
        return publisher_services.get_product(ctx, ps.get(u'id'), ps.get(u'version'))

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/publisher/pricetags/all')
def get_available_pricetags():
    SCHEMA = vv.Schema({
                           vv.Required(u'app'): vv.All(basestring),
                           vv.Required(u'category'): vv.All(basestring)
                       }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'app' in args:
            ps[u'app'] = args.get(u'app')
        if u'category' in args:
            ps[u'category'] = args.get(u'category')
        validate_params(SCHEMA, ps)
        ctx = context(req)
        return publisher_services.get_available_pricetags_for_lang(ctx, ps.get(u'app'), ps.get(u'category'), ctx.locale)

    return api_do(flask.request, process)

class DummyFile:
    def __init__(self, fp, fn=None):
        self.fp = fp
        self.fn = fn or os.path.basename(fp)

    def __nonzero__(self):
        return bool(self.fp)

    def save(self, fp):
        copyfile(self.fp, fp)

    @property
    def filename(self):
        return self.fn

@gv.app.route(u'/api/v1/publisher/products/update', methods=[u'POST'])
def update_product():
    SCHEMA = vv.Schema({
                           u'version': vv.All(basestring, vv.Match(purchaser_services.VERSION_PATTERN))
                       }, extra=True)

    def process(req):
        ps = merge_request_params(req)
        validate_params(SCHEMA, ps)
        ctx = context(req)
        if u'id' not in ps and u'file' in ps:
            f = ps[u'file']
            tmpfile = save_upload_file(f, u'tmp_' + uuid_hex() + u'.tmp')
            ps[u'file'] = DummyFile(tmpfile, f.filename)
            try:
                with Resource(tmpfile) as res:
                    ps[u'id'] = res.manifest.pid

                return publisher_services.upsert_version(ctx, **ps)
            finally:
                try:
                    os.remove(tmpfile)
                except:
                    pass
        else:
            return publisher_services.upsert_version(ctx, **ps)



    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/publisher/products/upload', methods=[u'POST'])
def upload_product():
    SCHEMA = vv.Schema({
                           vv.Required(u'file'): vv.All(FileStorage),
                           vv.Required(u'pricetag_id'): vv.All(basestring),
                       }, extra=True)
    def add_default_lang(mltext, lang):
        r = dict(**mltext)
        r[u'default'] = lang
        return r

    def read_params_from_res(ps0, res, tmpfile):
        manifest = res.manifest
        ps = dict()
        ps[u'pricetag_id'] = ps0[u'pricetag_id']
        ps[u'id'] = manifest.pid
        ps[u'version'] = unicode(manifest.version) + u'.0.0'
        ps[u'supported_mod'] = manifest.supported_mod or u'*'
        if manifest.recent_change:
            ps[u'update_change'] = to_json(add_default_lang(manifest.recent_change, manifest.default_lang))
        ps[u'min_app_vc'] = manifest.min_app_vc or 0
        ps[u'max_app_vc'] = manifest.max_app_vc or MAX_INT32
        ps[u'file'] = DummyFile(tmpfile, ps0[u'file'].filename)
        ps[u'name'] = to_json(add_default_lang(manifest.name, manifest.default_lang))
        ps[u'description'] = to_json(add_default_lang(manifest.description, manifest.default_lang))
        if manifest.author_name:
            ps[u'author_name'] = manifest.author_name
        if manifest.author_email:
            ps[u'author_email'] = manifest.author_email
        if manifest.author_phone:
            ps[u'author_phone'] = manifest.author_phone
        if manifest.author_website:
            ps[u'author_website'] = manifest.author_website
        if res.logo_path:
            ps[u'logo_file'] = DummyFile(res.logo_path)
        if res.cover_path:
            ps[u'cover_file'] = DummyFile(res.cover_path)
        if res.screenshot1_path:
            ps[u'screenshot1_file'] = DummyFile(res.screenshot1_path)
        if res.screenshot2_path:
            ps[u'screenshot2_file'] = DummyFile(res.screenshot2_path)
        if res.screenshot3_path:
            ps[u'screenshot3_file'] = DummyFile(res.screenshot3_path)
        if res.screenshot4_path:
            ps[u'screenshot4_file'] = DummyFile(res.screenshot4_path)
        if res.screenshot4_path:
            ps[u'screenshot5_file'] = DummyFile(res.screenshot5_path)
        return ps

    def process(req):
        ps = merge_request_params(req)
        validate_params(SCHEMA, ps)
        ctx = context(req)
        f = ps[u'file']
        tmpfile = save_upload_file(f, u'tmp_' + uuid_hex() + u'.tmp')
        try:
            r = None
            with Resource(tmpfile) as res:
                ps1 = read_params_from_res(ps, res, tmpfile)
                r = publisher_services.upsert_version(ctx, **ps1)
            return r
        finally:
            try:
                os.remove(tmpfile)
            except:
                pass

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/publisher/products/active')
def active_product():
    SCHEMA = vv.Schema({
                           vv.Required(u'id'): vv.All(basestring),
                           vv.Required(u'flag'): vv.All(bool)
                       }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'id' in args:
            ps[u'id'] = args.get(u'id')
        if u'flag' in args:
            ps[u'flag'] = str2bool(args.get(u'flag'))
        ctx = context(req)
        validate_params(SCHEMA, ps)
        return publisher_services.active_product(ctx, ps[u'id'], ps[u'flag'])

    return api_do(flask.request, process)


@gv.app.route(u'/api/v1/publisher/products/versions/active')
def active_version():
    SCHEMA = vv.Schema({
                           vv.Required(u'id'): vv.All(basestring),
                           vv.Required(u'version'): vv.All(basestring),
                           vv.Required(u'flag'): vv.All(bool)
                       }, extra=True)

    def process(req):
        args = req.args
        ps = dict()
        if u'id' in args:
            ps[u'id'] = args.get(u'id')
        if u'version' in args:
            ps[u'version'] = args.get(u'version')
        if u'flag' in args:
            ps[u'flag'] = str2bool(args.get(u'flag'))
        ctx = context(req)
        validate_params(SCHEMA, ps)
        return publisher_services.active_version(ctx, ps[u'id'], ps[u'version'], ps[u'flag'])

    return api_do(flask.request, process)

# ==========================================

import re


def _validate_fields(mod, ps):
    def validate_required(error_info, f, type=None):
        if f in ps:
            if type is not None and not isinstance(ps[f], type):
                error_info.append(u'Type error %s' % f)
        else:
            error_info.append(u'Missing %s' % f)

    def validate_length(error_info, f, max):
        if f in ps:
            l = len(ps[f])
            if not ps.get(f, u'').strip():
                error_info.append(u'Text is empty or blank (%s)' % f)
            if l >= max:
                error_info.append(u'Text is too lang (%s)' % f)

    def validate_pattern(error_info, f, patt):
        if f in ps:
            if not re.match(patt, str(ps[f])):
                error_info.append(u'Pattern error (%s)' % f)

    def validate_file_ext_in(error_info, f, file_exts):
        if f in ps:
            ff = ps[f]
            if ff:
                ext = get_filename_ext(ff.filename).lower()
                if ext not in file_exts:
                    error_info.append(u'File type error (%s)' % f)

    def validate_file_required(error_info, f):
        b = False
        if f in ps:
            if ps[f]:
                b = True
        if not b:
            error_info.append(u'Missing file (%s)' % f)

    ei = []
    min_app_vc = ps.get(u'min_app_vc', u'0').strip()
    max_app_vc = ps.get(u'max_app_vc', u'0').strip()
    ps[u'min_app_vc'] = 0 if min_app_vc in [None, u'', u'0', u'*'] else int(min_app_vc)
    ps[u'max_app_vc'] = MAX_INT32 if max_app_vc in [None, u'', u'0', u'*'] else int(max_app_vc)

    validate_required(ei, u'name', basestring);
    validate_length(ei, u'name', 64)
    validate_required(ei, u'version', basestring);
    validate_pattern(ei, u'version', purchaser_services.VERSION_PATTERN)
    validate_required(ei, u'description', basestring);
    validate_length(ei, u'description', 2048)
    validate_required(ei, u'update_change', basestring);
    validate_length(ei, u'update_change', 1024)
    validate_required(ei, u'min_app_vc', int)
    validate_required(ei, u'max_app_vc', int)
    validate_required(ei, u'supported_mod', basestring)
    validate_required(ei, u'author_name', basestring);
    validate_length(ei, u'author_name', 64)
    validate_required(ei, u'author_email', basestring);
    validate_length(ei, u'author_email', 128)
    IMAGE_EXTS = [u'.jpg', u'.png']
    validate_file_ext_in(ei, u'logo_file', IMAGE_EXTS)
    validate_file_ext_in(ei, u'cover_file', IMAGE_EXTS)
    validate_file_ext_in(ei, u'screenshot1_file', IMAGE_EXTS)
    validate_file_ext_in(ei, u'screenshot2_file', IMAGE_EXTS)
    validate_file_ext_in(ei, u'screenshot3_file', IMAGE_EXTS)
    validate_file_ext_in(ei, u'screenshot4_file', IMAGE_EXTS)
    validate_file_ext_in(ei, u'screenshot5_file', IMAGE_EXTS)

    if mod in [u'new_product', u'new_version']:
        validate_file_required(ei, u'logo_file')
        validate_file_required(ei, u'cover_file')
        validate_file_required(ei, u'file')
    return ei


@gv.app.route(u'/publisher/products/transit.html')
def transit_page():
    ctx = context(flask.request)
    all_apps = publisher_services.get_all_apps(ctx)
    app = flask.request.args.get(u'app')
    version = flask.request.args.get(u'version')
    id = flask.request.args.get(u'id')
    return flask.render_template(u'publisher_products_transit.jinja2',
                                 page_title=u'Continue editing?',
                                 all_apps=all_apps,
                                 app=app,
                                 version=version,
                                 id=id,
                                 ticket=flask.session.get(u'ticket', u''),
    )


def redirect_to_welcome_for_publisher():
    return flask.redirect(u'/publisher/index.html')


@gv.app.route(u'/publisher/index.html')
def index_page():
    ctx = context(flask.request)
    all_apps = publisher_services.get_all_apps(ctx)
    return flask.render_template(u'publisher_index.jinja2',
                                 page_title=u'Index',
                                 all_apps=all_apps,
                                 ticket=flask.session.get(u'ticket', u''),
    )


def _redirect_to_index():
    return flask.redirect(u'/publisher/index.html')


def _find_app_name(all_apps, app_id):
    for app in all_apps:
        if app[u'id'] == app_id:
            return app[u'name']
    return u''


@gv.app.route(u'/publisher/products/list.html')
def list_products_page():
    ctx = context(flask.request)
    if not ctx.has_borqs_id():
        return redirect_to_signin()

    app = flask.request.args.get(u'app')
    if not app:
        app = flask.session.get(u'current_app')
        if app:
            return flask.redirect(u'/publisher/products/list.html?app=' + app)
        else:
            return _redirect_to_index()

    all_apps = publisher_services.get_all_apps(ctx)
    if not all_apps:
        return _redirect_to_index()

    categories = publisher_services.get_app_categories(ctx, app)
    products = publisher_services.get_app_products(ctx, app)
    category_products = []
    for c in categories:
        c1 = {
            u'app': c[u'app_id'],
            u'category': c[u'category'],
            u'category_name': c[u'category_name'],
            u'products': []
        }
        for p in products:
            if p[u'category'] == c[u'category']:
                c1[u'products'].append(p)
        category_products.append(c1)

    flask.session[u'current_app'] = app
    resp = flask.render_template(u'publisher_product_list.jinja2',
                                 page_title=u'Edit product',
                                 app_id=app,
                                 all_apps=all_apps,
                                 category_products=category_products,
                                 ticket=flask.session.get(u'ticket', u''),
                                 nav_paths=[],
    )
    return resp


@gv.app.route(u'/publisher/products/new.html', methods=[u'GET', u'POST'])
def new_product_page():
    ctx = context(flask.request)
    if not ctx.has_borqs_id():
        return redirect_to_signin()
    app = flask.request.args.get(u'app')
    category = flask.request.args.get(u'category')
    lang = u'default'
    if not app:
        return error_page(u'Error', u'Missing param app')
    if not category:
        return error_page(u'Error', u'Missing param category')

    all_apps = publisher_services.get_all_apps(ctx)
    available_pricetags = publisher_services.get_available_pricetags_for_lang(ctx, app, category, lang)
    nav_paths = [(_find_app_name(all_apps, app), u'/publisher/products/list.html?app=%s' % app)]
    if flask.request.method == u'POST':
        ps = merge_request_params(flask.request)
        error_info = _validate_fields(u'new_product', ps)
        if not error_info:
            p = publisher_services.upsert_version_for_lang(ctx, ps[u'lang'],
                                                           ps.get(u'id', None), ps[u'version'],
                                                           ps[u'name'], ps[u'pricetag_id'], ps[u'description'],
                                                           ps[u'update_change'],
                                                           ps[u'min_app_vc'], ps[u'max_app_vc'], ps[u'supported_mod'],
                                                           ps[u'author_name'], ps[u'author_email'],
                                                           ps[u'logo_file'], ps[u'cover_file'], ps[u'screenshot1_file'],
                                                           ps[u'screenshot2_file'], ps[u'screenshot3_file'],
                                                           ps[u'screenshot4_file'], ps[u'screenshot5_file'],
                                                           ps[u'file']
            )
            return flask.redirect(
                u'/publisher/products/transit.html?app=%s&version=%s&id=%s' % (p[u'app_id'], p[u'version'], p[u'id']))
        else:
            return flask.render_template(u'publisher_product_edit.jinja2',
                                         page_title=u'New product',
                                         all_apps=all_apps,
                                         app_id=app,
                                         category=category,
                                         lang=lang,
                                         lang_name=get_locale_name(lang),
                                         available_pricetags=available_pricetags,
                                         id=ps.get(u'id'),
                                         version=ps.get(u'version'),
                                         name=ps.get(u'name'),
                                         pricetag_id=ps.get(u'pricetag_id'),
                                         description=ps.get(u'description'),
                                         update_change=ps.get(u'update_change'),
                                         min_app_vc=ps.get(u'min_app_vc'),
                                         max_app_vc=ps.get(u'max_app_vc'),
                                         supported_mod=ps.get(u'supported_mod'),
                                         author_name=ps.get(u'author_name'),
                                         author_email=ps.get(u'author_email'),
                                         logo_image=u'',
                                         cover_image=u'',
                                         screenshot1_image=u'',
                                         screenshot2_image=u'',
                                         screenshot3_image=u'',
                                         screenshot4_image=u'',
                                         screenshot5_image=u'',
                                         action_url=u'',
                                         errors=error_info,
                                         ticket=flask.session.get(u'ticket', u''),
                                         mod=u'new_product',
                                         nav_paths=nav_paths,
            )
    else:
        return flask.render_template(u'publisher_product_edit.jinja2',
                                     page_title=u'New',
                                     all_apps=all_apps,
                                     app_id=app,
                                     category=category,
                                     lang=lang,
                                     lang_name=get_locale_name(lang),
                                     available_pricetags=available_pricetags,
                                     id=u'',
                                     version=u'',
                                     name=u'',
                                     pricetag_id=u'',
                                     description=u'',
                                     update_change=u'',
                                     min_app_vc=u'0',
                                     max_app_vc=u'',
                                     supported_mod=u'',
                                     author_name=u'',
                                     author_email=u'',
                                     logo_image=u'',
                                     cover_image=u'',
                                     screenshot1_image=u'',
                                     screenshot2_image=u'',
                                     screenshot3_image=u'',
                                     screenshot4_image=u'',
                                     screenshot5_image=u'',
                                     action_url=u'',
                                     ticket=flask.session.get(u'ticket', u''),
                                     errors=[],
                                     nav_paths=nav_paths,
                                     mod=u'new_product',
        )


@gv.app.route(u'/publisher/products/versions/new.html', methods=[u'GET', u'POST'])
def new_version_page():
    ctx = context(flask.request)
    if not ctx.has_borqs_id():
        return redirect_to_signin()

    lang = u'default'
    all_apps = publisher_services.get_all_apps(ctx)

    if flask.request.method == u'POST':
        ps = merge_request_params(flask.request)
        error_info = _validate_fields(u'update_version', ps)
        if not error_info:
            p = publisher_services.upsert_version_for_lang(ctx, ps[u'lang'],
                                                           ps.get(u'id', None), ps[u'version'],
                                                           ps[u'name'], ps[u'pricetag_id'], ps[u'description'],
                                                           ps[u'update_change'],
                                                           ps[u'min_app_vc'], ps[u'max_app_vc'], ps[u'supported_mod'],
                                                           ps[u'author_name'], ps[u'author_email'],
                                                           ps[u'logo_file'], ps[u'cover_file'], ps[u'screenshot1_file'],
                                                           ps[u'screenshot2_file'], ps[u'screenshot3_file'],
                                                           ps[u'screenshot4_file'], ps[u'screenshot5_file'],
                                                           ps[u'file']
            )
            #available_pricetags = publisher_services.get_available_pricetags_for_lang_by_pid(ctx, p[u'id'], lang)
            return flask.redirect(
                u'/publisher/products/transit.html?app=%s&version=%s&id=%s' % (p[u'app_id'], p[u'version'], p[u'id']))
        else:
            p = publisher_services.get_product_for_lang(ctx, ps[u'id'], ps[u'version'], lang)
            available_pricetags = publisher_services.get_available_pricetags_for_lang_by_pid(ctx, ps[u'id'], lang)
            app, category = publisher_services.get_app_and_category_for_product(p[u'id'])
            nav_paths = [(_find_app_name(all_apps, app), u'/publisher/products/list.html?app=%s' % app)]
            return flask.render_template(u'publisher_product_edit.jinja2',
                                         page_title=u'New version',
                                         all_apps=all_apps,
                                         app_id=p[u'app_id'],
                                         category=p[u'category'],
                                         lang=lang,
                                         lang_name=get_locale_name(lang),
                                         available_pricetags=available_pricetags,
                                         id=p[u'id'],
                                         version=p[u'version'],
                                         name=p[u'name'],
                                         pricetag_id=p[u'pricetag_id'],
                                         description=p[u'description'],
                                         update_change=p[u'update_change'],
                                         min_app_vc=p[u'min_app_vc'],
                                         max_app_vc=p[u'max_app_vc'],
                                         supported_mod=p[u'supported_mod'],
                                         author_name=p[u'author_name'],
                                         author_email=p[u'author_email'],
                                         logo_image=p[u'logo_image'].get(u'url', u''),
                                         cover_image=p[u'cover_image'].get(u'url', u''),
                                         screenshot1_image=p[u'screenshot1_image'].get(u'url', u''),
                                         screenshot2_image=p[u'screenshot2_image'].get(u'url', u''),
                                         screenshot3_image=p[u'screenshot3_image'].get(u'url', u''),
                                         screenshot4_image=p[u'screenshot4_image'].get(u'url', u''),
                                         screenshot5_image=p[u'screenshot5_image'].get(u'url', u''),
                                         action_url=p.get(u'action_url', u''),
                                         ticket=flask.session.get(u'ticket', u''),
                                         errors=error_info,
                                         nav_paths=nav_paths,
                                         mod=u'new_version'
            )
    else:
        id = flask.request.args.get(u'id')
        if not id:
            return error_page(u'Error', u'Missing param "id"')
        version = publisher_services.get_last_version(id)
        if not version:
            return error_page(u'New versions for the product')

        try:
            p = publisher_services.get_product_for_lang(ctx, id, version, lang)
        except:
            p = None
        if not p:
            return error_page(u'Error', u'Invalid product version')

        app, category = publisher_services.get_app_and_category_for_product(id)

        available_pricetags = publisher_services.get_available_pricetags_for_lang(ctx, p[u'app_id'], p[u'category'],
                                                                                  lang)
        nav_paths = [(_find_app_name(all_apps, app), u'/publisher/products/list.html?app=%s' % app)]
        return flask.render_template(u'publisher_product_edit.jinja2',
                                     page_title=u'New version',
                                     all_apps=all_apps,
                                     app_id=app,
                                     category=category,
                                     lang=lang,
                                     lang_name=get_locale_name(lang),
                                     available_pricetags=available_pricetags,
                                     id=p[u'id'],
                                     version=p[u'version'],
                                     name=p[u'name'],
                                     pricetag_id=p[u'pricetag_id'],
                                     description=p[u'description'],
                                     update_change=p[u'update_change'],
                                     min_app_vc=p[u'min_app_vc'],
                                     max_app_vc=p[u'max_app_vc'],
                                     supported_mod=p[u'supported_mod'],
                                     author_name=p[u'author_name'],
                                     author_email=p[u'author_email'],
                                     logo_image=p[u'logo_image'].get(u'url', u''),
                                     cover_image=p[u'cover_image'].get(u'url', u''),
                                     screenshot1_image=p[u'screenshot1_image'].get(u'url', u''),
                                     screenshot2_image=p[u'screenshot2_image'].get(u'url', u''),
                                     screenshot3_image=p[u'screenshot3_image'].get(u'url', u''),
                                     screenshot4_image=p[u'screenshot4_image'].get(u'url', u''),
                                     screenshot5_image=p[u'screenshot5_image'].get(u'url', u''),
                                     action_url=p.get(u'action_url'),
                                     ticket=flask.session.get(u'ticket', u''),
                                     errors=[],
                                     nav_paths=nav_paths,
                                     mod=u'new_version'
        )


AVAILABLE_LANGS = ['default', 'en_US', 'zh_CN']
AVAILABLE_LANGS_OPTIONS = [(lang, get_locale_name(lang)) for lang in AVAILABLE_LANGS]


@gv.app.route(u'/publisher/products/versions/update.html', methods=[u'GET', u'POST'])
def update_version_page():
    ctx = context(flask.request)
    if not ctx.has_borqs_id():
        return redirect_to_signin()

    all_apps = publisher_services.get_all_apps(ctx)
    if flask.request.method == u'POST':
        lang = flask.request.form.get(u'lang', u'default')
        ps = merge_request_params(flask.request)
        error_info = _validate_fields(u'update_version', ps)
        if not error_info:
            p = publisher_services.upsert_version_for_lang(ctx, ps[u'lang'],
                                                           ps.get(u'id', None), ps[u'version'],
                                                           ps[u'name'], ps[u'pricetag_id'], ps[u'description'],
                                                           ps[u'update_change'],
                                                           ps[u'min_app_vc'], ps[u'max_app_vc'], ps[u'supported_mod'],
                                                           ps[u'author_name'], ps[u'author_email'],
                                                           ps[u'logo_file'], ps[u'cover_file'], ps[u'screenshot1_file'],
                                                           ps[u'screenshot2_file'], ps[u'screenshot3_file'],
                                                           ps[u'screenshot4_file'], ps[u'screenshot5_file'],
                                                           ps[u'file']
            )
            available_pricetags = publisher_services.get_available_pricetags_for_lang_by_pid(ctx, p[u'id'], lang)
            nav_paths = [
                (_find_app_name(all_apps, p[u'app_id']), u'/publisher/products/list.html?app=%s' % p[u'app_id'])]
            return flask.render_template(u'publisher_product_edit.jinja2',
                                         page_title=u'Update version',
                                         all_apps=all_apps,
                                         app_id=p[u'app_id'],
                                         category=p[u'category'],
                                         lang=lang,
                                         lang_name=get_locale_name(lang),
                                         available_pricetags=available_pricetags,
                                         id=p[u'id'],
                                         version=p[u'version'],
                                         name=p[u'name'],
                                         pricetag_id=p[u'pricetag_id'],
                                         description=p[u'description'],
                                         update_change=p[u'update_change'],
                                         min_app_vc=p[u'min_app_vc'],
                                         max_app_vc=p[u'max_app_vc'],
                                         supported_mod=p[u'supported_mod'],
                                         author_name=p[u'author_name'],
                                         author_email=p[u'author_email'],
                                         logo_image=p[u'logo_image'].get(u'url', u''),
                                         cover_image=p[u'cover_image'].get(u'url', u''),
                                         screenshot1_image=p[u'screenshot1_image'].get(u'url', u''),
                                         screenshot2_image=p[u'screenshot2_image'].get(u'url', u''),
                                         screenshot3_image=p[u'screenshot3_image'].get(u'url', u''),
                                         screenshot4_image=p[u'screenshot4_image'].get(u'url', u''),
                                         screenshot5_image=p[u'screenshot5_image'].get(u'url', u''),
                                         action_url=p.get(u'action_url', u''),
                                         errors=error_info,
                                         available_langs=AVAILABLE_LANGS_OPTIONS,
                                         ticket=flask.session.get(u'ticket', u''),
                                         nav_paths=nav_paths,
                                         mod=u'update_version'
            )
        else:
            p = publisher_services.get_product_for_lang(ctx, ps[u'id'], ps[u'version'], lang)
            available_pricetags = publisher_services.get_available_pricetags_for_lang_by_pid(ctx, ps[u'id'], lang)
            nav_paths = [
                (_find_app_name(all_apps, p[u'app_id']), u'/publisher/products/list.html?app=%s' % p[u'app_id'])]
            return flask.render_template(u'publisher_product_edit.jinja2',
                                         page_title=u'Update version',
                                         all_apps=all_apps,
                                         app_id=p[u'app_id'],
                                         category=p[u'category'],
                                         lang=lang,
                                         lang_name=get_locale_name(lang),
                                         available_pricetags=available_pricetags,
                                         id=p[u'id'],
                                         version=p[u'version'],
                                         name=p[u'name'],
                                         pricetag_id=p[u'pricetag_id'],
                                         description=p[u'description'],
                                         update_change=p[u'update_change'],
                                         min_app_vc=p[u'min_app_vc'],
                                         max_app_vc=p[u'max_app_vc'],
                                         supported_mod=p[u'supported_mod'],
                                         author_name=p[u'author_name'],
                                         author_email=p[u'author_email'],
                                         logo_image=p[u'logo_image'].get(u'url', u''),
                                         cover_image=p[u'cover_image'].get(u'url', u''),
                                         screenshot1_image=p[u'screenshot1_image'].get(u'url', u''),
                                         screenshot2_image=p[u'screenshot2_image'].get(u'url', u''),
                                         screenshot3_image=p[u'screenshot3_image'].get(u'url', u''),
                                         screenshot4_image=p[u'screenshot4_image'].get(u'url', u''),
                                         screenshot5_image=p[u'screenshot5_image'].get(u'url', u''),
                                         action_url=p.get(u'action_url', u''),
                                         errors=error_info,
                                         available_langs=AVAILABLE_LANGS_OPTIONS,
                                         ticket=flask.session.get(u'ticket', u''),
                                         nav_paths=nav_paths,
                                         mod=u'update_version'
            )
    else:
        lang = flask.request.args.get(u'lang', u'default')
        id = flask.request.args.get(u'id')
        version = flask.request.args.get(u'version')
        if not id:
            return error_page(u'Error', u'Missing param "id"')
        if not version:
            return error_page(u'Error', u'Missing param "version"')

        try:
            p = publisher_services.get_product_for_lang(ctx, id, version, lang)
        except:
            p = None
        if not p:
            return error_page(u'Error', u'Invalid product version')

        app, category = publisher_services.get_app_and_category_for_product(id)
        nav_paths = [(_find_app_name(all_apps, app), u'/publisher/products/list.html?app=%s' % app)]
        available_pricetags = publisher_services.get_available_pricetags_for_lang(ctx, p[u'app_id'], p[u'category'],
                                                                                  lang)
        return flask.render_template(u'publisher_product_edit.jinja2',
                                     page_title=u'Update version',
                                     all_apps=all_apps,
                                     app_id=app,
                                     category=category,
                                     lang=lang,
                                     lang_name=get_locale_name(lang),
                                     available_pricetags=available_pricetags,
                                     id=p[u'id'],
                                     version=p[u'version'],
                                     name=p[u'name'],
                                     pricetag_id=p[u'pricetag_id'],
                                     description=p[u'description'],
                                     update_change=p[u'update_change'],
                                     min_app_vc=p[u'min_app_vc'],
                                     max_app_vc=p[u'max_app_vc'],
                                     supported_mod=p[u'supported_mod'],
                                     author_name=p[u'author_name'],
                                     author_email=p[u'author_email'],
                                     logo_image=p[u'logo_image'].get(u'url', u''),
                                     cover_image=p[u'cover_image'].get(u'url', u''),
                                     screenshot1_image=p[u'screenshot1_image'].get(u'url', u''),
                                     screenshot2_image=p[u'screenshot2_image'].get(u'url', u''),
                                     screenshot3_image=p[u'screenshot3_image'].get(u'url', u''),
                                     screenshot4_image=p[u'screenshot4_image'].get(u'url', u''),
                                     screenshot5_image=p[u'screenshot5_image'].get(u'url', u''),
                                     action_url=p.get(u'action_url'),
                                     errors=[],
                                     available_langs=AVAILABLE_LANGS_OPTIONS,
                                     ticket=flask.session.get(u'ticket', u''),
                                     nav_paths=nav_paths,
                                     mod=u'update_version'
        )




