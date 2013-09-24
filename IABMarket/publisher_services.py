__author__ = 'rongxin.gao@borqs.com'

import gv
from dbwrapper import *
from util import *
from upload import *
import account_services

def _check_borqs_id(ctx):
    if not ctx.has_borqs_id():
        raise APIError(E_MISSING_IDENTITY, u'Missing Borqs ID(Ticket)')


def _check_permission(ctx, borqs_id):
    _check_borqs_id(ctx)
    if ctx.borqs_id != borqs_id:
        raise APIError(E_PERMISSION_DENIED, u'Permission denied')


def get_all_apps(ctx):
    SQL = u"""
    SELECT
        id,
        name
    FROM
        apps
    ORDER BY
        id
    """
    apps = list(gv.db.fetch_all(SQL))
    for app in apps:
        select_lang_field(app, ctx.locale, u'name')
    return apps

def get_app_categories(ctx, app_id):
    SQL1 = u"""
    SELECT
        app_id as app_id,
        category as category,
        name as category_name
    FROM
        categories
    WHERE
        app_id=%s
    """
    categories = list(gv.db.fetch_all(SQL1, (app_id)))
    for category in categories:
        select_lang_field(category, ctx.locale, u'category_name')
    return categories

def get_app_products(ctx, app_id):
    SQL1 = u"""
    SELECT
        apps.id as app_id,
        apps.name as app_name,
        products.id as id,
        products.name as name,
        categories.category as category,
        categories.name as category_name,
        MAX(product_versions.version) AS last_version,
        products.status as status,
        products.logo_image as logo_image,
        products.cover_image as cover_image
    FROM
        product_versions, products, pricetags, categories, apps
    WHERE
        (product_versions.product_id=products.id)
        AND (products.pricetag_id=pricetags.id)
        AND (pricetags.app_id=apps.id)
        AND (categories.app_id=apps.id)
        AND (pricetags.category=categories.category)
        AND (apps.id=%s)
        AND (products.author_borqs_id=%s)
    GROUP BY
        product_versions.product_id
    ORDER BY
        products.updated_at DESC
    """

    SQL2 = u"""
    SELECT
        version,
        status
    FROM
        product_versions
    WHERE
        product_id=%s
    ORDER BY
        version DESC
    """

    _check_borqs_id(ctx)

    def handle_conn(conn):
        products = FetchAll(SQL1, (app_id, ctx.borqs_id))(conn)
        for p in products:
            versions = list(FetchAll(SQL2, (p[u'id']))(conn))
            versions = sorted(versions, key = lambda x:version_sort_key(x[u'version']), reverse = True)
            select_lang_fields(p, ctx.locale, [u'app_name', u'name', u'category_name'])
            parse_json_fields(p, [u'logo_image', u'cover_image'])
            wrap_image_field(p, u'logo_image')
            wrap_image_field(p, u'cover_image')
            p[u'versions'] = versions
        return products

    return gv.db.open_conn(handle_conn)


