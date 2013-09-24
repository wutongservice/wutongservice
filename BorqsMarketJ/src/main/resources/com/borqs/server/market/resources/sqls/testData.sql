
DELETE FROM product_versions;
DELETE FROM products;
DELETE FROM pricetags;
DELETE FROM categories;
DELETE FROM borqs_apps;

-- ------------------------------------------

INSERT INTO borqs_apps
SET
  `id` = 'com.borqs.se',
  `created_at` = :now,
  `updated_at` = :now,
  `default_lang` = 'en_US',
  `name` = '{"en_US":"3D Home", "zh_CN":"3D 主屏"}'
;
INSERT INTO borqs_apps
SET
  `id` = 'com.borqs.wutong',
  `created_at` = :now,
  `updated_at` = :now,
  `default_lang` = 'en_US',
  `name` = '{"en_US":"Wutong", "zh_CN":"梧桐"}'
;

-- ------------------------------------------

INSERT INTO categories
SET
  `app_id` = 'com.borqs.se',
    `category` = 'theme',
    `created_at` = :now,
    `updated_at` = :now,
    `name` = '{"en_US":"Theme", "zh_CN":"主题"}'
;

INSERT INTO categories
SET
  `app_id` = 'com.borqs.se',
  `category` = 'object',
  `created_at` = :now,
  `updated_at` = :now,
  `name` = '{"en_US":"Object", "zh_CN":"对象"}'
;

INSERT INTO categories
SET
  `app_id` = 'com.borqs.wutong',
  `category` = 'feature',
  `created_at` = :now,
  `updated_at` = :now,
  `name` = '{"en_US":"Feature", "zh_CN":"功能"}'
;

-- ------------------------------------------

INSERT INTO pricetags
SET
  `id` = 'theme.$0',
  `app_id` = 'com.borqs.se',
  `category` = 'theme',
  `created_at` = :now,
  `updated_at` = :now,
  `google_sku` = '',
  `free` = 1,
  `price` = ''
;

INSERT INTO pricetags
SET
  `id` = 'theme.$1',
  `app_id` = 'com.borqs.se',
  `category` = 'theme',
  `created_at` = :now,
  `updated_at` = :now,
  `google_sku` = 'gsku1',
  `free` = 0,
  `price` = '{"default":"USD1", "en_US":"USD1", "zh_CN":"CNY6"}'
;

INSERT INTO pricetags
SET
  `id` = 'theme.$2',
  `app_id` = 'com.borqs.se',
  `category` = 'theme',
  `created_at` = :now,
  `updated_at` = :now,
  `google_sku` = 'gsku2',
  `free` = 0,
  `price` = '{"default":"USD2", "en_US":"USD2", "zh_CN":"CNY12"}'
;

INSERT INTO pricetags
SET
  `id` = 'object.$0',
  `app_id` = 'com.borqs.se',
  `category` = 'object',
  `created_at` = :now,
  `updated_at` = :now,
  `google_sku` = '',
  `free` = 1,
  `price` = ''
;


INSERT INTO pricetags
SET
  `id` = 'feature.$0',
  `app_id` = 'com.borqs.wutong',
  `category` = 'feature',
  `created_at` = :now,
  `updated_at` = :now,
  `google_sku` = '',
  `free` = 1,
  `price` = ''
;
-- ------------------------------------------

INSERT INTO products
SET
  `id` = 'com.borqs.se.theme1',
  `created_at` = :now,
  `updated_at` = :now,
  `last_version_created_at` = :now,
  `app_id` = 'com.borqs.se',
  `category` = 'theme',
  `pricetag_id` = 'theme.$0',
  `available_langs` = '',
  `type1` = '',
  `type2` = '',
  `type3` = '',
  `author_id` = '10012',
  `author_name` = 'Gao Rongxin',
  `author_email` = 'rongxin.gao@borqs.com',
  `author_phone` = '12345678',
  `author_google_id` = 'rongxin.gao@gmail.com',
  `author_website` = 'http://gaorx',
  `default_lang` = 'en_US',
  `name` = '{"en_US":"Theme1", "zh_CN":"主题1"}',
  `description` = '{"en_US":"Theme1 desc", "zh_CN":"主题1描述"}'
