DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS product_stats;
DROP TABLE IF EXISTS product_versions;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS pricetags;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS borqs_apps;
DROP TABLE IF EXISTS languages;


/* Languages */
CREATE TABLE languages (
  lcid            VARCHAR(16)  NOT NULL,
  name            VARCHAR(255) NOT NULL,
  currency_symbol VARCHAR(8)   NOT NULL,

  PRIMARY KEY (`lcid`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;

INSERT INTO languages (lcid, name, currency_symbol) VALUES ('en_US', 'English(US)', 'USD');
INSERT INTO languages (lcid, name, currency_symbol) VALUES ('zh_CN', '中文(中国)', 'CNY');

/* Borqs apps */

CREATE TABLE borqs_apps (
  `id`           VARCHAR(255) NOT NULL,
  `created_at`   BIGINT       NOT NULL,
  `updated_at`   BIGINT       NOT NULL,
  `default_lang` VARCHAR(16)  NOT NULL,
  `name`         MEDIUMTEXT   NOT NULL,
  `phone_page`   MEDIUMTEXT,
  `pad_page`     MEDIUMTEXT,
  `pc_page`      MEDIUMTEXT,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`default_lang`) REFERENCES languages (`lcid`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;

/* Product categories in a Borqs app */

CREATE TABLE categories (
  `app_id`     VARCHAR(255) NOT NULL,
  `category`   VARCHAR(255) NOT NULL,
  `created_at` BIGINT       NOT NULL,
  `updated_at` BIGINT       NOT NULL,
  `name`       MEDIUMTEXT   NOT NULL,
  `phone_page` MEDIUMTEXT,
  `pad_page`   MEDIUMTEXT,
  `pc_page`    MEDIUMTEXT,


  PRIMARY KEY (`app_id`, `category`),
  FOREIGN KEY (`app_id`) REFERENCES borqs_apps (`id`),
  INDEX (`category`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;

/* Pricetags */

CREATE TABLE pricetags (
  `id`         VARCHAR(255) NOT NULL,
  `app_id`     VARCHAR(255) NOT NULL,
  `category`   VARCHAR(255) NOT NULL,
  `created_at` BIGINT       NOT NULL,
  `updated_at` BIGINT       NOT NULL,
  `google_sku` VARCHAR(255) NOT NULL DEFAULT '',
  `free`       TINYINT      NOT NULL,
  `price`     VARCHAR(4096),

  PRIMARY KEY (`id`, `app_id`, `category`),
  FOREIGN KEY (`app_id`, `category`) REFERENCES categories (`app_id`, `category`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;


/* Products & Versions*/

CREATE TABLE products (
  `id`                      VARCHAR(255)  NOT NULL,
  `created_at`              BIGINT        NOT NULL,
  `updated_at`              BIGINT        NOT NULL,
  `last_version_created_at` BIGINT        NOT NULL DEFAULT 0,
  `app_id`                  VARCHAR(255)  NOT NULL,
  `category`                VARCHAR(255)  NOT NULL,
  `pricetag_id`             VARCHAR(255)  NOT NULL,
  `available_langs`         VARCHAR(2048) NOT NULL DEFAULT '',
  `type1`                   VARCHAR(255)  NOT NULL DEFAULT '',
  `type2`                   VARCHAR(255)  NOT NULL DEFAULT '',
  `type3`                   VARCHAR(255)  NOT NULL DEFAULT '',
  `author_id`               VARCHAR(255)  NOT NULL,
  `author_name`             VARCHAR(255)  NOT NULL,
  `author_email`            VARCHAR(255)  NOT NULL,
  `author_phone`            VARCHAR(255)  NOT NULL,
  `author_google_id`        VARCHAR(255)  NOT NULL,
  `author_website`          VARCHAR(1024) NOT NULL,
  `logo_image`              VARCHAR(1024) NOT NULL DEFAULT '{}',
  `cover_image`             VARCHAR(1024) NOT NULL DEFAULT '{}',
  `default_lang`            VARCHAR(16)   NOT NULL,
  `name`                    MEDIUMTEXT    NOT NULL,
  `description`             MEDIUMTEXT    NOT NULL,

-- stat
  `download_count`          BIGINT        NOT NULL DEFAULT 0,
  `rating`                  DOUBLE        NOT NULL DEFAULT 0.6,
  `rating_count`            BIGINT        NOT NULL DEFAULT 0,
  `like_count`              BIGINT        NOT NULL DEFAULT 0,
  `dislike_count`           BIGINT        NOT NULL DEFAULT 0,
  `comment_count`           BIGINT        NOT NULL DEFAULT 0,
  `share_count`             BIGINT        NOT NULL DEFAULT 0,

-- page
  `phone_page`              MEDIUMTEXT,
  `pad_page`                MEDIUMTEXT,
  `pc_page`                 MEDIUMTEXT,

  PRIMARY KEY (`id`),
  INDEX (`app_id`, `category`),
  INDEX (`app_id`),
  INDEX (`pricetag_id`),
  FOREIGN KEY (`app_id`, `category`) REFERENCES pricetags (`app_id`, `category`),
  FOREIGN KEY (`default_lang`) REFERENCES languages (`lcid`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;

CREATE TABLE product_versions (
  `product_id`        VARCHAR(255)  NOT NULL,
  `version`           INT           NOT NULL,
  `created_at`        BIGINT        NOT NULL,
  `updated_at`        BIGINT        NOT NULL,
  `status`            TINYINT       NOT NULL,
  `min_app_version`        INT           NOT NULL,
  `max_app_version`        INT           NOT NULL,
  `supported_mod`     VARCHAR(255)  NOT NULL DEFAULT '*',
  `action`            VARCHAR(64)   NOT NULL,
  `action_url`        VARCHAR(1024) NOT NULL,
  `file_size`         BIGINT        NOT NULL DEFAULT 0,
  `file_md5`          VARCHAR(64)   NOT NULL DEFAULT '',
  `screenshot1_image` VARCHAR(1024) NOT NULL DEFAULT '{}',
  `screenshot2_image` VARCHAR(1024) NOT NULL DEFAULT '{}',
  `screenshot3_image` VARCHAR(1024) NOT NULL DEFAULT '{}',
  `screenshot4_image` VARCHAR(1024) NOT NULL DEFAULT '{}',
  `screenshot5_image` VARCHAR(1024) NOT NULL DEFAULT '{}',
  `version_name`      MEDIUMTEXT    NOT NULL,
  `recent_change`     MEDIUMTEXT    NOT NULL,
  `dependencies`      MEDIUMTEXT    NOT NULL,

-- stat
  `download_count`    BIGINT        NOT NULL DEFAULT 0,
  `rating`            DOUBLE        NOT NULL DEFAULT 0.6,
  `rating_count`      BIGINT        NOT NULL DEFAULT 0,
  `like_count`        BIGINT        NOT NULL DEFAULT 0,
  `dislike_count`     BIGINT        NOT NULL DEFAULT 0,
  `comment_count`     BIGINT        NOT NULL DEFAULT 0,
  `share_count`       BIGINT        NOT NULL DEFAULT 0,

  PRIMARY KEY (`product_id`, `version`),
  FOREIGN KEY (`product_id`) REFERENCES products (`id`),
  INDEX (`product_id`),
  INDEX (`version`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;

/* Download Stat */
CREATE TABLE product_stats (
  `product_id`     VARCHAR(255) NOT NULL,
  `version`        INT          NOT NULL,
  `date`           VARCHAR(64)  NOT NULL,
  `lang`           VARCHAR(16)  NOT NULL,
  `download_count` BIGINT       NOT NULL DEFAULT 0,

  PRIMARY KEY (`product_id`, `version`, `date`, `lang`),
  FOREIGN KEY (`lang`) REFERENCES languages (`lcid`),
  INDEX (`product_id`),
  INDEX (`date`),
  INDEX (`lang`)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;

/* Orders */
CREATE TABLE orders (
  id                   VARCHAR(255), -- 订单ID
  created_at           BIGINT       NOT NULL, -- 订单时间

  product_id           VARCHAR(64)  NOT NULL, -- 购买的产品ID
  product_version      VARCHAR(128) NOT NULL, -- 产品的版本
  app_id               VARCHAR(200) NOT NULL, -- 购买产品所属的APP ID
  category             VARCHAR(255) NOT NULL, -- 价格标签所属的应用内资源分类ID

  purchaser_id         VARCHAR(255) NOT NULL DEFAULT '', -- 购买者ID
  purchaser_device_id  VARCHAR(255) NOT NULL DEFAULT '', -- 购买者设备ID
  purchaser_google_id1 VARCHAR(255) NOT NULL DEFAULT '', -- 购买者Google ID 1
  purchaser_google_id2 VARCHAR(255) NOT NULL DEFAULT '', -- 购买者Google ID 2
  purchaser_google_id3 VARCHAR(255) NOT NULL DEFAULT '',

  google_iab_order_id  VARCHAR(255) NOT NULL DEFAULT '',

  purchaser_locale     VARCHAR(32)  NOT NULL, -- 购买者的地区，格式为"zh_CN"
  purchaser_ip         VARCHAR(32)  NOT NULL, -- 购买者IP
  purchaser_user_agent VARCHAR(255) NOT NULL, -- 购买者客户端的User Agent

  PRIMARY KEY (id),
  INDEX (created_at),
  INDEX (product_id),
  INDEX (product_id, app_id),
  INDEX (product_id, purchaser_id),
  INDEX (product_id, purchaser_device_id),
  INDEX (product_id, purchaser_google_id1),
  INDEX (product_id, purchaser_google_id2),
  INDEX (product_id, purchaser_google_id3)
)
  ENGINE innodb
  DEFAULT CHARSET utf8;