def get_product(ctx, pid, version):
    SQL1 = u"""
    SELECT
        products.id as id,
        products.name as name,
        products.author_name as author_name,
        products.author_email as author_email,
        products.author_google_id as author_google_id,
        products.author_borqs_id as author_borqs_id,
        products.description as description,
        products.supported_languages as supported_languages,
        products.status as status,
        products.logo_image as logo_image,
        products.cover_image as cover_image,
        products.screenshot1_image as screenshot1_image,
        products.screenshot2_image as screenshot2_image,
        products.screenshot3_image as screenshot3_image,
        products.screenshot4_image as screenshot4_image,
        products.screenshot5_image as screenshot5_image,
        product_versions.version as version,
        product_versions.support_mod as supported_mod,
        product_versions.status as version_status,
        product_versions.update_change as update_change,
        product_versions.min_app_vc as min_app_vc,
        product_versions.max_app_vc as max_app_vc,
        product_versions.action as action,
        product_versions.action_url as action_url,
        product_versions.file_size as file_size,
        pricetags.id as pricetag_id,
        pricetags.app_id as app_id,
        pricetags.category as category,
        pricetags.price as price
    FROM
        product_versions, products, pricetags
    WHERE
        (product_versions.product_id=products.id)
        AND (products.pricetag_id=pricetags.id)
        AND (products.id=%s)
        AND (product_versions.version=%s)
    """

    SQL2 = u"""
    SELECT
        version,
        status
    FROM
        product_versions
    WHERE
        product_id=%s
    ORDER BY
        version DESC
    """

    SQL3 = u"""
    SELECT
        id,
        price
    FROM
        pricetags
    WHERE
        app_id=%s
        AND category=%s
    ORDER BY
        price
    """

    def handle_conn(conn):
        p = FetchFirst(SQL1, (pid, version))(conn)
        if not p:
            raise APIError(E_ILLEGAL_PRODUCT, u'Illegal product')
        _check_permission(ctx, p[u'author_borqs_id'])
        parse_json_fields(p, [u'name', u'description', u'update_change',
                              u'logo_image', u'cover_image',
                              u'screenshot1_image', u'screenshot2_image', u'screenshot3_image',
                              u'screenshot4_image', u'screenshot5_image'])
        wrap_image_fields(p,
            [u'logo_image', u'cover_image', u'screenshot1_image', u'screenshot2_image', u'screenshot3_image',
             u'screenshot4_image', u'screenshot5_image'])
        wrap_product_url_field(p, u'action_url')
        p[u'supported_languages'] = split2(p[u'supported_languages'], u',')
        p[u'versions'] = FetchAll(SQL2, (pid))(conn)
        p[u'versions'] = sorted(p[u'versions'], key = lambda x:version_sort_key(x[u'version']), reverse = True)
        p[u'available_prices'] = FetchAll(SQL3, (p[u'app_id'], p[u'category']))(conn)
        return p

    return gv.db.open_conn(handle_conn)

def get_product_for_lang(ctx, pid, version, lang):
    if not lang:
        lang = u'default'
    p = get_product(ctx, pid, version)
    select_lang_fields(p, lang, [u'name', u'description', u'update_change'])
    return p


def get_available_pricetags(ctx, app_id, category):
    SQL1 = u"""
    SELECT
        id,
        price
    FROM
        pricetags
    WHERE
        app_id=%s
        AND category=%s
    ORDER BY
        price
    """
    return list(gv.db.fetch_all(SQL1, (app_id, category)))

def get_available_pricetags_for_lang(ctx, app_id, category, lang):
    pts = get_available_pricetags(ctx, app_id, category)
    for pt in pts:
        pt[u'price_name'] = select_lang_for_price(pt[u'price'], lang)
    return pts

def get_available_pricetags_by_pid(ctx, pid):
    SQL1 = u"""
    SELECT
        pricetags.app_id as app_id,
        pricetags.category as category
    FROM
        products, pricetags
    WHERE
        products.pricetag_id=pricetags.id
        AND products.id = %s
    ORDER BY
        price
    """
    SQL2 = u"""
    SELECT
        id,
        price
    FROM
        pricetags
    WHERE
        app_id=%s
        AND category=%s
    ORDER BY
        price
    """
    def handle_conn(conn):
        row = FetchFirst(SQL1, (pid))(conn)
        if row:
            return list(FetchAll(SQL2, (row[u'app_id'], row[u'category']))(conn))
        else:
            return []
    return gv.db.open_conn(handle_conn)

def get_available_pricetags_for_lang_by_pid(ctx, pid, lang):
    pts = get_available_pricetags_by_pid(ctx, pid)
    for pt in pts:
        pt[u'price_name'] = select_lang_for_price(pt[u'price'], lang)
    return pts

ML_JSON_SCHEMA = {
    u'type': u'object',
    u'properties': {
        u'default': {u'type':u'string'},
        u'zh_CN': {u'type':u'string'},
        u'en_US': {u'type':u'string'},
    },
    u'required': [u'default']
}