;

INSERT INTO products
SET
  `id` = 'com.borqs.se.theme2',
  `created_at` = :now,
  `updated_at` = :now,
  `last_version_created_at` = :now,
  `app_id` = 'com.borqs.se',
  `category` = 'theme',
  `pricetag_id` = 'theme.$0',
  `available_langs` = '',
  `type1` = '',
  `type2` = '',
  `type3` = '',
  `author_id` = '10012',
  `author_name` = 'Gao Rongxin',
  `author_email` = 'rongxin.gao@borqs.com',
  `author_phone` = '12345678',
  `author_google_id` = 'rongxin.gao@gmail.com',
  `author_website` = 'http://gaorx',
  `default_lang` = 'en_US',
  `name` = '{"en_US":"Theme2", "zh_CN":"主题2"}',
  `description` = '{"en_US":"Theme2 desc", "zh_CN":"主题2描述"}'
;

INSERT INTO products
SET
  `id` = 'com.borqs.se.theme3',
  `created_at` = :now,
  `updated_at` = :now,
  `last_version_created_at` = :now,
  `app_id` = 'com.borqs.se',
  `category` = 'theme',
  `pricetag_id` = 'theme.$1',
  `available_langs` = '',
  `type1` = '',
  `type2` = '',
  `type3` = '',
  `author_id` = '10012',
  `author_name` = 'Gao Rongxin',
  `author_email` = 'rongxin.gao@borqs.com',
  `author_phone` = '12345678',
  `author_google_id` = 'rongxin.gao@gmail.com',
  `author_website` = 'http://gaorx',
  `default_lang` = 'en_US',
  `name` = '{"en_US":"Theme3", "zh_CN":"主题3"}',
  `description` = '{"en_US":"Theme3 desc", "zh_CN":"主题3描述"}'
;

-- ------------------------------------------

INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme1',
  `version` = 100,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 50,
  `max_app_version` = 60,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 100", "zh_CN":"版本 100"}',
  `recent_change` = '{"en_US":"Change 100", "zh_CN":"更改 100"}',
  `dependencies` = '[]'
;


INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme1',
  `version` = 101,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 51,
  `max_app_version` = 61,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 101", "zh_CN":"版本 101"}',
  `recent_change` = '{"en_US":"Change 101", "zh_CN":"更改 101"}',
  `dependencies` = '[]'
;

INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme1',
  `version` = 102,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 51,
  `max_app_version` = 61,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 102", "zh_CN":"版本 102"}',
  `recent_change` = '{"en_US":"Change 102", "zh_CN":"更改 102"}',
  `dependencies` = '[]'
;

INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme2',
  `version` = 200,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 0,
  `max_app_version` = 10000,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 200", "zh_CN":"版本 200"}',
  `recent_change` = '{"en_US":"Change 200", "zh_CN":"更改 200"}',
  `dependencies` = '[]'
;

INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme2',
  `version` = 201,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 0,
  `max_app_version` = 10000,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 201", "zh_CN":"版本 201"}',
  `recent_change` = '{"en_US":"Change 201", "zh_CN":"更改 201"}',
  `dependencies` = '[]'
;

INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme3',
  `version` = 300,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 0,
  `max_app_version` = 10000,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 300", "zh_CN":"版本 300"}',
  `recent_change` = '{"en_US":"Change 300", "zh_CN":"更改 300"}',
  `dependencies` = '[]'
;

INSERT INTO product_versions
SET
  `product_id` = 'com.borqs.se.theme3',
  `version` = 301,
  `created_at` = :now,
  `updated_at` = :now,
  `status` = 1,
  `min_app_version` = 0,
  `max_app_version` = 10000,
  `supported_mod` = '*',
  `action` = 'download',
  `action_url` = 'http://download1',
  `version_name` = '{"en_US":"Version 301", "zh_CN":"版本 301"}',
  `recent_change` = '{"en_US":"Change 301", "zh_CN":"更改 301"}',
  `dependencies` = '[]'
;