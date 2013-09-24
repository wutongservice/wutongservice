

DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS product_versions;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS product_pricetags;
DROP TABLE IF EXISTS product_categories;
DROP TABLE IF EXISTS apps;
DROP TABLE IF EXISTS signin_tickets;
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
  id VARCHAR(128) NOT NULL PRIMARY KEY,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  disabled_at BIGINT NOT NULL DEFAULT 0,
  borqs BIT NOT NULL DEFAULT 0,
  boss BIT NOT NULL DEFAULT 0,

  name VARCHAR(128) NOT NULL DEFAULT '',
  password VARCHAR(128) NOT NULL DEFAULT '',
  avatar_image VARCHAR(1023) NOT NULL DEFAULT '',
  email VARCHAR(128),
  phone VARCHAR(128),
  website VARCHAR(128) NOT NULL DEFAULT '',

  google_id VARCHAR(128),
  facebook_id VARCHAR(128),
  twitter_id VARCHAR(128),
  qq_id VARCHAR(128),
  weibo_id VARCHAR(128),

  UNIQUE INDEX (email),
  UNIQUE INDEX (phone),
  UNIQUE INDEX (google_id),
  UNIQUE INDEX (facebook_id),
  UNIQUE INDEX (twitter_id),
  UNIQUE INDEX (qq_id),
  UNIQUE INDEX (weibo_id)
) ENGINE INNODB;


CREATE TABLE signin_tickets (
  ticket VARCHAR(128) NOT NULL PRIMARY KEY,
  account_id VARCHAR(128) NOT NULL,
  created_at BIGINT NOT NULL,

  INDEX (account_id)
) ENGINE INNODB;


CREATE TABLE apps (
  id VARCHAR(128) NOT NULL PRIMARY KEY,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  disabled_at BIGINT NOT NULL,
  name VARCHAR(8192) NOT NULL,
  creator_id VARCHAR(128) NOT NULL,
  operator_id VARCHAR(128) NOT NULL,

  INDEX (creator_id),
  INDEX (operator_id)
) ENGINE INNODB;


CREATE TABLE product_categories (
  category_id VARCHAR(128) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  name VARCHAR(8192) NOT NULL,
  available_mods VARCHAR(1024) NOT NULL DEFAULT '',
  available_tags VARCHAR(1024) NOT NULL DEFAULT '',
  page_b MEDIUMTEXT,
  page_m MEDIUMTEXT,
  page_s MEDIUMTEXT,

  PRIMARY KEY (category_id, app_id),
  INDEX (app_id)
) ENGINE INNODB;

CREATE TABLE product_pricetags (
  pricetag_id VARCHAR(128) NOT NULL,
  category_id VARCHAR(128) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  paid TINYINT NOT NULL,
  price VARCHAR(8192) NOT NULL,
  payment_type TINYINT NOT NULL DEFAULT 1,
  google_iab_sku VARCHAR(128) NOT NULL DEFAULT '',
  cmcc_mm_paycode VARCHAR(128) NOT NULL DEFAULT '',
  cmcc_mm_amount INT NOT NULL DEFAULT 1,

  PRIMARY KEY (pricetag_id, category_id, app_id),
  INDEX (app_id),
  INDEX (app_id, category_id)
) ENGINE INNODB;


CREATE TABLE products (
  id VARCHAR(128) NOT NULL PRIMARY KEY,
  pricetag_id VARCHAR(128),
  category_id VARCHAR(128) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  last_version_created_at BIGINT NOT NULL,
  default_locale VARCHAR(32) NOT NULL DEFAULT '',
  available_locales VARCHAR(1024) NOT NULL DEFAULT '',
  author_id VARCHAR(128) NOT NULL,
  author_name VARCHAR(128) NOT NULL DEFAULT '',
  author_email VARCHAR(128) NOT NULL DEFAULT '',
  author_phone VARCHAR(128) NOT NULL DEFAULT '',
  author_website VARCHAR(128) NOT NULL DEFAULT '',
  name VARCHAR(8192) NOT NULL,
  description MEDIUMTEXT NOT NULL,
  logo_image VARCHAR(512) NOT NULL DEFAULT '',
  cover_image VARCHAR(512) NOT NULL DEFAULT '',
  promotion_image VARCHAR(512) NOT NULL DEFAULT '',
  screenshot1_image VARCHAR(512) NOT NULL DEFAULT '',
  screenshot2_image VARCHAR(512) NOT NULL DEFAULT '',
  screenshot3_image VARCHAR(512) NOT NULL DEFAULT '',
  screenshot4_image VARCHAR(512) NOT NULL DEFAULT '',
  screenshot5_image VARCHAR(512) NOT NULL DEFAULT '',
  type1 VARCHAR(128) NOT NULL DEFAULT '',
  type2 VARCHAR(128) NOT NULL DEFAULT '',
  type3 VARCHAR(128) NOT NULL DEFAULT '',
  tags VARCHAR(512) NOT NULL DEFAULT '',
  publish_channels VARCHAR(256) NOT NULL DEFAULT '',
  page_b MEDIUMTEXT,
  page_m MEDIUMTEXT,
  page_s MEDIUMTEXT,

  purchase_count BIGINT NOT NULL DEFAULT 0,
  download_count BIGINT NOT NULL DEFAULT 0,
  rating DOUBLE NOT NULL DEFAULT 0.6,
  rating_count BIGINT NOT NULL DEFAULT 0,
  like_count BIGINT NOT NULL DEFAULT 0,
  dislike_count BIGINT NOT NULL DEFAULT 0,
  comment_count BIGINT NOT NULL DEFAULT 0,
  share_count BIGINT NOT NULL DEFAULT 0,


  INDEX (app_id, category_id),
  INDEX (app_id),
  INDEX (type1),
  INDEX (type2),
  INDEX (type3)
) ENGINE INNODB;