def _check_text_field(json, field_name):
    try:
        o = parse_json(json)
        if is_greater_than_265():
            jsonschema.validate(kws_to_26(o), kws_to_26(ML_JSON_SCHEMA))
    except BaseException, e:
        raise APIError(E_ILLEGAL_PARAM, u'Illegal text field %s' % field_name)


def get_app_and_category_for_product(pid):
    SQL1 = u"SELECT pricetags.app_id as app_id, pricetags.category as category FROM products, pricetags WHERE products.pricetag_id = pricetags.id AND products.id=%s"
    row = gv.db.fetch_first(SQL1, (pid))
    return  row[u'app_id'], row[u'category'] if row else None

def upsert_version(ctx, **ps):
    SQL1 = u"SELECT * FROM products WHERE id=%s"
    SQL2 = u"SELECT version FROM product_versions WHERE product_id=%s"
    SQL3 = u"""
        INSERT INTO product_versions (
            product_id,
            version,
            created_at,
            updated_at,
            support_mod,
            status,
            update_change,
            min_app_vc,
            max_app_vc,
            action,
            action_url,
            file_size
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
        );
    """
    SQL5 = u"""
        UPDATE products SET
            updated_at=%s,
            pricetag_id=%s,
            name=%s,
            type1=%s,
            type2=%s,
            description=%s,
            author_name=%s,
            author_email=%s,
            author_google_id=%s,
            7 pic
        WHERE
            id=%s
        ;
    """

    SQL6 = u"""
        INSERT INTO products (
            id,
            created_at,
            updated_at,
            pricetag_id,
            supported_languages,
            name,
            type1,
            type2,
            status,
            description,
            author_name,
            author_email,
            author_borqs_id,
            author_google_id,
            download_count
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
        )
        ;
    """

    _check_borqs_id(ctx)
    version = ps[u'version']

    if not u'id' in ps:
        pid = u'p_' + uuid_hex()
    else:
        pid = ps[u'id']

    supported_mod = ps.get(u'supported_mod', u'*')
    update_change = ps.get(u'update_change', to_json({u'default': u''}))
    min_app_vc = ps.get(u'min_app_vc', 0)
    max_app_vc = ps.get(u'max_app_vc', int(1 << 31))
    action = u'download'
    if u'file' in ps:
        f = ps[u'file']
        fn = pid + u'_' + version + get_filename_ext(f.filename)
        action_url = save_upload_file(ps[u'file'], fn)
        file_size = get_file_size(action_url)
        action_url = os.path.basename(action_url)
    else:
        action_url = u''
        file_size = 0

    SQL4 = u"UPDATE product_versions SET updated_at=%s "
    if u'supported_mod' in ps:
        SQL4 += u", support_mod=" + sqlval(supported_mod)
    if u'update_change' in ps:
        _check_text_field(update_change, u'update_change')
        SQL4 += u", update_change=" + sqlval(update_change)
    if u'min_app_vc' in ps:
        SQL4 += u", min_app_vc=" + sqlval(min_app_vc)
    if u'max_app_vc' in ps:
        SQL4 += u", max_app_vc=" + sqlval(max_app_vc)
    if u'file' in ps:
        SQL4 += u", action=%s, action_url=%s, file_size=%s" % (sqlval(action), sqlval(action_url), sqlval(file_size))
    SQL4 += u" WHERE product_id=%s AND version=%s"

    SQL5 = u"UPDATE products SET updated_at=%s "
    if u'supported_languages' in ps:
        SQL5 += u", supported_languages=" + sqlval(ps[u'supported_languages'])
    if u'pricetag_id' in ps:
        SQL5 += u" ,pricetag_id=" + sqlval(ps[u'pricetag_id'])
    if u'name' in ps:
        _check_text_field(ps[u'name'], u'name')
        SQL5 += u", name=" + sqlval(ps[u'name'])
        # type1, type2
    if u'description' in ps:
        _check_text_field(ps[u'description'], u'description')
        SQL5 += u", description=" + sqlval(ps[u'description'])
    if u'author_name' in ps:
        SQL5 += u", author_name=" + sqlval(ps[u'author_name'])
    if u'author_email' in ps:
        SQL5 += u", author_email=" + sqlval(ps[u'author_email'])
    if u'author_google_id' in ps:
        SQL5 += u", author_google_id=" + sqlval(ps[u'author_google_id'])

    def save_image_file(psk, postfix):
        f = ps[psk]
        fn = pid + u'_' + version + u'_' + postfix + get_filename_ext(f.filename)
        fp = save_upload_file(f, fn)
        size = get_image_size(fp)
        return {u'url': fn, u'width': size[0], u'height': size[1]}

    if u'logo_file' in ps:
        imo = save_image_file(u'logo_file', u'logo')
        SQL5 += u", logo_image=" + sqlval(to_json(imo))
    if u'cover_file' in ps:
        imo = save_image_file(u'cover_file', u'cover')
        SQL5 += u", cover_image=" + sqlval(to_json(imo))
    if u'screenshot1_file' in ps:
        imo = save_image_file(u'screenshot1_file', u'screenshot1')
        SQL5 += u", screenshot1_image=" + sqlval(to_json(imo))
    if u'screenshot2_file' in ps:
        imo = save_image_file(u'screenshot2_file', u'screenshot2')
        SQL5 += u", screenshot2_image=" + sqlval(to_json(imo))
    if u'screenshot3_file' in ps:
        imo = save_image_file(u'screenshot3_file', u'screenshot3')
        SQL5 += u", screenshot3_image=" + sqlval(to_json(imo))
    if u'screenshot4_file' in ps:
        imo = save_image_file(u'screenshot4_file', u'screenshot4')
        SQL5 += u", screenshot4_image=" + sqlval(to_json(imo))
    if u'screenshot5_file' in ps:
        imo = save_image_file(u'screenshot5_file', u'screenshot5')
        SQL5 += u", screenshot5_image=" + sqlval(to_json(imo))

    SQL5 += u" WHERE id=%s"

    now = now_ms()

    def handle_conn(conn):
        p = FetchFirst(SQL1, (pid))(conn)
        if not p:
            new_product = True
        else:
            new_product = False
            if p[u'author_borqs_id'] != ctx.borqs_id:
                raise APIError(E_PERMISSION_DENIED, u"Permission denied")

        if new_product:
            if u'pricetag_id' not in ps:
                raise APIError(E_ILLEGAL_PARAM, u'Missing pricetag_id')
            profile = account_services.get_profile(ctx.borqs_id)
            gv.db.update(SQL6, (pid, now, now,
                                ps[u'pricetag_id'], u'',
                                to_json({u'default': u''}), u'', u'', 0, to_json({u'default': u''}),
                                profile.get(u'name', u''), profile.get(u'email', u''), ctx.borqs_id, u'',
                                0))



        versions = FetchColumn(u'version', SQL2, (pid))(conn)
        if version in versions:
            is_new_version = False
        else:
            if len(versions) > 0 and version_comparator(version, max(versions, key=version_sort_key)) < 0:
                raise APIError(E_TOO_SMALL_VERSION, u'Version must > ' + max(version))
            is_new_version = True

        if is_new_version:
            Update(SQL3, (pid, version, now, now, supported_mod, 1,
                          update_change, min_app_vc, max_app_vc,
                          action, action_url, file_size))(conn)
        else:
            Update(SQL4, (now, pid, version))(conn)

        Update(SQL5, (now, pid))(conn)

    gv.db.open_conn(handle_conn)
    return get_product(ctx, pid, version)

