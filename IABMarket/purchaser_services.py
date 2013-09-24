__author__ = 'rongxin.gao@borqs.com'

from dbwrapper import *
from errors import *
from util import *
import gv
from upload import *


APP_PATTERN = u'(\\w|\\.)+'
VERSION_PATTERN = u'\\d+\\.\\d+\\.\\d+'

def _attach_purchase(ctx, p, conn):
    if p[u'price'] != u'0':
        # Cost product
        p[u'purchasable'] = ctx.has_google_ids()
    else:
        # Free product
        p[u'purchasable'] = ctx.has_google_ids() or ctx.has_device_id() or ctx.has_borqs_id()

    if ctx.has_google_ids() or ctx.has_device_id():
        conds = []
        if ctx.has_google_ids():
            conds.extend([u"(product_id=%(pid)s AND purchaser_google_id1 IN (%(gids)s))", u"(product_id=%(pid)s AND purchaser_google_id1 IN (%(gids)s))", u"(product_id=%(pid)s AND purchaser_google_id1 IN (%(gids)s))"])
        if ctx.has_device_id():
            conds.append(u"(product_id=%(pid)s AND purchaser_device_id=%(did)s)")
        sql = u"SELECT id FROM orders WHERE %s" % (u' OR '.join(conds))
        order_id = FetchValue(sql % {u'pid':sqlval(p[u'id']), u'gids':sqlval(ctx.google_ids), u'did':sqlval(ctx.device_id)})(conn)
        p[u'purchased'] = True if order_id else False
    else:
        p[u'purchased'] = False

def list_products(ctx, app, appvc, category, order, mod, paging):
    order_policies = {u'downloads':u'products.download_count', u'updated':u'MAX(product_versions.created_at)'}
    order_by = order_policies.get(order, u'MAX(product_versions.created_at)')
    mod_cond = (u" AND (product_versions.support_mod=%s OR product_versions.support_mod='*')" % sqlval(mod)) if mod not in [None, u'*', u'', '*', ''] else u""

    SQL1 = u"""
    SELECT
        product_versions.product_id as id,
        GROUP_CONCAT(product_versions.version) as version
    FROM
        products, product_versions, pricetags
    WHERE
        (products.id=product_versions.product_id)
        AND (products.pricetag_id=pricetags.id)
        AND (pricetags.app_id=%s)
        AND (pricetags.category=%s)
        AND (%s BETWEEN product_versions.min_app_vc AND product_versions.max_app_vc)
    """ + mod_cond + u"""
        AND (products.status<>0)
        AND (product_versions.status<>0)
    GROUP BY
        product_versions.product_id
    ORDER BY
        """ + order_by + u""" DESC
    LIMIT %s, %s
    """

    SQL2 = u"""
    SELECT
        product_versions.product_id as id,
        GROUP_CONCAT(product_versions.version) as version
    FROM
        products, product_versions, pricetags
    WHERE
        (products.id=product_versions.product_id)
        AND (products.pricetag_id=pricetags.id)
        AND (pricetags.app_id=%s)
        AND (%s BETWEEN product_versions.min_app_vc AND product_versions.max_app_vc)
     """ + mod_cond + u"""
        AND (products.status<>0)
        AND (product_versions.status<>0)
    GROUP BY
        product_versions.product_id
    ORDER BY
        """ + order_by + u""" DESC
    LIMIT %s, %s
    ;
    """

    SQL3 = u"""
    SELECT
        products.id as id,
        product_versions.version as version,
        products.name as name,
        apps.id as app,
        categories.category,
        categories.name as category_name,
        pricetags.price as price,
        products.logo_image as logo_image,
        products.cover_image as cover_image
    FROM
        product_versions, products, pricetags, categories, apps
    WHERE
        (product_versions.product_id=products.id)
        AND (products.pricetag_id=pricetags.id)
        AND (pricetags.app_id=categories.app_id)
        AND (pricetags.category=categories.category)
        AND (pricetags.app_id=apps.id)
        AND (categories.app_id=apps.id)
        AND (product_versions.product_id=%s)
        AND (product_versions.version=%s)
        AND (products.status<>0)
        AND (product_versions.status<>0)
    ;
    """

    def find_max_version(vers):
        return max(split2(vers, u','), key=version_sort_key)

    def handle_conn(conn):
        if category in [None, u'*', u'', '*', '']:
            pvs = FetchAll(SQL2, (app, appvc, paging[0] * paging[1], paging[1]))(conn)
        else:
            pvs = FetchAll(SQL1, (app, category, appvc, paging[0] * paging[1], paging[1]))(conn)

        for pv in pvs:
            pv[u'version'] = find_max_version(pv[u'version'])

        products = []
        for pv in pvs:
            p = FetchFirst(SQL3, (pv[u'id'], pv[u'version']))(conn)
            if p:
                parse_json_field(p, u'logo_image', {})
                parse_json_field(p, u'cover_image', {})
                wrap_image_fields(p, [u'logo_image', u'cover_image'])
                select_lang_fields(p, ctx.locale, [u'name', u'category_name'])
                select_lang_for_price_field(p, ctx.locale, u'price')
                _attach_purchase(ctx, p, conn)
                products.append(p)
        return products

    return gv.db.open_conn(handle_conn)