CREATE TABLE product_versions (
  product_id VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  beta TINYINT NOT NULL DEFAULT 0,
  min_app_version INT NOT NULL,
  max_app_version INT NOT NULL,
  supported_mod VARCHAR(512) NOT NULL DEFAULT '',
  action TINYINT NOT NULL DEFAULT 1,
  url VARCHAR(512) NOT NULL DEFAULT '',
  file_size BIGINT NOT NULL DEFAULT 0,
  file_md5 VARCHAR(64) NOT NULL DEFAULT '',
  version_name VARCHAR(8192) NOT NULL,
  recent_change MEDIUMTEXT NOT NULL,
  dependencies VARCHAR(4096) NOT NULL DEFAULT '',

  purchase_count BIGINT NOT NULL DEFAULT 0,
  download_count BIGINT NOT NULL DEFAULT 0,
  rating DOUBLE NOT NULL DEFAULT 0.6,
  rating_count BIGINT NOT NULL DEFAULT 0,
  like_count BIGINT NOT NULL DEFAULT 0,
  dislike_count BIGINT NOT NULL DEFAULT 0,
  comment_count BIGINT NOT NULL DEFAULT 0,
  share_count BIGINT NOT NULL DEFAULT 0,

  PRIMARY KEY (product_id, version)
) ENGINE INNODB;

CREATE TABLE orders (
  id VARCHAR(128) NOT NULL PRIMARY KEY,
  created_at BIGINT NOT NULL,
  purchaser_id VARCHAR(128) NOT NULL,

  status TINYINT NOT NULL DEFAULT 0,
  product_id VARCHAR(128) NOT NULL,
  product_version INT NOT NULL,
  product_category_id VARCHAR(128) NOT NULL,
  product_app_id VARCHAR(128) NOT NULL,

  purchaser_device_id VARCHAR(128) NOT NULL,
  purchaser_locale VARCHAR(64) NOT NULL,
  purchaser_ip VARCHAR(64) NOT NULL,
  purchaser_ua VARCHAR(512) NOT NULL DEFAULT '',

  google_iab_order VARCHAR(128),
  cmcc_mm_order VARCHAR(128),
  cmcc_mm_trade VARCHAR(128),

  pay_cs VARCHAR(16) NOT NULL DEFAULT '',
  pay_amount DOUBLE NOT NULL DEFAULT 0.0,

  data1 VARCHAR(2048),
  data2 VARCHAR(2048),

  INDEX (created_at),
  INDEX (purchaser_id),
  INDEX (product_id),
  INDEX (product_app_id),
  INDEX (product_app_id, product_id),
  INDEX (product_app_id, product_category_id)
) ENGINE INNODB;

CREATE TABLE downloads (
  id VARCHAR(128) NOT NULL PRIMARY KEY,
  created_at BIGINT NOT NULL,
  purchaser_id VARCHAR(128) NOT NULL DEFAULT '',
  order_id VARCHAR(128) NOT NULL DEFAULT '',
  product_id VARCHAR(128) NOT NULL,
  product_version INT NOT NULL,
  product_category_id VARCHAR(128) NOT NULL,
  product_app_id VARCHAR(128) NOT NULL,
  download_device_id VARCHAR(128) NOT NULL,
  download_locale VARCHAR(64) NOT NULL,
  download_ip VARCHAR(64) NOT NULL,
  download_ua VARCHAR(512) NOT NULL DEFAULT '',

  INDEX (created_at),
  INDEX (purchaser_id),
  INDEX (product_id),
  INDEX (product_app_id),
  INDEX (order_id),
  INDEX (product_app_id, product_id),
  INDEX (product_app_id, product_category_id)
) ENGINE INNODB;