def get_last_version(pid):
    SQL1 = u"SELECT MAX(version) AS last_version FROM product_versions WHERE product_id=%s GROUP BY product_id"
    return gv.db.fetch_value(SQL1, (pid))

def upsert_version_for_lang(ctx, lang, id, version, name, pricetag_id,
                            description, update_change,
                            min_app_vc, max_app_vc, supported_mod,
                            author_name, author_email,
                            logo_file, cover_file, screenshot1_file, screenshot2_file, screenshot3_file, screenshot4_file, screenshot5_file,
                            product_file):
    def merge_text(json_obj, text):
        if not text:
            text = u''
        if isinstance(json_obj, basestring):
            json_obj = parse_json(json_obj)
        o = json_obj.copy() if json_obj else {}
        o[lang] = text
        return to_json(o)

    def merge_supported_lang(langs):
        langs1 = list(langs) if langs else []
        if lang not in langs1:
            langs1.append(lang)
        return ','.join(langs1)


    try:
        p = get_product(ctx, id, version)
    except APIError, e:
        if e.code == E_ILLEGAL_PRODUCT:
            p = {}
        else:
            raise e
    ps = dict()
    if id:
        ps[u'id'] = id

    ps[u'version'] = version
    ps[u'supported_languages'] = merge_supported_lang(p.get(u'supported_languages', []))
    ps[u'name'] = merge_text(p.get(u'name'), name)
    ps[u'pricetag_id'] = pricetag_id
    ps[u'description'] = merge_text(p.get(u'description'), description)
    ps[u'update_change'] = merge_text(p.get(u'update_change'), update_change)
    ps[u'min_app_vc'] = min_app_vc
    ps[u'max_app_vc'] = max_app_vc
    ps[u'supported_mod'] = supported_mod
    ps[u'author_name'] = author_name
    ps[u'author_email'] = author_email
    if logo_file:
        ps[u'logo_file'] = logo_file
    if cover_file:
        ps[u'cover_file'] = cover_file
    if screenshot1_file:
        ps[u'screenshot1_file'] = screenshot1_file
    if screenshot2_file:
        ps[u'screenshot2_file'] = screenshot2_file
    if screenshot3_file:
        ps[u'screenshot3_file'] = screenshot3_file
    if screenshot4_file:
        ps[u'screenshot4_file'] = screenshot4_file
    if screenshot5_file:
        ps[u'screenshot5_file'] = screenshot5_file
    if product_file:
        ps[u'file'] = product_file
    p1 = upsert_version(ctx, **kws_to_26(ps))
    return get_product_for_lang(ctx, p1[u'id'], p1[u'version'], lang)


