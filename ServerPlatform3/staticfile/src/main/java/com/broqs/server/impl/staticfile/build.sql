
CREATE TABLE IF NOT EXISTS `static_file` (
  `file_id` BIGINT(20) NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `summary` VARCHAR(4096) DEFAULT '',
  `description` VARCHAR(4096) DEFAULT '',
  `file_size` BIGINT(20) DEFAULT '0',
  `user_id` BIGINT(20) NOT NULL,
  `exp_name` VARCHAR(32) NOT NULL DEFAULT '',
  `html_url` VARCHAR(512) DEFAULT '',
  `content_type` VARCHAR(32) DEFAULT '',
  `new_file_name` VARCHAR(128) DEFAULT '',
  `created_time` BIGINT(20) NOT NULL DEFAULT '0',
  `updated_time` BIGINT(20) NOT NULL DEFAULT '0',
  `destroyed_time` BIGINT(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`file_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8