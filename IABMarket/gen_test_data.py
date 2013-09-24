#!/usr/bin/python
# coding=utf-8

__author__ = 'rongxin.gao@borqs.com'

from dbwrapper import *
from util import *

now = now_ms()
db = DBWrapper(host=u'192.168.5.22', db=u'IABMarket1', user=u'root', passwd=u'111111')

# SELECT product_items.product_id, MAX(product_items.version) FROM products, product_items, pricetags WHERE (products.id=product_items.product_id) AND (products.pricetag_id=pricetags.id) AND (pricetags.app_id='com.borqs._3dhome') AND (pricetags.category='theme') AND (44 BETWEEN product_items.min_app_vc AND product_items.max_app_vc) GROUP BY product_items.product_id;
def handle_conn(conn):
    Update(u'DELETE FROM logs')(conn)
    Update(u'DELETE FROM orders')(conn)
    Update(u'DELETE FROM product_versions')(conn)
    Update(u'DELETE FROM products')(conn)
    Update(u'DELETE FROM pricetags')(conn)
    Update(u'DELETE FROM categories')(conn)
    Update(u'DELETE FROM apps')(conn)

    APP_3DHOME = u'com.borqs.se'

    Update(u"INSERT INTO apps (id, created_at, updated_at, name, supported_languages) VALUES (%s, %s, %s, %s, %s)", [
        (APP_3DHOME, now, now, u'{"default":"3DHome","zh_CN":"3D主屏"}', u'default,zh_CN'),
        #(u'com.borqs.wutong', now, now, u'{"default":"Wutong","zh_CN":"梧桐"}', u'default,zh_CN'),
    ])(conn)

    Update(u"INSERT INTO categories (app_id, category, created_at, updated_at, name) VALUES (%s, %s, %s, %s, %s)", [
        (APP_3DHOME, u'theme', now, now, u'{"default":"Theme","zh_CN":"主题"}'),
        (APP_3DHOME, u'object', now, now, u'{"default":"Object","zh_CN":"物件"}'),
    ])(conn)

    PT_FREE_THEME = u'pt_' + uuid_hex()
    PT_ONE_DOLLAR_THEME = u'pt_' + uuid_hex()
    PT_TWO_DOLLAR_THEME = u'pt_' + uuid_hex()

    PT_FREE_OBJECT = u'pt_' + uuid_hex()
    Update(
        u"INSERT INTO pricetags (id, created_at, updated_at, app_id, category, google_iab_sku, price) VALUES (%s, %s, %s, %s, %s, %s, %s)",
        [
            (PT_FREE_THEME, now, now, APP_3DHOME, u'theme', u'', 0),
            #(PT_ONE_DOLLAR_THEME, now, now, APP_3DHOME, u'theme', u'sku1', 100),
            #(PT_TWO_DOLLAR_THEME, now, now, APP_3DHOME, u'theme', u'sku2', 200),
            (PT_FREE_OBJECT, now, now, APP_3DHOME, u'object', u'', 0)
        ])(conn)

#    P_THEME1 = u'p_' + uuid_hex()
#    P_THEME2 = u'p_' + uuid_hex()
#    Update(
#        u"INSERT INTO products (id, created_at, updated_at, pricetag_id, name, supported_languages, status, description, author_name,    author_email,              author_borqs_id, author_google_id,        download_count, logo_image, cover_image) VALUES "
#                             u"(%s, %s,         %s,         %s,          %s,    %s,                 1,      %s,          'Gao Rongxin',  'rongxin.gao@borqs.com',   '10012',         'rongxin.gao@gmail.com', 0,              '{}',       '{}')",
#        [
#            (P_THEME1, now, now, PT_FREE_THEME, u'{"default":"Theme1","zh_CN":"主题1"}', u'default,zh_CN',
#             u'{"default":"Theme1 description","zh_CN":"主题1描述"}'),
#            (P_THEME2, now, now, PT_ONE_DOLLAR_THEME, u'{"default":"Theme2","zh_CN":"主题2"}', u'default,zh_CN',
#             u'{"default":"Theme2 description","zh_CN":"主题2描述"}'),
#        ])(conn)
#
#    Update(
#        u"INSERT INTO product_versions (product_id, version, created_at, updated_at, status, update_change, min_app_vc, max_app_vc, action, action_url, file_size) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
#        [
#            (P_THEME1, u'1.0.1', now, now, 1, u'{"default":"1.0.1 change","zh_CN":"1.0.1 改动"}', 33, 77, u'download',
#             u'http://42.121.13.15/appdata/3DHome/v0.1/Themes/Valentine/Resource/valentine.zip', 1280078),
#            (P_THEME1, u'1.1.2', now, now, 1, u'{"default":"1.1.2 change","zh_CN":"1.1.2 改动"}', 44, 88, u'download',
#             u'http://42.121.13.15/appdata/3DHome/v0.1/Themes/Valentine/Resource/valentine.zip', 1280078),
#            (P_THEME2, u'2.1.3', now, now, 1, u'{"default":"2.1.3 change","zh_CN":"2.1.3改动"}', 30, 100, u'download',
#             u'http://42.121.13.15/appdata/3DHome/v0.1/Themes/Valentine/Resource/valentine.zip', 1280078),
#        ])(conn)

db.open_conn(handle_conn)




