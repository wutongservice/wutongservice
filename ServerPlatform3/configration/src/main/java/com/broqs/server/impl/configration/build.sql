
CREATE TABLE IF NOT EXISTS `configration` (
  `user_id` BIGINT(20) NOT NULL,
  `config_key` VARCHAR(128) NOT NULL DEFAULT '',
  `value` VARCHAR(4096) NOT NULL DEFAULT '',
  `version_code` TINYINT(11) NOT NULL DEFAULT '0',
  `content_type` TINYINT(11) DEFAULT '0',
  `created_time` BIGINT(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`,`config_key`,`version_code`)
) ENGINE=INNODB DEFAULT CHARSET=utf8