def active_version(ctx, pid, version, value):
    SQL1 = u"UPDATE product_versions SET status=%s WHERE product_id=%s AND version=%s"
    SQL2 = u"SELECT status FROM product_versions WHERE product_id=%s AND version=%s"
    _check_borqs_id(ctx)

    def handle_conn(conn):
        pid1 = FetchValue(u"SELECT version FROM product_versions WHERE product_id=%s AND version=%s")
        if not pid1:
            raise APIError(E_ILLEGAL_PRODUCT_VERSION, u'Product or Version is not exists')
        Update(SQL1, (1 if value else 0, pid, version))(conn)
        v = FetchValue(SQL2, (pid, version))(conn)
        return True if v == 1 else False

    return gv.db.open_conn(handle_conn)


def active_product(ctx, pid, value):
    SQL1 = u"UPDATE products SET status=%s WHERE id=%s"
    SQL2 = u"SELECT status FROM products WHERE id=%s"
    _check_borqs_id(ctx)

    def handle_conn(conn):
        pid1 = FetchValue(u"SELECT id FROM products WHERE id=%s", (pid))(conn)
        if not pid1:
            raise APIError(E_ILLEGAL_PRODUCT, u'Product is not exists')

        Update(SQL1, (1 if value else 0, pid))(conn)
        v = FetchValue(SQL2, (pid))(conn)
        return True if v == 1 else False

    return gv.db.open_conn(handle_conn)