CREATE TABLE partitions (
  id VARCHAR(128) NOT NULL PRIMARY KEY,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  creator_id VARCHAR(128) NOT NULL,
  app_id VARCHAR(128) NOT NULL,
  category_id VARCHAR(128) NOT NULL,
  name VARCHAR(8192) NOT NULL,
  description MEDIUMTEXT NOT NULL,
  logo_image VARCHAR(512) NOT NULL DEFAULT '',
  status TINYINT NOT NULL,
  module TINYINT NOT NULL,
  rule VARCHAR(4096),
  list MEDIUMTEXT,

  INDEX (`app_id`),
  INDEX (`app_id`, `category_id`),
  INDEX (`status`)
) ENGINE INNODB;

CREATE TABLE `statistics` (
  `app_id` VARCHAR(128) COLLATE utf8_bin NOT NULL,
  `category_id` VARCHAR(128) COLLATE utf8_bin NOT NULL,
  `product_id` VARCHAR(128) COLLATE utf8_bin NOT NULL,
  `version` VARCHAR(60) COLLATE utf8_bin NOT NULL,
  `country` VARCHAR(60) COLLATE utf8_bin NOT NULL,
  `dates` VARCHAR(20) COLLATE utf8_bin NOT NULL,
  `count` BIGINT(20) DEFAULT NULL,
  PRIMARY KEY (`product_id`,`version`,`country`,`dates`),
  KEY `NewIndex1` (`country`,`dates`),
  KEY `NewIndex2` (`product_id`),
  KEY `NewIndex3` (`dates`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `comment` (
  `id` VARCHAR(128) CHARACTER SET utf8 NOT NULL,
  `account_id` VARCHAR(128) CHARACTER SET utf8 NOT NULL,
  `product_id` VARCHAR(128) CHARACTER SET utf8 NOT NULL,
  `version` VARCHAR(128) CHARACTER SET utf8 DEFAULT NULL,
  `created_at` BIGINT(20) NOT NULL,
  `updated_at` BIGINT(20) NOT NULL,
  `message` VARCHAR(2000) CHARACTER SET utf8 DEFAULT NULL,
  `device` VARCHAR(500) CHARACTER SET utf8 DEFAULT NULL,
  `rating` DOUBLE DEFAULT 5,
  PRIMARY KEY (`id`),
  KEY `NewIndex1` (`account_id`),
  KEY `NewIndex2` (`product_id`),
  KEY `NewIndex3` (`updated_at`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE `share` (
  `id` VARCHAR(128) COLLATE utf8_bin NOT NULL,
  `category_id` VARCHAR(128) COLLATE utf8_bin NOT NULL,
  `app_id` VARCHAR(128) COLLATE utf8_bin NOT NULL,
  `created_at` BIGINT(20) NOT NULL DEFAULT 0,
  `updated_at` BIGINT(20) NOT NULL DEFAULT 0,
  `author_id` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `author_name` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `author_email` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `name` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `description` VARCHAR(1024) COLLATE utf8_bin NOT NULL DEFAULT '',
  `content` VARCHAR(1024) COLLATE utf8_bin NOT NULL DEFAULT '',
  `url` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `file_size` BIGINT(20) NOT NULL DEFAULT 0,
  `file_md5` VARCHAR(64) COLLATE utf8_bin NOT NULL DEFAULT '',
  `app_data_1` VARCHAR(1024) COLLATE utf8_bin NOT NULL DEFAULT '',
  `app_data_2` VARCHAR(1024) COLLATE utf8_bin NOT NULL DEFAULT '',
  `logo_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `cover_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `screenshot1_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `screenshot2_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `screenshot3_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `screenshot4_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `screenshot5_image` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `type1` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `type2` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `type3` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `tags` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `download_count` BIGINT(20) NOT NULL DEFAULT 0,
  `rating` DOUBLE NOT NULL DEFAULT 5,
  `rating_count` BIGINT(20) NOT NULL DEFAULT 0,
  `like_count` BIGINT(20) NOT NULL DEFAULT 0,
  `dislike_count` BIGINT(20) NOT NULL DEFAULT 0,
  `comment_count` BIGINT(20) NOT NULL DEFAULT 0,
  `share_count` BIGINT(20) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0,
  `app_version` INT NOT NULL DEFAULT 0,
  `supported_mod` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',
  `device_id` VARCHAR(128) COLLATE utf8_bin NOT NULL DEFAULT '',
  `locale` VARCHAR(64) COLLATE utf8_bin NOT NULL DEFAULT '',
  `ip` VARCHAR(64) COLLATE utf8_bin NOT NULL DEFAULT '',
  `ua` VARCHAR(512) COLLATE utf8_bin NOT NULL DEFAULT '',

  PRIMARY KEY (`id`),
  KEY `created_at` (`created_at`),
  KEY `updated_at` (`updated_at`),
  KEY `NewIndex1` (`category_id`,`app_id`),
  KEY `NewIndex2` (`download_count`),
  KEY `NewIndex3` (`rating`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE promotions (
  app_id VARCHAR(128) NOT NULL,
  category_id VARCHAR(128) NOT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  `list` MEDIUMTEXT,

  PRIMARY KEY (`app_id`, `category_id`)
) ENGINE INNODB;
