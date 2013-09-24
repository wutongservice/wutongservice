DELETE FROM orders;
DELETE FROM product_versions;
DELETE FROM products;
DELETE FROM product_pricetags;
DELETE FROM product_categories;
DELETE FROM apps;
-- DELETE FROM signin_tickets;
DELETE FROM accounts;


-- accounts

INSERT INTO accounts
SET
  id='u_abcdefg',
  created_at=1370229870145,
  updated_at=1370229870145,
  disabled_at=0,
  borqs=1,
  `name`='gaorx',
  `password`='',
  avatar_image='',
  email='rongxin.gao@borqs.com',
  phone='',
  google_id='rongxin.gao'
;

-- apps

INSERT INTO apps
SET
  id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  disabled_at=0,
  `name`='{"en_US":"3D HOME", "zh_CN":"3D 主屏"}',
  creator_id = ''
;

INSERT INTO apps
SET
  id='com.borqs.ani',
  created_at=1370229870145,
  updated_at=1370229870145,
  disabled_at=0,
  `name`='{"en_US":"Ani HOME", "zh_CN":"卡通主屏"}',
  creator_id = ''
;

-- product_categories

INSERT INTO product_categories
SET
  category_id='theme',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  `name`='{"en_US":"Theme", "zh_CN":"主题"}'
;

INSERT INTO product_categories
SET
  category_id='object',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  `name`='{"en_US":"Object", "zh_CN":"对象"}'
;

INSERT INTO product_categories
SET
  category_id='wallpaper',
  app_id='com.borqs.ani',
  created_at=1370229870145,
  updated_at=1370229870145,
  `name`='{"en_US":"Wallpaper", "zh_CN":"壁纸"}'
;

-- product_pricetags

INSERT INTO product_pricetags
SET
  pricetag_id='free_theme',
  category_id='theme',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  paid=1,
  price='0',
  payment_type=1,
  google_iab_sku='',
  cmcc_mm_paycode='',
  cmcc_mm_amount=1
;

INSERT INTO product_pricetags
SET
  pricetag_id='1_theme',
  category_id='theme',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  paid=2,
  price='{"en_US":{"amount":1.0, "cs":"USD"}, "zh_CN":{"amount":6.0, "cs":"CNY"}}',
  payment_type=1,
  google_iab_sku='1_theme.google_iab_order',
  cmcc_mm_paycode='1_theme.cmcc_pay_code',
  cmcc_mm_amount=1
;

INSERT INTO product_pricetags
SET
  pricetag_id='free_object',
  category_id='object',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  paid=1,
  price='0',
  payment_type=1,
  google_iab_sku='',
  cmcc_mm_paycode='',
  cmcc_mm_amount=1
;

-- products

INSERT INTO products
SET
  id='com.borqs.se.theme.valentine',
  pricetag_id='free_theme',
  category_id='theme',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  last_version_created_at=1370229870145,
  default_locale='en_US',
  available_locales='',
  author_id='u_abcdefg',
  author_name='gaorx',
  author_email='rongxin.gao@borqs.com',
  author_phone='',
  author_website='',
  `name`='{"en_US":"Valentine", "zh_CN":"情人节"}',
  description='{"en_US":"Valentine desc", "zh_CN":"情人节 描述信息"}',
  logo_image='valentine_logo.jpg',
  cover_image='valentine_cover.jpg',
  promotion_image='valentine_promotion.jpg',
  screenshot1_image='valentine_screenshot1.jpg',
  screenshot2_image='valentine_screenshot2.jpg',
  screenshot3_image='',
  screenshot4_image='',
  screenshot5_image='',
  type1='',
  type2='',
  type3='',
  tags='pink'
;

INSERT INTO products
SET
  id='com.borqs.se.theme.wood',
  pricetag_id='1_theme',
  category_id='theme',
  app_id='com.borqs.se',
  created_at=1370229870145,
  updated_at=1370229870145,
  last_version_created_at=1370229870145,
  default_locale='en_US',
  available_locales='',
  author_id='u_abcdefg',
  author_name='gaorx',
  author_email='rongxin.gao@borqs.com',
  author_phone='',
  author_website='',
  `name`='{"en_US":"Wood", "zh_CN":"木纹"}',
  description='{"en_US":"Wood desc", "zh_CN":"木纹 描述信息"}',
  logo_image='wood_logo.jpg',
  cover_image='wood_cover.jpg',
  promotion_image='wood_promotion.jpg',
  screenshot1_image='wood_screenshot1.jpg',
  screenshot2_image='',
  screenshot3_image='',
  screenshot4_image='',
  screenshot5_image='',
  type1='',
  type2='',
  type3='',
  tags='wood'
;


-- product_versions

INSERT INTO product_versions
SET
  product_id='com.borqs.se.theme.valentine',
  version=1,
  created_at=1370229870145,
  updated_at=1370229870145,
  `status`=3,
  min_app_version=0,
  max_app_version=10000,
  supported_mod='',
  `action`=1,
  url='valentine_1.apk',
  file_size=1024,
  file_md5='md5',
  version_name='{"en_US":"Version 1", "zh_CN":"版本 1"}',
  recent_change='{"en_US":"Change 1", "zh_CN":"更改 1"}',
  dependencies=''
;

INSERT INTO product_versions
SET
  product_id='com.borqs.se.theme.valentine',
  version=2,
  created_at=1370229870145,
  updated_at=1370229870145,
  `status`=3,
  min_app_version=0,
  max_app_version=10000,
  supported_mod='',
  `action`=1,
  url='valentine_2.apk',
  file_size=1024,
  file_md5='md5',
  version_name='{"en_US":"Version 2", "zh_CN":"版本 2"}',
  recent_change='{"en_US":"Change 2", "zh_CN":"更改 2"}',
  dependencies=''
;


INSERT INTO product_versions
SET
  product_id='com.borqs.se.theme.wood',
  version=3,
  created_at=1370229870145,
  updated_at=1370229870145,
  `status`=3,
  min_app_version=0,
  max_app_version=10000,
  supported_mod='',
  `action`=1,
  url='wood_3.apk',
  file_size=2048,
  file_md5='md5',
  version_name='{"en_US":"Version 3", "zh_CN":"版本 3"}',
  recent_change='{"en_US":"Change 3", "zh_CN":"更改 3"}',
  dependencies=''
;