def show_products(ctx, pid, version):
    SQL = u"""
    SELECT
        products.id as id,
        product_versions.version as version,
        products.name as name,
        apps.id as app,
        categories.category,
        categories.name as category_name,
        pricetags.price as price,
        products.author_name as author_name,
        products.author_email as author_email,
        products.author_borqs_id as author_borqs_id,
        products.created_at as created_at,
        products.updated_at as updated_at,
        products.description as description,
        products.download_count as download_count,
        products.logo_image as logo_image,
        products.cover_image as cover_image,
        products.screenshot1_image as screenshot1_image,
        products.screenshot2_image as screenshot2_image,
        products.screenshot3_image as screenshot3_image,
        products.screenshot4_image as screenshot4_image,
        products.screenshot5_image as screenshot5_image,
        pricetags.google_iab_sku as google_iab_sku
    FROM
        product_versions, products, pricetags, categories, apps
    WHERE
        (product_versions.product_id=products.id)
        AND (products.pricetag_id=pricetags.id)
        AND (pricetags.app_id=categories.app_id)
        AND (pricetags.category=categories.category)
        AND (pricetags.app_id=apps.id)
        AND (categories.app_id=apps.id)
        AND (product_versions.product_id=%s)
        AND (product_versions.version=%s)
        AND (products.status<>0)
        AND (product_versions.status<>0)
    ;
    """
    def handle_conn(conn):
        p = FetchFirst(SQL, (pid, version))(conn)
        if not p:
            raise APIError(E_ILLEGAL_PRODUCT, u'Illegal product id ' + pid)
        screenshot_images = []
        for k in [u'screenshot1_image', u'screenshot2_image', u'screenshot3_image', u'screenshot4_image', u'screenshot5_image']:
            img = parse_json(p[k], None)
            if img:
                wrap_image_object(img)
                screenshot_images.append(img)
            del p[k]
        parse_json_field(p, u'logo_image', {})
        parse_json_field(p, u'cover_image', {})
        wrap_image_fields(p, [u'logo_image', u'cover_image'])
        select_lang_fields(p, ctx.locale, [u'name', u'category_name', u'description'])
        select_lang_for_price_field(p, ctx.locale, u'price')
        p[u'screenshot_images'] = screenshot_images
        _attach_purchase(ctx, p, conn)
        return p
    return gv.db.open_conn(handle_conn)

def purchase(ctx, pid, version, google_iab_order_id):
    SQL1 = u"""
    SELECT
        products.id as id,
        product_versions.version as version,
        apps.id as app,
        categories.category as category,
        pricetags.price as price,
        product_versions.action as action,
        product_versions.action_url as action_url,
        product_versions.file_size as file_size
    FROM
        product_versions, products, pricetags, categories, apps
    WHERE
        (product_versions.product_id=products.id)
        AND (products.pricetag_id=pricetags.id)
        AND (pricetags.app_id=categories.app_id)
        AND (pricetags.category=categories.category)
        AND (pricetags.app_id=apps.id)
        AND (categories.app_id=apps.id)
        AND (product_versions.product_id=%s)
        AND (product_versions.version=%s)
        AND (products.status<>0)
        AND (product_versions.status<>0)
    ;
    """

    SQL2 = u"""
        INSERT INTO orders (
            id,
            created_at,
            product_id,
            product_version,
            app_id,
            category,
            purchaser_device_id,
            purchaser_borqs_id,
            purchaser_google_id1,
            purchaser_google_id2,
            purchaser_google_id3,
            google_iab_order_id,
            purchaser_locale,
            purchaser_ip,
            purchaser_user_agent
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
        )
    ;
    """

    SQL3 = u"UPDATE products SET download_count=download_count + 1 WHERE id=%s"
    def handle_conn(conn):
        p = FetchFirst(SQL1, (pid, version))(conn)

        if p[u'price'] != 0:
            # Cost product
            if not ctx.has_google_ids():
                raise APIError(E_MISSING_IDENTITY, u'Missing google ids')
        else:
            # Free product
            if (not ctx.has_google_ids()) and (not ctx.has_device_id()):
                raise APIError(E_MISSING_IDENTITY, u'Missing google ids or device id')

        conds = []
        if ctx.has_borqs_id():
            conds.append(u"(product_id=%(pid)s AND purchaser_borqs_id=%(bid)s)")
        if ctx.has_google_ids():
            conds.extend([u"(product_id=%(pid)s AND purchaser_google_id1 IN (%(gids)s))", u"(product_id=%(pid)s AND purchaser_google_id2 IN (%(gids)s))", u"(product_id=%(pid)s AND purchaser_google_id3 IN (%(gids)s))"])
        if ctx.has_device_id():
            conds.append(u"(product_id=%(pid)s AND purchaser_device_id=%(did)s)")
        sql = u"SELECT id FROM orders WHERE %s" % (u' OR '.join(conds))
        sql = sql % {u'pid':sqlval(pid), u'gids':sqlval(ctx.google_ids), u'did':sqlval(ctx.device_id), u'bid':sqlval(ctx.borqs_id)}
        order_id = FetchValue(sql)(conn)
        if not order_id:
            # Purchase!
            first_purchase = True
            order_id = unicode(pid) + u'.' + unicode(random_int()) + u'.0'
            Update(SQL2, (order_id,
                          now_ms(),
                          pid,
                          version,
                          p[u'app'],
                          p[u'category'],
                          ctx.device_id,
                          ctx.borqs_id,
                          ctx.google_id1(),
                          ctx.google_id2(),
                          ctx.google_id3(),
                          unicode(google_iab_order_id),
                          ctx.locale,
                          ctx.ip,
                          ctx.user_agent
                ))(conn)
            Update(SQL3, (pid))(conn)
        else:
            first_purchase = False

        r = dict()
        r[u'order_id'] = order_id
        r[u'action'] = p[u'action']
        r[u'url'] = p[u'action_url']
        r[u'first_purchase'] = first_purchase
        r[u'app'] = p[u'app']
        r[u'category'] = p[u'category']
        if p[u'action'] == u'download':
            r[u'file_size'] = p[u'file_size']
        wrap_product_url_field(r, u'url')
        return r

    return gv.db.open_conn(handle_conn)