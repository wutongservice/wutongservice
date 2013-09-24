
CREATE TABLE IF NOT EXISTS comment (
  `comment_id` BIGINT(20) NOT NULL,
  `target_id` VARCHAR(64) NOT NULL DEFAULT '',
  `target_type` BIGINT(20) DEFAULT '0',
  `created_time` BIGINT(20) NOT NULL,
  `destroyed_time` BIGINT(20) DEFAULT '0',
  `commenter` BIGINT(20) NOT NULL,
  `message` VARCHAR(4096) NOT NULL,
  `device` VARCHAR(256) DEFAULT NULL,
  `can_like` TINYINT(4) DEFAULT '1',
  `add_to` VARCHAR(256) DEFAULT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `created_time` (`created_time`),
  KEY `commenter` (`commenter`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `comment_target_index` (
  `target_id` VARCHAR(60) NOT NULL,
  `target_type` BIGINT(20) NOT NULL,
  `comment_id` BIGINT(20) NOT NULL,
  PRIMARY KEY (`target_id`,`target_type`,`comment_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;